package kz.edu.lms.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "lms-admin") @NotBlank String username,
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) @NotBlank String password
) {
}
