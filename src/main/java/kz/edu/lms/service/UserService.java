package kz.edu.lms.service;

import kz.edu.lms.dto.auth.KeycloakTokenResponse;
import kz.edu.lms.dto.user.*;
import kz.edu.lms.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final RestClient tokenClient;
    private final RestClient adminClient;
    private final UserMapper userMapper;
    private final String clientId;
    private final String clientSecret;

    public UserService(@Qualifier("keycloakRestClient") RestClient tokenClient,
                       @Qualifier("keycloakAdminRestClient") RestClient adminClient,
                       UserMapper userMapper,
                       @Value("${keycloak.admin-client-id}") String clientId,
                       @Value("${keycloak.admin-client-secret}") String clientSecret) {
        this.tokenClient = tokenClient;
        this.adminClient = adminClient;
        this.userMapper = userMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public UserResponse create(UserCreateRequest request) {
        log.info("Creating new user");
        RestClient client = authorizedClient();
        KeycloakRole role;
        try {
            role = client.get().uri("/roles/{name}", request.role().name())
                    .retrieve().body(KeycloakRole.class);
        } catch (RestClientException exception) {
            throw unavailable();
        }
        if (role == null || !StringUtils.hasText(role.id()) || !request.role().name().equals(role.name())) {
            throw unavailable();
        }

        String id;
        try {
            var response = client.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
                    .body(userMapper.toKeycloakUser(request)).retrieve().toBodilessEntity();
            var location = response.getHeaders().getLocation();
            if (response.getStatusCode().value() != 201 || location == null || location.getPath() == null) {
                throw unavailable();
            }
            id = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
            if (!StringUtils.hasText(id)) throw unavailable();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists");
            }
            if (exception.getStatusCode().value() == 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user data or password policy not met");
            }
            throw unavailable();
        } catch (RestClientException exception) {
            throw unavailable();
        }

        try {
            client.post().uri("/users/{id}/role-mappings/realm", id)
                    .contentType(MediaType.APPLICATION_JSON).body(List.of(role)).retrieve().toBodilessEntity();
            client.put().uri("/users/{id}", id).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", true)).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            try {
                client.delete().uri("/users/{id}", id).retrieve().toBodilessEntity();
            } catch (RestClientException cleanupException) {
                log.error("Could not remove incomplete Keycloak user: {}", id);
            }
            throw unavailable();
        }
        log.debug("Created user: id={}, role={}", id, request.role());
        return userMapper.toResponse(id, request);
    }


    public void updateProfile(String userId, UserUpdateRequest request) {
        log.info("Updating user profile");
        try {
            authorizedClient().put().uri("/users/{id}", userId).contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw updateError(exception);
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    public void updatePassword(String userId, PasswordUpdateRequest request) {
        log.info("Updating user password");
        var credential = new KeycloakUserRequest.Credential("password", request.newPassword(), false);
        try {
            authorizedClient().put().uri("/users/{id}/reset-password", userId)
                    .contentType(MediaType.APPLICATION_JSON).body(credential).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw updateError(exception);
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    public void updateRole(String userId, RoleUpdateRequest request) {
        log.info("Updating user role");
        RestClient client = authorizedClient();
        try {
            var role = client.get().uri("/roles/{name}", request.role().name())
                    .retrieve().body(KeycloakRole.class);
            if (role == null || !StringUtils.hasText(role.id()) || !request.role().name().equals(role.name())) {
                throw unavailable();
            }
            var current = client.get().uri("/users/{id}/role-mappings/realm", userId)
                    .retrieve().body(KeycloakRole[].class);
            if (current == null) throw unavailable();
            var applicationRoles = Set.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_USER");
            var previous = Arrays.stream(current).filter(item -> applicationRoles.contains(item.name())).toList();
            if (previous.size() == 1 && previous.getFirst().name().equals(role.name())) return;

            if (!previous.isEmpty()) {
                client.method(org.springframework.http.HttpMethod.DELETE)
                        .uri("/users/{id}/role-mappings/realm", userId).contentType(MediaType.APPLICATION_JSON)
                        .body(previous).retrieve().toBodilessEntity();
            }
            try {
                client.post().uri("/users/{id}/role-mappings/realm", userId).contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(role)).retrieve().toBodilessEntity();
            } catch (RestClientException exception) {
                if (!previous.isEmpty()) {
                    try {
                        client.post().uri("/users/{id}/role-mappings/realm", userId).contentType(MediaType.APPLICATION_JSON)
                                .body(previous).retrieve().toBodilessEntity();
                    } catch (RestClientException restoreException) {
                        log.error("Could not restore roles for Keycloak user: {}", userId);
                    }
                }
                throw unavailable();
            }
        } catch (RestClientResponseException exception) {
            throw updateError(exception);
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private RestClient authorizedClient() {
        return adminClient.mutate().defaultHeaders(headers -> headers.setBearerAuth(adminToken())).build();
    }

    private ResponseStatusException updateError(RestClientResponseException exception) {
        return switch (exception.getStatusCode().value()) {
            case 400 -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user data or password policy not met");
            case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User or role not found");
            case 409 -> new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            default -> unavailable();
        };
    }

    private String adminToken() {
        if (!StringUtils.hasText(clientSecret)) throw unavailable();
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        try {
            var token = tokenClient.post().contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(KeycloakTokenResponse.class);
            if (token == null || !StringUtils.hasText(token.accessToken())) throw unavailable();
            return token.accessToken();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User management service unavailable");
    }

    private record KeycloakRole(String id, String name) {
    }
}
