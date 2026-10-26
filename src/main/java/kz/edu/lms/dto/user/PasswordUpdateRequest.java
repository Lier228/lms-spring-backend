package kz.edu.lms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(
        @NotBlank @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String newPassword
) {
}
