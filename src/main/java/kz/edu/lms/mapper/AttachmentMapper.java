package kz.edu.lms.mapper;

import kz.edu.lms.dto.attachment.AttachmentResponse;
import kz.edu.lms.entity.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {
    @Mapping(target = "lessonId", source = "lesson.id")
    AttachmentResponse toResponse(Attachment attachment);
}
