package kz.edu.lms.controller;

import kz.edu.lms.dto.user.*;
import kz.edu.lms.service.UserService;
import kz.edu.lms.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {
    private UserService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    private static final String BODY = """
            {"username":"new-user","password":"Password123!","email":"new@example.com",
             "firstName":"New","lastName":"User","role":"ROLE_USER"}
            """;

    @BeforeEach
    void setUp() {
        service = mock(UserService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void tearDown() { validator.close(); }

    @Test
    void returnsCreatedProfileWithoutPassword() throws Exception {
        var request = new UserCreateRequest("new-user", "Password123!", "new@example.com", "New", "User", Role.ROLE_USER);
        when(service.create(request)).thenReturn(new UserResponse("user-id", "new-user", "new@example.com", "New", "User", Role.ROLE_USER));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("user-id"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.credentials").doesNotExist());
        verify(service).create(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "password", "email", "firstName", "lastName", "role"})
    void requiredFieldsAreValidated(String field) throws Exception {
        String invalid = BODY.replaceAll("\"" + field + "\":\"[^\"]*\"", "\"" + field + "\":null");
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.trace").doesNotExist());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsRoleOutsideApplicationRoles() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("ROLE_USER", "realm-admin")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedEmail() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("new@example.com", "invalid")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void duplicateReturnsSafeConflict() throws Exception {
        when(service.create(any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists"));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Username or email already exists"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
    private org.springframework.security.core.Authentication principal() {
        return new org.springframework.security.authentication.TestingAuthenticationToken("subject-id", null);
    }

    @Test
    void profileUsesAuthenticatedSubjectAndIgnoresRoleAndTargetId() throws Exception {
        mvc.perform(put("/api/users/me").principal(principal()).contentType(MediaType.APPLICATION_JSON).content("""
                {"id":"victim-id","role":"ROLE_ADMIN","enabled":false,
                 "email":"updated@example.com","firstName":"Updated","lastName":"User"}
                """))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).updateProfile("subject-id", new UserUpdateRequest("updated@example.com", "Updated", "User"));
        verifyNoMoreInteractions(service);
    }

    @Test
    void passwordUsesAuthenticatedSubject() throws Exception {
        mvc.perform(put("/api/users/me/password").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"NewPassword123!\",\"id\":\"victim-id\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).updatePassword("subject-id", new PasswordUpdateRequest("NewPassword123!"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/users/me", "/api/users/me/password", "/api/users/user-id/role"})
    void updateRequestsRequireFields(String path) throws Exception {
        mvc.perform(put(path).principal(principal()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void roleUpdateUsesPathIdAndValidatedRole() throws Exception {
        mvc.perform(put("/api/users/user-id/role").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_TEACHER\"}"))
                .andExpect(status().isNoContent());
        verify(service).updateRole("user-id", new RoleUpdateRequest(Role.ROLE_TEACHER));
    }

    @Test
    void rejectsInvalidRoleUpdate() throws Exception {
        mvc.perform(put("/api/users/user-id/role").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"realm-admin\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void tokenWithoutUserIdCannotChangeProfile() throws Exception {
        var authentication = mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("");
        mvc.perform(put("/api/users/me").principal(authentication).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"updated@example.com","firstName":"Updated","lastName":"User"}
                """))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
