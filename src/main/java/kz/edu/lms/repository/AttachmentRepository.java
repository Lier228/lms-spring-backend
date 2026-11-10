package kz.edu.lms.repository;

import kz.edu.lms.entity.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    @EntityGraph(attributePaths = "lesson.chapter.course")
    Optional<Attachment> findByUrl(String url);
}
