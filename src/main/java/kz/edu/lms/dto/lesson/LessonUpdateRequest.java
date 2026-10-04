package kz.edu.lms.dto.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LessonUpdateRequest(
        @NotBlank @Size(max=255) String name,
        String description,
        String content,
        @NotNull @Min(1) Integer sortOrder
) {
}
