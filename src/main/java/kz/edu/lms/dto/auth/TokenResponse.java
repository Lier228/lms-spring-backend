package kz.edu.lms.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        @Schema(description = "Access token lifetime in seconds", example = "300") Long expiresIn,
        @Schema(description = "Refresh token lifetime in seconds", example = "604800") Long refreshExpiresIn,
        @Schema(example = "Bearer") String tokenType
) {
}
