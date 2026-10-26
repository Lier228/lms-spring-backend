package kz.edu.lms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.edu.lms.dto.user.*;
import kz.edu.lms.exception.ApiError;
import kz.edu.lms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "Create a user", description = "ROLE_ADMIN only. Creates a Keycloak account and assigns the selected application role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid user data or role", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "ROLE_ADMIN required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Username or email already exists", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "502", description = "Keycloak request failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody @Valid UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }
    @Operation(summary = "Update my profile", description = "Updates email, first name and last name of the authenticated user. Username and roles are unchanged.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Updated", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "502", description = "Keycloak request failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/me")
    public ResponseEntity<Void> updateProfile(Authentication authentication, @RequestBody @Valid UserUpdateRequest request) {
        userService.updateProfile(currentUserId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update my password", description = "Changes the password of the authenticated user. Keycloak password policy applies.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Updated", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "502", description = "Keycloak request failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(Authentication authentication, @RequestBody @Valid PasswordUpdateRequest request) {
        userService.updatePassword(currentUserId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update user role", description = "ROLE_ADMIN only. Replaces application roles with the selected role. Other Keycloak roles are preserved.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Updated", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "502", description = "Keycloak request failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/role")
    public ResponseEntity<Void> updateRole(@PathVariable String id, @RequestBody @Valid RoleUpdateRequest request) {
        userService.updateRole(id, request);
        return ResponseEntity.noContent().build();
    }

    private String currentUserId(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token does not contain user ID");
        }
        return authentication.getName();
    }
}
