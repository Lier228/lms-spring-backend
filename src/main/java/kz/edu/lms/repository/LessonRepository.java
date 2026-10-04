package kz.edu.lms.repository;

import kz.edu.lms.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Page<Lesson> findAllByChapterId(Long chapterId, Pageable pageable);
    Optional<Lesson> findByIdAndChapterId(Long id, Long chapterId);
}
