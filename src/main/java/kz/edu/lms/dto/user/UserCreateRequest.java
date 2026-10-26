package kz.edu.lms.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotNull Role role
) {
}
