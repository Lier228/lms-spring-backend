package kz.edu.lms.service;

import kz.edu.lms.dto.attachment.AttachmentResponse;
import kz.edu.lms.entity.Attachment;
import kz.edu.lms.mapper.AttachmentMapper;
import kz.edu.lms.repository.AttachmentRepository;
import kz.edu.lms.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final LessonRepository lessonRepository;
    private final AttachmentMapper attachmentMapper;
    private final FileService fileService;
    private final CourseService courseService;
    private final TransactionTemplate transactionTemplate;

    public AttachmentResponse upload(Long lessonId, MultipartFile file, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean allowed = authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_TEACHER"));
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin or teacher can upload attachments");

        var lesson = lessonRepository.findWithCourseById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));
        courseService.checkReadAccess(lesson.getChapter().getCourse().getId(), authentication);
        log.info("Creating new attachment");
        var stored = fileService.upload(file);
        try {
            // Complete the database transaction before returning; remove the file if it fails.
            return transactionTemplate.execute(status -> {
                var attachment = new Attachment();
                attachment.setName(stored.filename());
                attachment.setUrl("/download?url=" + stored.id());
                attachment.setLesson(lesson);
                var response = attachmentMapper.toResponse(attachmentRepository.saveAndFlush(attachment));
                log.debug("Persisted attachment data: {}", response);
                return response;
            });
        } catch (RuntimeException exception) {
            try {
                fileService.delete(stored.id());
            } catch (RuntimeException cleanupException) {
                log.error("Failed to remove unlinked file: {}", stored.id());
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public FileService.Download download(UUID url, Authentication authentication) {
        var attachment = attachmentRepository.findByUrl("/download?url=" + url)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        courseService.checkReadAccess(attachment.getLesson().getChapter().getCourse().getId(), authentication);
        return fileService.download(url);
    }
}
