package kz.edu.lms.service;

import kz.edu.lms.dto.course.*;
import kz.edu.lms.entity.*;
import kz.edu.lms.mapper.CourseMapper;
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
class CourseServiceTest {
    @Mock CourseRepository repository;
    private final CourseMapper mapper = Mappers.getMapper(CourseMapper.class);
    private CourseService service;
    private Course entity;
    private final LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        service = new CourseService(repository, mapper);
        entity = new Course();
        entity.setId(1L);
        entity.setName("Original");
        entity.setDescription("Original description");
        entity.setCreatedTime(created);
        entity.setUpdatedTime(created);
    }

    @Test
    void listsAllWithPaginationMetadata() {
        Pageable pageable = PageRequest.of(1, 2);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 3));
        var result = service.getAllCourses(pageable);
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals("Original", result.getContent().getFirst().name());
    }

    @Test
    void getsExistingRecord() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertEquals(1L, service.getCourseById(1L).id());
        verify(repository).findById(1L);
    }

    @Test
    void rejectsMissingRecord() {
        var error = assertThrows(IllegalArgumentException.class, () -> service.getCourseById(1L));
        assertEquals("Course not found", error.getMessage());
    }

    @Test
    void createsRecordWithRequestFieldsAndGeneratedId() {
        when(repository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            assertNull(saved.getId());
            assertEquals("New name", saved.getName());
            assertEquals("Description", saved.getDescription());
            saved.setId(42L);
            saved.setCreatedTime(created);
            saved.setUpdatedTime(created);
            return saved;
        });
        var result = service.createCourse(new CourseCreateRequest("New name", "Description"));
        assertEquals(42L, result.id());
        assertEquals("New name", result.name());
        assertEquals(created, result.createdTime());
    }

    @Test
    void updatesExistingObjectAndReturnsFlushedAuditDate() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        doAnswer(invocation -> { entity.setUpdatedTime(created.plusDays(1)); return null; }).when(repository).flush();
        var result = service.updateCourse(1L, new CourseUpdateRequest("New name", "Description"));
        assertEquals("New name", entity.getName());
        assertEquals("Description", entity.getDescription());
        assertEquals(1L, result.id());
        assertEquals(created, result.createdTime());
        assertEquals(created.plusDays(1), result.updatedTime());
    }

    @Test
    void missingUpdateDoesNotWrite() {
        assertThrows(IllegalArgumentException.class, () -> service.updateCourse(1L, new CourseUpdateRequest("New name", "Description")));
        verify(repository, never()).save(any());
        verify(repository, never()).flush();
    }

    @Test
    void deletesExistingRecord() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        service.deleteCourse(1L);
        verify(repository).delete(entity);
    }

    @Test
    void missingDeleteDoesNotWrite() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteCourse(1L));
        verify(repository, never()).delete(any(Course.class));
    }

    @Test
    void translatesForeignKeyFailureToConflict() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new DataIntegrityViolationException("foreign key")).when(repository).flush();
        var error = assertThrows(ResponseStatusException.class, () -> service.deleteCourse(1L));
        assertEquals(409, error.getStatusCode().value());
    }
}
