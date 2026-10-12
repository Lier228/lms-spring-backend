package kz.edu.lms.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CourseResponse(
        @Schema(description = "Record ID", example = "1")
        Long id,
        @Schema(description = "Display name", example = "Java Fundamentals")
        String name,
        @Schema(description = "Optional description", example = "Learn the core concepts with practical examples.")
        String description,
        @Schema(description = "Creation time", example = "2026-10-13T10:00:00")
        LocalDateTime createdTime,
        @Schema(description = "Last modification time", example = "2026-10-13T10:00:00")
        LocalDateTime updatedTime
) {
}
