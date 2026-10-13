package kz.edu.lms.service;

import kz.edu.lms.dto.lesson.*;
import kz.edu.lms.entity.*;
import kz.edu.lms.mapper.LessonMapper;
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
class LessonServiceTest {
    @Mock LessonRepository repository;
    @Mock ChapterRepository parentRepository;
    Chapter parent;
    private final LessonMapper mapper = Mappers.getMapper(LessonMapper.class);
    private LessonService service;
    private Lesson entity;
    private final LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        service = new LessonService(mapper, repository, parentRepository);
        entity = new Lesson();
        entity.setId(1L);
        entity.setName("Original");
        entity.setDescription("Original description");
        entity.setCreatedTime(created);
        entity.setUpdatedTime(created);
        parent = new Chapter();
        parent.setId(10L);
        entity.setChapter(parent);
        entity.setSortOrder(1);
    }

    @Test
    void listsAllWithPaginationMetadata() {
        Pageable pageable = PageRequest.of(1, 2);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 3));
        var result = service.getAll(pageable);
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals("Original", result.getContent().getFirst().name());
    }

    @Test
    void getsExistingRecord() {
        when(repository.findByIdAndChapterId(1L, 10L)).thenReturn(Optional.of(entity));
        assertEquals(1L, service.getLessonById(10L, 1L).id());
        verify(repository).findByIdAndChapterId(1L, 10L);
    }

    @Test
    void rejectsMissingRecord() {
        var error = assertThrows(IllegalArgumentException.class, () -> service.getLessonById(10L, 1L));
        assertEquals("Lesson not found", error.getMessage());
    }

    @Test
    void createsRecordWithRequestFieldsAndGeneratedId() {
        when(parentRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(repository.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson saved = invocation.getArgument(0);
            assertNull(saved.getId());
            assertEquals("New name", saved.getName());
            assertEquals("Description", saved.getDescription());
            assertSame(parent, saved.getChapter());
            assertEquals(2, saved.getSortOrder());
            assertEquals("Content", saved.getContent());
            saved.setId(42L);
            saved.setCreatedTime(created);
            saved.setUpdatedTime(created);
            return saved;
        });
        var result = service.createLesson(10L, new LessonCreateRequest("New name", "Description", "Content", 2));
        assertEquals(42L, result.id());
        assertEquals("New name", result.name());
        assertEquals(created, result.createdTime());
    }

    @Test
    void updatesExistingObjectAndReturnsFlushedAuditDate() {
        when(repository.findByIdAndChapterId(1L, 10L)).thenReturn(Optional.of(entity));
        doAnswer(invocation -> { entity.setUpdatedTime(created.plusDays(1)); return null; }).when(repository).flush();
        var result = service.updateLesson(10L, 1L, new LessonUpdateRequest("New name", "Description", "Content", 2));
        assertEquals("New name", entity.getName());
        assertEquals("Description", entity.getDescription());
        assertEquals(1L, result.id());
        assertEquals(created, result.createdTime());
        assertEquals(created.plusDays(1), result.updatedTime());
        assertSame(parent, entity.getChapter());
        assertEquals(2, result.sortOrder());
        assertEquals("Content", result.content());
    }

    @Test
    void missingUpdateDoesNotWrite() {
        assertThrows(IllegalArgumentException.class, () -> service.updateLesson(10L, 1L, new LessonUpdateRequest("New name", "Description", "Content", 2)));
        verify(repository, never()).save(any());
        verify(repository, never()).flush();
    }

    @Test
    void deletesExistingRecord() {
        when(repository.findByIdAndChapterId(1L, 10L)).thenReturn(Optional.of(entity));
        service.deleteLessonByChapterId(1L, 10L);
        verify(repository).delete(entity);
    }

    @Test
    void missingDeleteDoesNotWrite() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteLessonByChapterId(1L, 10L));
        verify(repository, never()).delete(any(Lesson.class));
    }

    @Test
    void listsOnlyRequestedParent() {
        var pageable = PageRequest.of(0, 10, Sort.by("sortOrder", "id"));
        when(parentRepository.existsById(10L)).thenReturn(true);
        when(repository.findAllByChapterId(10L, pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        assertEquals(1L, service.getAllByChapterId(10L, pageable).getContent().getFirst().id());
        verify(repository).findAllByChapterId(10L, pageable);
    }

    @Test
    void existingParentWithNoChildrenReturnsEmptyPage() {
        var pageable = PageRequest.of(0, 10);
        when(parentRepository.existsById(10L)).thenReturn(true);
        when(repository.findAllByChapterId(10L, pageable)).thenReturn(Page.empty(pageable));
        assertTrue(service.getAllByChapterId(10L, pageable).isEmpty());
    }

    @Test
    void missingParentRejectsListingBeforeQueryingChildren() {
        assertThrows(IllegalArgumentException.class, () -> service.getAllByChapterId(10L, PageRequest.of(0, 10)));
        verifyNoInteractions(repository);
    }

    @Test
    void missingParentRejectsCreationBeforeSaving() {
        assertThrows(IllegalArgumentException.class, () -> service.createLesson(10L, new LessonCreateRequest("New name", "Description", "Content", 2)));
        verifyNoInteractions(repository);
    }

    @Test
    void cannotReadRecordUnderDifferentParent() {
        assertThrows(IllegalArgumentException.class, () -> service.getLessonById(99L, 1L));
        verify(repository).findByIdAndChapterId(1L, 99L);
        verify(repository, never()).findById(anyLong());
    }
}
