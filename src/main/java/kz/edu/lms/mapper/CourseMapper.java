package kz.edu.lms.mapper;


import kz.edu.lms.dto.course.CourseCreateRequest;
import kz.edu.lms.dto.course.CourseResponse;
import kz.edu.lms.dto.course.CourseUpdateRequest;
import kz.edu.lms.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    Course toEntity(CourseCreateRequest request);

    CourseResponse toResponse(Course course);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdTime", ignore = true)
    @Mapping(target = "updatedTime", ignore = true)
    void updateEntity(
            CourseUpdateRequest request,
            @MappingTarget Course course
    );
}
