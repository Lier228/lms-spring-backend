package kz.edu.lms;

import kz.edu.lms.entity.Course;
import kz.edu.lms.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LmsBackendApplicationTests {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testDatabaseInteraction() {
        Course course = new Course();
        course.setName("Java Developer");
        course.setDescription("Test Description");

        Course savedCourse = courseRepository.save(course);

        assertThat(savedCourse.getId()).isNotNull();

        Course foundCourse = courseRepository.findById(savedCourse.getId()).orElse(null);

        assertThat(foundCourse).isNotNull();
        assertThat(foundCourse.getName()).isEqualTo("Java Developer");
    }
}
