package kz.edu.lms.repository;

import kz.edu.lms.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    Page<Chapter> findAllByCourseId(Long courseId, Pageable pageable);
    Optional<Chapter> findByIdAndCourseId(Long id, Long courseId);

    @Query("select chapter from Chapter chapter join chapter.course.readerIds reader where reader = :userId")
    Page<Chapter> findAssigned(String userId, Pageable pageable);
}
