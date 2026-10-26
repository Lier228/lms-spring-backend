package kz.edu.lms.dto.user;

public record UserResponse(String id, String username, String email, String firstName, String lastName, Role role) {
}
