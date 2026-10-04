package kz.edu.lms.repository;

import kz.edu.lms.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    Page<Chapter> findAllByCourseId(Long courseId, Pageable pageable);
    Optional<Chapter> findByIdAndCourseId(Long id, Long courseId);
}
