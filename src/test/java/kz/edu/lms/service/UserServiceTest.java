package kz.edu.lms.service;

import kz.edu.lms.dto.user.*;
import kz.edu.lms.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UserServiceTest {
    private MockRestServiceServer tokens;
    private MockRestServiceServer admin;
    private UserService service;
    private static final String BASE = "http://keycloak.test/admin/realms/lms";

    @BeforeEach
    void setUp() {
        var tokenBuilder = RestClient.builder().baseUrl("http://keycloak.test/token");
        var adminBuilder = RestClient.builder().baseUrl(BASE);
        tokens = MockRestServiceServer.bindTo(tokenBuilder).build();
        admin = MockRestServiceServer.bindTo(adminBuilder).build();
        service = new UserService(tokenBuilder.build(), adminBuilder.build(), Mappers.getMapper(UserMapper.class), "lms-user-management", "secret");
    }

    private UserCreateRequest request(Role role) {
        return new UserCreateRequest("new-user", "Password123!", "new@example.com", "New", "User", role);
    }

    private void expectToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", "lms-user-management");
        form.add("client_secret", "secret");
        tokens.expect(requestTo("http://keycloak.test/token")).andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(form))
                .andRespond(withSuccess("{\"access_token\":\"service-token\"}", MediaType.APPLICATION_JSON));
    }

    private void expectRole(Role role) {
        admin.expect(requestTo(BASE + "/roles/" + role)).andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("{\"id\":\"role-id\",\"name\":\"" + role + "\"}", MediaType.APPLICATION_JSON));
    }

    private void expectCreated() {
        admin.expect(requestTo(BASE + "/users")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.username").value("new-user"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.credentials[0].type").value("password"))
                .andExpect(jsonPath("$.credentials[0].value").value("Password123!"))
                .andExpect(jsonPath("$.credentials[0].temporary").value(false))
                .andExpect(jsonPath("$.role").doesNotExist())
                .andRespond(withCreatedEntity(URI.create(BASE + "/users/user-id")));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void createsDisabledUserAssignsSelectedRoleAndEnablesAccount(Role role) {
        expectToken(); expectRole(role); expectCreated();
        admin.expect(requestTo(BASE + "/users/user-id/role-mappings/realm")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].id").value("role-id")).andExpect(jsonPath("$[0].name").value(role.name()))
                .andRespond(withNoContent());
        admin.expect(requestTo(BASE + "/users/user-id")).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.enabled").value(true)).andRespond(withNoContent());
        var result = service.create(request(role));
        assertEquals("user-id", result.id());
        assertEquals("new-user", result.username());
        assertEquals("new@example.com", result.email());
        assertEquals(role, result.role());
        tokens.verify(); admin.verify();
    }

    @Test
    void duplicateReturnsConflictWithoutDeletingExistingUser() {
        expectToken(); expectRole(Role.ROLE_USER);
        admin.expect(requestTo(BASE + "/users")).andRespond(withStatus(HttpStatus.CONFLICT));
        assertFailure(409);
    }

    @Test
    void invalidProfileOrPasswordPolicyReturnsBadRequest() {
        expectToken(); expectRole(Role.ROLE_USER);
        admin.expect(requestTo(BASE + "/users")).andRespond(withBadRequest().body("private details"));
        assertFailure(400);
    }

    @Test
    void failedRoleAssignmentDeletesIncompleteAccount() {
        expectToken(); expectRole(Role.ROLE_USER); expectCreated();
        admin.expect(requestTo(BASE + "/users/user-id/role-mappings/realm")).andRespond(withServerError());
        admin.expect(requestTo(BASE + "/users/user-id")).andExpect(method(HttpMethod.DELETE)).andRespond(withNoContent());
        assertFailure(502);
    }

    @Test
    void failedEnableDeletesIncompleteAccount() {
        expectToken(); expectRole(Role.ROLE_USER); expectCreated();
        admin.expect(requestTo(BASE + "/users/user-id/role-mappings/realm")).andRespond(withNoContent());
        admin.expect(requestTo(BASE + "/users/user-id")).andExpect(method(HttpMethod.PUT)).andRespond(withServerError());
        admin.expect(requestTo(BASE + "/users/user-id")).andExpect(method(HttpMethod.DELETE)).andRespond(withNoContent());
        assertFailure(502);
    }

    @Test
    void failedCleanupStillReturnsSafeError() {
        expectToken(); expectRole(Role.ROLE_USER); expectCreated();
        admin.expect(requestTo(BASE + "/users/user-id/role-mappings/realm")).andRespond(withServerError());
        admin.expect(requestTo(BASE + "/users/user-id")).andExpect(method(HttpMethod.DELETE)).andRespond(withServerError());
        assertFailure(502);
    }

    @Test
    void unknownRoleInKeycloakDoesNotCreateUser() {
        expectToken();
        admin.expect(requestTo(BASE + "/roles/ROLE_USER")).andRespond(withResourceNotFound());
        assertFailure(502);
    }

    @Test
    void tokenConnectionFailureDoesNotCallAdminApi() {
        tokens.expect(requestTo("http://keycloak.test/token")).andRespond(withException(new IOException("connection refused")));
        assertFailure(502);
    }

    @Test
    void invalidServiceCredentialsReturnGatewayError() {
        tokens.expect(requestTo("http://keycloak.test/token")).andRespond(withUnauthorizedRequest());
        assertFailure(502);
    }

    private void assertFailure(int status) {
        var error = assertThrows(ResponseStatusException.class, () -> service.create(request(Role.ROLE_USER)));
        assertEquals(status, error.getStatusCode().value());
        assertNull(error.getCause());
        tokens.verify(); admin.verify();
    }
    @Test
    void updatesOnlyProfileFieldsForSpecifiedSubject() {
        expectToken();
        admin.expect(requestTo(BASE + "/users/subject-id")).andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                    {"email":"updated@example.com","firstName":"Updated","lastName":"User"}
                    """, org.springframework.test.json.JsonCompareMode.STRICT)).andRespond(withNoContent());
        service.updateProfile("subject-id", new UserUpdateRequest("updated@example.com", "Updated", "User"));
        tokens.verify(); admin.verify();
    }

    @Test
    void updatesPasswordAsPermanentCredential() {
        expectToken();
        admin.expect(requestTo(BASE + "/users/subject-id/reset-password")).andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                    {"type":"password","value":"NewPassword123!","temporary":false}
                    """, org.springframework.test.json.JsonCompareMode.STRICT)).andRespond(withNoContent());
        service.updatePassword("subject-id", new PasswordUpdateRequest("NewPassword123!"));
        tokens.verify(); admin.verify();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {400, 404, 409, 500})
    void profileErrorsAreMappedSafely(int upstreamStatus) {
        expectToken();
        admin.expect(requestTo(BASE + "/users/subject-id")).andRespond(withStatus(HttpStatus.valueOf(upstreamStatus)).body("private details"));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateProfile("subject-id", new UserUpdateRequest("updated@example.com", "Updated", "User")));
        assertEquals(upstreamStatus == 500 ? 502 : upstreamStatus, error.getStatusCode().value());
        assertNull(error.getCause());
        tokens.verify(); admin.verify();
    }

    @Test
    void passwordPolicyFailureReturnsSafeBadRequest() {
        expectToken();
        admin.expect(requestTo(BASE + "/users/subject-id/reset-password")).andRespond(withBadRequest());
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updatePassword("subject-id", new PasswordUpdateRequest("short")));
        assertEquals(400, error.getStatusCode().value());
        tokens.verify(); admin.verify();
    }

    private void expectCurrentRoles() {
        expectToken(); expectRole(Role.ROLE_TEACHER);
        admin.expect(requestTo(BASE + "/users/subject-id/role-mappings/realm"))
                .andRespond(withSuccess("""
                    [{"id":"old-role","name":"ROLE_USER"},{"id":"default-role","name":"default-roles-lms"}]
                    """, MediaType.APPLICATION_JSON));
        admin.expect(requestTo(BASE + "/users/subject-id/role-mappings/realm")).andExpect(method(HttpMethod.DELETE))
                .andExpect(content().json("""
                    [{"id":"old-role","name":"ROLE_USER"}]
                    """, org.springframework.test.json.JsonCompareMode.STRICT)).andRespond(withNoContent());
    }

    @Test
    void roleUpdateReplacesApplicationRolesAndPreservesOtherRoles() {
        expectCurrentRoles();
        admin.expect(requestTo(BASE + "/users/subject-id/role-mappings/realm")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].name").value("ROLE_TEACHER")).andRespond(withNoContent());
        service.updateRole("subject-id", new RoleUpdateRequest(Role.ROLE_TEACHER));
        tokens.verify(); admin.verify();
    }

    @Test
    void failedRoleUpdateRestoresPreviousApplicationRoles() {
        expectCurrentRoles();
        admin.expect(requestTo(BASE + "/users/subject-id/role-mappings/realm")).andRespond(withServerError());
        admin.expect(requestTo(BASE + "/users/subject-id/role-mappings/realm")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].name").value("ROLE_USER")).andRespond(withNoContent());
        var error = assertThrows(ResponseStatusException.class, () -> service.updateRole("subject-id", new RoleUpdateRequest(Role.ROLE_TEACHER)));
        assertEquals(502, error.getStatusCode().value());
        tokens.verify(); admin.verify();
    }

    @Test
    void unchangedRoleNeedsNoMutation() {
        expectToken(); expectRole(Role.ROLE_USER);
        admin.expect(requestTo(BASE + "/users/subject-id/role-mappings/realm"))
                .andRespond(withSuccess("[{\"id\":\"role-id\",\"name\":\"ROLE_USER\"}]", MediaType.APPLICATION_JSON));
        service.updateRole("subject-id", new RoleUpdateRequest(Role.ROLE_USER));
        tokens.verify(); admin.verify();
    }

    @Test
    void missingUserForRoleUpdateReturnsNotFound() {
        expectToken(); expectRole(Role.ROLE_USER);
        admin.expect(requestTo(BASE + "/users/missing/role-mappings/realm")).andRespond(withResourceNotFound());
        var error = assertThrows(ResponseStatusException.class, () -> service.updateRole("missing", new RoleUpdateRequest(Role.ROLE_USER)));
        assertEquals(404, error.getStatusCode().value());
        tokens.verify(); admin.verify();
    }

}
