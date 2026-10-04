package kz.edu.lms.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        String description
) {
}
