package kz.edu.lms.dto.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        String name,
        @Schema(description = "Relative download URL. Send a Bearer token when requesting it.") String url,
        Long lessonId,
        LocalDateTime createdTime
) {
}
