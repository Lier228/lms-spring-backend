package kz.edu.lms.repository;

import kz.edu.lms.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @EntityGraph(attributePaths = "chapter.course")
    @Query("select lesson from Lesson lesson where lesson.id = :id")
    Optional<Lesson> findWithCourseById(@Param("id") Long id);

    Page<Lesson> findAllByChapterId(Long chapterId, Pageable pageable);
    Optional<Lesson> findByIdAndChapterId(Long id, Long chapterId);
}
