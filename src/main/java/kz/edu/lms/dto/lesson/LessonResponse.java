package kz.edu.lms.dto.lesson;

import java.time.LocalDateTime;

public record LessonResponse(
        Long id,
        String name,
        String description,
        String content,
        int sortOrder,
        LocalDateTime createdTime,
        LocalDateTime updatedTime
) {
}
