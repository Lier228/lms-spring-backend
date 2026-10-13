package kz.edu.lms.service;

import kz.edu.lms.dto.chapter.*;
import kz.edu.lms.entity.*;
import kz.edu.lms.mapper.ChapterMapper;
import kz.edu.lms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {
    @Mock ChapterRepository repository;
    @Mock CourseRepository parentRepository;
    Course parent;
    private final ChapterMapper mapper = Mappers.getMapper(ChapterMapper.class);
    private ChapterService service;
    private Chapter entity;
    private final LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        service = new ChapterService(repository, parentRepository, mapper);
        entity = new Chapter();
        entity.setId(1L);
        entity.setName("Original");
        entity.setDescription("Original description");
        entity.setCreatedTime(created);
        entity.setUpdatedTime(created);
        parent = new Course();
        parent.setId(10L);
        entity.setCourse(parent);
        entity.setSortOrder(1);
    }

    @Test
    void listsAllWithPaginationMetadata() {
        Pageable pageable = PageRequest.of(1, 2);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 3));
        var result = service.getAllChapters(pageable);
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals("Original", result.getContent().getFirst().name());
    }

    @Test
    void getsExistingRecord() {
        when(repository.findByIdAndCourseId(1L, 10L)).thenReturn(Optional.of(entity));
        assertEquals(1L, service.getChapterById(10L, 1L).id());
        verify(repository).findByIdAndCourseId(1L, 10L);
    }

    @Test
    void rejectsMissingRecord() {
        var error = assertThrows(IllegalArgumentException.class, () -> service.getChapterById(10L, 1L));
        assertEquals("Chapter not found", error.getMessage());
    }

    @Test
    void createsRecordWithRequestFieldsAndGeneratedId() {
        when(parentRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(repository.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter saved = invocation.getArgument(0);
            assertNull(saved.getId());
            assertEquals("New name", saved.getName());
            assertEquals("Description", saved.getDescription());
            assertSame(parent, saved.getCourse());
            assertEquals(2, saved.getSortOrder());
            saved.setId(42L);
            saved.setCreatedTime(created);
            saved.setUpdatedTime(created);
            return saved;
        });
        var result = service.createChapter(10L, new ChapterCreateRequest("New name", 2, "Description"));
        assertEquals(42L, result.id());
        assertEquals("New name", result.name());
        assertEquals(created, result.createdTime());
    }

    @Test
    void updatesExistingObjectAndReturnsFlushedAuditDate() {
        when(repository.findByIdAndCourseId(1L, 10L)).thenReturn(Optional.of(entity));
        doAnswer(invocation -> { entity.setUpdatedTime(created.plusDays(1)); return null; }).when(repository).flush();
        var result = service.updateChapter(10L, 1L, new ChapterUpdateRequest("New name", 2, "Description"));
        assertEquals("New name", entity.getName());
        assertEquals("Description", entity.getDescription());
        assertEquals(1L, result.id());
        assertEquals(created, result.createdTime());
        assertEquals(created.plusDays(1), result.updatedTime());
        assertSame(parent, entity.getCourse());
        assertEquals(2, result.sortOrder());
    }

    @Test
    void missingUpdateDoesNotWrite() {
        assertThrows(IllegalArgumentException.class, () -> service.updateChapter(10L, 1L, new ChapterUpdateRequest("New name", 2, "Description")));
        verify(repository, never()).save(any());
        verify(repository, never()).flush();
    }

    @Test
    void deletesExistingRecord() {
        when(repository.findByIdAndCourseId(1L, 10L)).thenReturn(Optional.of(entity));
        service.deleteChapterByCourseId(1L, 10L);
        verify(repository).delete(entity);
    }

    @Test
    void missingDeleteDoesNotWrite() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteChapterByCourseId(1L, 10L));
        verify(repository, never()).delete(any(Chapter.class));
    }

    @Test
    void translatesForeignKeyFailureToConflict() {
        when(repository.findByIdAndCourseId(1L, 10L)).thenReturn(Optional.of(entity));
        doThrow(new DataIntegrityViolationException("foreign key")).when(repository).flush();
        var error = assertThrows(ResponseStatusException.class, () -> service.deleteChapterByCourseId(1L, 10L));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void listsOnlyRequestedParent() {
        var pageable = PageRequest.of(0, 10, Sort.by("sortOrder", "id"));
        when(parentRepository.existsById(10L)).thenReturn(true);
        when(repository.findAllByCourseId(10L, pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        assertEquals(1L, service.getAllByCourseId(10L, pageable).getContent().getFirst().id());
        verify(repository).findAllByCourseId(10L, pageable);
    }

    @Test
    void existingParentWithNoChildrenReturnsEmptyPage() {
        var pageable = PageRequest.of(0, 10);
        when(parentRepository.existsById(10L)).thenReturn(true);
        when(repository.findAllByCourseId(10L, pageable)).thenReturn(Page.empty(pageable));
        assertTrue(service.getAllByCourseId(10L, pageable).isEmpty());
    }

    @Test
    void missingParentRejectsListingBeforeQueryingChildren() {
        assertThrows(IllegalArgumentException.class, () -> service.getAllByCourseId(10L, PageRequest.of(0, 10)));
        verifyNoInteractions(repository);
    }

    @Test
    void missingParentRejectsCreationBeforeSaving() {
        assertThrows(IllegalArgumentException.class, () -> service.createChapter(10L, new ChapterCreateRequest("New name", 2, "Description")));
        verifyNoInteractions(repository);
    }

    @Test
    void cannotReadRecordUnderDifferentParent() {
        assertThrows(IllegalArgumentException.class, () -> service.getChapterById(99L, 1L));
        verify(repository).findByIdAndCourseId(1L, 99L);
        verify(repository, never()).findById(anyLong());
    }
}
