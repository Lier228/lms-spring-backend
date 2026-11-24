package kz.edu.lms.repository;

import kz.edu.lms.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("select c from Course c join c.readerIds reader where reader = :userId")
    Page<Course> findAssigned(String userId, Pageable pageable);

    @Query("select count(c) > 0 from Course c join c.readerIds reader where c.id = :courseId and reader = :userId")
    boolean hasAccess(Long courseId, String userId);

    @Modifying
    @Query(value = "insert into course_access (course_id, user_id) values (:courseId, :userId) on conflict do nothing", nativeQuery = true)
    void grantAccess(Long courseId, String userId);

    @Modifying
    @Query(value = "delete from course_access where course_id = :courseId and user_id = :userId", nativeQuery = true)
    void revokeAccess(Long courseId, String userId);
}
