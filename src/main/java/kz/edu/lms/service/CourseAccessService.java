package kz.edu.lms.service;

import kz.edu.lms.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseAccessService {
    private final CourseRepository courses;
    private final UserService users;

    // Staff can read all courses. Students are scoped by their Keycloak subject.
    String restrictedReaderId() {
        return restrictedReaderId(SecurityContextHolder.getContext().getAuthentication());
    }

    private String restrictedReaderId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean student = false;
        for (var authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN", "ROLE_TEACHER" -> { return null; }
                case "ROLE_USER" -> student = true;
            }
        }
        if (!student) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course access denied");
        String subject = authentication.getName();
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token does not contain user ID");
        }
        return subject;
    }

    public void checkReadAccess(Long courseId) {
        checkReadAccess(courseId, SecurityContextHolder.getContext().getAuthentication());
    }

    public void checkReadAccess(Long courseId, Authentication authentication) {
        String userId = restrictedReaderId(authentication);
        if (userId != null && !courses.hasAccess(courseId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course access denied");
        }
    }

    @Transactional
    public void grant(Long courseId, UUID userId) {
        requireCourse(courseId);
        users.requireExistingUser(userId.toString());
        courses.grantAccess(courseId, userId.toString());
    }

    @Transactional
    public void revoke(Long courseId, UUID userId) {
        requireCourse(courseId);
        courses.revokeAccess(courseId, userId.toString());
    }

    private void requireCourse(Long courseId) {
        if (!courses.existsById(courseId)) throw new IllegalArgumentException("Course not found");
    }
}
