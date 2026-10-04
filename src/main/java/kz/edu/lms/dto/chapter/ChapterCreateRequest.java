package kz.edu.lms.dto.chapter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChapterCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Min(1) Integer sortOrder,
        String description
) {
}
