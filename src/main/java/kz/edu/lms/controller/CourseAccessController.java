package kz.edu.lms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.edu.lms.exception.ApiError;
import kz.edu.lms.service.CourseAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Courses")
@RestController
@RequestMapping("/course/{courseId}/access/{userId}")
@RequiredArgsConstructor
public class CourseAccessController {
    private final CourseAccessService access;

    @Operation(summary = "Grant course access", description = "ROLE_ADMIN only. Use the Keycloak user ID. Repeating this request returns 204.")
    @ApiResponse(responseCode = "204", description = "Access updated", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid ID", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Administrator required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Course or user not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "502", description = "Keycloak unavailable", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grant(@PathVariable Long courseId, @PathVariable UUID userId) {
        access.grant(courseId, userId);
    }

    @Operation(summary = "Revoke course access", description = "ROLE_ADMIN only. Use the Keycloak user ID. Repeating this request returns 204.")
    @ApiResponse(responseCode = "204", description = "Access updated", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid ID", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Administrator required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long courseId, @PathVariable UUID userId) {
        access.revoke(courseId, userId);
    }
}
