package kz.edu.lms.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "Refresh token returned by login", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank String refreshToken
) {
}
