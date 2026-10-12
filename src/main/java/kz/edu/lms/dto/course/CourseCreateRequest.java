package kz.edu.lms.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseCreateRequest(
    @Schema(description = "Display name", example = "Java Fundamentals")
    @NotBlank @Size(max = 255) String name,
    @Schema(description = "Optional description", example = "Learn the core concepts with practical examples.")
    String description
) {
}
