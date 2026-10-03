package kz.edu.lms;

import kz.edu.lms.entity.Course; // Укажите ваш правильный пакет сущностей
import kz.edu.lms.repository.CourseRepository; // Укажите ваш пакет репозиториев
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

        // Check if it's id is equal to expected
        assertThat(savedCourse.getId()).isNotNull();

        // Check the course by id
        Course foundCourse = courseRepository.findById(savedCourse.getId()).orElse(null);

        assertThat(foundCourse).isNotNull();
        assertThat(foundCourse.getName()).isEqualTo("Java Developer");
    }
}
