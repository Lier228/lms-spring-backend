package kz.edu.lms.dto.chapter;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChapterUpdateRequest(
        @Schema(description = "Display name", example = "Java Fundamentals")
        @NotBlank @Size(max = 255) String name,
        @Schema(description = "Position within the parent; starts at 1", example = "1")
        @NotNull @Min(1) Integer sortOrder,
        @Schema(description = "Optional description", example = "Learn the core concepts with practical examples.")
        String description
) {
}
