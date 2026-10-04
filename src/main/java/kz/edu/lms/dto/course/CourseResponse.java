package kz.edu.lms.dto.course;

import java.time.LocalDateTime;

public record CourseResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdTime,
        LocalDateTime updatedTime
) {
}
