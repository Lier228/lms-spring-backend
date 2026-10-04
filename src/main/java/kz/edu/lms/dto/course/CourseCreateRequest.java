package kz.edu.lms.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseCreateRequest(
    @NotBlank @Size(max = 255) String name,
    String description
) {
}
