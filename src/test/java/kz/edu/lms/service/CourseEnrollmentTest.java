package kz.edu.lms.service;

import kz.edu.lms.entity.*;
import kz.edu.lms.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Transactional
class CourseEnrollmentTest {
    @Autowired CourseService courses;
    @Autowired ChapterService chapters;
    @Autowired LessonService lessons;
    @Autowired AttachmentService attachments;
    @Autowired CourseAccessService access;
    @Autowired CourseRepository courseRepository;
    @Autowired ChapterRepository chapterRepository;
    @Autowired LessonRepository lessonRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @MockitoBean UserService users;
    @MockitoBean FileService files;

    private final UUID student = UUID.randomUUID();
    private final PageRequest page = PageRequest.of(0, 1);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantAndRevokeApplyToTheWholeCourseWithTheSameToken() {
        var course = new Course();
        course.setName("Private course");
        courseRepository.save(course);
        var chapter = new Chapter();
        chapter.setName("Private chapter");
        chapter.setCourse(course);
        chapterRepository.save(chapter);
        var lesson = new Lesson();
        lesson.setName("Private lesson");
        lesson.setChapter(chapter);
        lessonRepository.save(lesson);
        UUID key = UUID.randomUUID();
        var attachment = new Attachment();
        attachment.setName("notes");
        attachment.setLesson(lesson);
        attachment.setUrl("/download?url=" + key);
        attachmentRepository.saveAndFlush(attachment);

        var auth = new UsernamePasswordAuthenticationToken(student.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        Long c = course.getId(), ch = chapter.getId(), l = lesson.getId();
        Runnable assertClosed = () -> {
            assertEquals(0, courses.getAllCourses(page).getTotalElements());
            assertEquals(0, chapters.getAllChapters(page).getTotalElements());
            assertEquals(0, lessons.getAll(page).getTotalElements());
            forbidden(() -> courses.getCourseById(c));
            forbidden(() -> chapters.getAllByCourseId(c, page));
            forbidden(() -> chapters.getChapterById(c, ch));
            forbidden(() -> lessons.getAllByChapterId(ch, page));
            forbidden(() -> lessons.getLessonById(ch, l));
            forbidden(() -> attachments.download(key, auth));
        };
        access.grant(c, UUID.randomUUID());
        assertClosed.run();
        verifyNoInteractions(files);

        access.grant(c, student);
        access.grant(c, student);
        assertEquals(1, courses.getAllCourses(page).getTotalElements());
        assertEquals(c, courses.getCourseById(c).id());
        assertEquals(1, chapters.getAllChapters(page).getTotalElements());
        assertEquals(ch, chapters.getAllByCourseId(c, page).getContent().getFirst().id());
        assertEquals(ch, chapters.getChapterById(c, ch).id());
        assertEquals(1, lessons.getAll(page).getTotalElements());
        assertEquals(l, lessons.getAllByChapterId(ch, page).getContent().getFirst().id());
        assertEquals(l, lessons.getLessonById(ch, l).id());
        attachments.download(key, auth);
        verify(files).download(key);

        access.revoke(c, student);
        access.revoke(c, student);
        assertClosed.run();
        verifyNoMoreInteractions(files);
        for (String role : List.of("ROLE_TEACHER", "ROLE_ADMIN")) {
            var staff = new UsernamePasswordAuthenticationToken("staff", null,
                    List.of(new SimpleGrantedAuthority(role)));
            SecurityContextHolder.getContext().setAuthentication(staff);
            assertEquals(c, courses.getCourseById(c).id());
            assertEquals(l, lessons.getLessonById(ch, l).id());
            attachments.download(key, staff);
        }
    }

    private void forbidden(Runnable read) {
        assertEquals(403, assertThrows(ResponseStatusException.class, read::run).getStatusCode().value());
    }
}
