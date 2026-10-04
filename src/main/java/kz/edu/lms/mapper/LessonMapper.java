package kz.edu.lms.mapper;

import kz.edu.lms.dto.lesson.LessonCreateRequest;
import kz.edu.lms.dto.lesson.LessonResponse;
import kz.edu.lms.dto.lesson.LessonUpdateRequest;
import kz.edu.lms.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LessonMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdTime", ignore = true)
    @Mapping(target = "updatedTime", ignore = true)
    @Mapping(target = "chapter", ignore = true)
    Lesson toEntity(LessonCreateRequest request);

    LessonResponse toResponse(Lesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdTime", ignore = true)
    @Mapping(target = "updatedTime", ignore = true)
    @Mapping(target = "chapter", ignore = true)
    void updateEntity(
            LessonUpdateRequest request,
            @MappingTarget Lesson lesson
    );
}
