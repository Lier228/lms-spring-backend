package kz.edu.lms.service;

import kz.edu.lms.dto.file.FileResponse;
import kz.edu.lms.entity.*;
import kz.edu.lms.mapper.AttachmentMapper;
import kz.edu.lms.repository.AttachmentRepository;
import kz.edu.lms.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    @Mock AttachmentRepository repository;
    @Mock LessonRepository lessons;
    @Mock FileService files;
    @Mock CourseService courses;
    @Mock TransactionTemplate transactions;
    private AttachmentService service;
    private Lesson lesson;
    private final UUID id = UUID.randomUUID();
    private final MockMultipartFile file = new MockMultipartFile("file", "lesson.pdf", "application/pdf", new byte[]{1, 2});

    @BeforeEach
    void setUp() {
        service = new AttachmentService(repository, lessons, Mappers.getMapper(AttachmentMapper.class), files, courses, transactions);
        var course = new Course();
        course.setId(7L);
        var chapter = new Chapter();
        chapter.setCourse(course);
        lesson = new Lesson();
        lesson.setId(3L);
        lesson.setChapter(chapter);
    }

    private Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken("user-id", "unused", List.of(new SimpleGrantedAuthority(role)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_TEACHER"})
    void uploadsAndStoresAttachmentWithLessonAndDownloadUrl(String role) {
        var auth = auth(role);
        when(lessons.findWithCourseById(3L)).thenReturn(Optional.of(lesson));
        when(files.upload(file)).thenReturn(new FileResponse(id, "lesson.pdf", 2));
        when(transactions.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        var time = LocalDateTime.of(2026, 1, 1, 12, 0);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            Attachment attachment = invocation.getArgument(0);
            assertSame(lesson, attachment.getLesson());
            attachment.setId(10L);
            attachment.setCreatedTime(time);
            return attachment;
        });
        var response = service.upload(3L, file, auth);
        assertEquals(10L, response.id());
        assertEquals("lesson.pdf", response.name());
        assertEquals("/download?url=" + id, response.url());
        assertEquals(3L, response.lessonId());
        assertEquals(time, response.createdTime());
        var order = inOrder(courses, files, repository);
        order.verify(courses).checkReadAccess(7L, auth);
        order.verify(files).upload(file);
        order.verify(repository).saveAndFlush(any());
        verify(files, never()).delete(any());
    }

    @Test
    void deniesUserUploadBeforeReadingLessonOrStorage() {
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.upload(3L, file, auth("ROLE_USER"))).getStatusCode().value());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> service.upload(3L, file, null)).getStatusCode().value());
        verifyNoInteractions(lessons, files, repository, transactions);
    }

    @Test
    void missingLessonDoesNotUploadFile() {
        assertThrows(IllegalArgumentException.class, () -> service.upload(3L, file, auth("ROLE_ADMIN")));
        verifyNoInteractions(files, repository, transactions);
    }

    @Test
    void deniedCourseDoesNotUploadFile() {
        var auth = auth("ROLE_TEACHER");
        when(lessons.findWithCourseById(3L)).thenReturn(Optional.of(lesson));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(courses).checkReadAccess(7L, auth);
        assertThrows(ResponseStatusException.class, () -> service.upload(3L, file, auth));
        verifyNoInteractions(files, repository, transactions);
    }

    @Test
    void removesStoredFileIfDatabaseTransactionFails() {
        when(lessons.findWithCourseById(3L)).thenReturn(Optional.of(lesson));
        when(files.upload(file)).thenReturn(new FileResponse(id, "lesson.pdf", 2));
        var failure = new IllegalStateException("Commit failed");
        when(transactions.execute(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.upload(3L, file, auth("ROLE_ADMIN"))));
        verify(files).delete(id);
    }

    @Test
    void cleanupFailureDoesNotHideDatabaseFailure() {
        when(lessons.findWithCourseById(3L)).thenReturn(Optional.of(lesson));
        when(files.upload(file)).thenReturn(new FileResponse(id, "lesson.pdf", 2));
        var failure = new IllegalStateException("Commit failed");
        when(transactions.execute(any())).thenThrow(failure);
        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY)).when(files).delete(id);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.upload(3L, file, auth("ROLE_ADMIN"))));
    }

    @Test
    void uploadFailureDoesNotCreateAttachment() {
        when(lessons.findWithCourseById(3L)).thenReturn(Optional.of(lesson));
        when(files.upload(file)).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY));
        assertThrows(ResponseStatusException.class, () -> service.upload(3L, file, auth("ROLE_ADMIN")));
        verifyNoInteractions(repository, transactions);
    }

    private void existingAttachment() {
        var attachment = new Attachment();
        attachment.setUrl("/download?url=" + id);
        attachment.setLesson(lesson);
        when(repository.findByUrl(attachment.getUrl())).thenReturn(Optional.of(attachment));
    }

    @Test
    void checksActualLessonCourseBeforeDownloading() {
        existingAttachment();
        var auth = auth("ROLE_USER");
        var download = new FileService.Download(new InputStreamResource(new ByteArrayInputStream(new byte[]{1})), "lesson.pdf", 1);
        when(files.download(id)).thenReturn(download);
        assertSame(download, service.download(id, auth));
        var order = inOrder(repository, courses, files);
        order.verify(repository).findByUrl("/download?url=" + id);
        order.verify(courses).checkReadAccess(7L, auth);
        order.verify(files).download(id);
    }

    @Test
    void deniedCourseNeverReadsStorage() {
        existingAttachment();
        var auth = auth("ROLE_USER");
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(courses).checkReadAccess(7L, auth);
        assertThrows(ResponseStatusException.class, () -> service.download(id, auth));
        verifyNoInteractions(files);
    }

    @Test
    void unlinkedStorageObjectCannotBeDownloaded() {
        var error = assertThrows(IllegalArgumentException.class, () -> service.download(id, auth("ROLE_ADMIN")));
        assertEquals("Attachment not found", error.getMessage());
        verifyNoInteractions(courses, files);
    }
}
