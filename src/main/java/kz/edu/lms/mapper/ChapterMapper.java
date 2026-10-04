package kz.edu.lms.mapper;

import kz.edu.lms.dto.chapter.ChapterCreateRequest;
import kz.edu.lms.dto.chapter.ChapterResponse;
import kz.edu.lms.dto.chapter.ChapterUpdateRequest;
import kz.edu.lms.entity.Chapter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ChapterMapper {
    @Mapping(target = "course", ignore = true)
    Chapter toEntity(ChapterCreateRequest request);

    ChapterResponse toResponse(Chapter chapter);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdTime", ignore = true)
    @Mapping(target = "updatedTime", ignore = true)
    @Mapping(target = "course", ignore = true)
    void updateEntity(
            ChapterUpdateRequest request,
            @MappingTarget Chapter chapter
    );
}
