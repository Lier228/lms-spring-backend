package kz.edu.lms.dto.chapter;

import java.time.LocalDateTime;

public record ChapterResponse(
        Long id,
        String name,
        String description,
        int sortOrder,
        LocalDateTime createdTime,
        LocalDateTime updatedTime
) {
}
