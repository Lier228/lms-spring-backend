package kz.edu.lms.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response without stack traces or internal exception details")
public record ApiError(
        @Schema(description = "HTTP status code", example = "404") int status,
        @Schema(description = "HTTP status name", example = "Not Found") String error,
        @Schema(description = "Human-readable error message", example = "Course not found") String message
) {
}
