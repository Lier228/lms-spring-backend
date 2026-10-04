package kz.edu.lms.service;

import kz.edu.lms.dto.course.CourseCreateRequest;
import kz.edu.lms.dto.course.CourseResponse;
import kz.edu.lms.dto.course.CourseUpdateRequest;
import kz.edu.lms.entity.Course;
import kz.edu.lms.mapper.CourseMapper;
import kz.edu.lms.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(Pageable pageable) {
        return courseRepository.findAll(pageable).map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found"
                ));

        return courseMapper.toResponse(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        Course course = courseMapper.toEntity(request);

        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseUpdateRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Course not found"
                        )
                );

        courseMapper.updateEntity(request, course);

        courseRepository.flush();
        return courseMapper.toResponse(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course entity = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found"
                ));

        try {
            courseRepository.delete(entity);
            // Execute DELETE here so constraint violations can be translated to HTTP 409.
            courseRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete course: it contains chapters",
                    exception
            );
        }
    }
}
