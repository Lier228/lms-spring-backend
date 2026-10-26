package kz.edu.lms.controller;

import kz.edu.lms.dto.auth.*;
import kz.edu.lms.service.AuthService;
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

class AuthControllerTest {
    private AuthService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        service = mock(AuthService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void tearDown() { validator.close(); }

    @Test
    void loginReturnsTokensAndDisablesCaching() throws Exception {
        var request = new LoginRequest("student", "password");
        when(service.login(request)).thenReturn(new TokenResponse("access", "refresh", 300L, 604800L, "Bearer"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800))
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(service).login(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"username\":\"student\"}", "{\"password\":\"password\"}",
            "{\"username\":\" \",\"password\":\"password\"}", "{\"username\":\"student\",\"password\":\" \"}", "not-json"})
    void rejectsInvalidInput(String body) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());
        verifyNoInteractions(service);
    }

    @Test
    void wrongCredentialsReturnSafeUnauthorizedResponse() throws Exception {
        when(service.login(any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void keycloakFailureReturnsSafeBadGatewayResponse() throws Exception {
        when(service.login(any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Authentication service unavailable"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student\",\"password\":\"password\"}"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
    @Test
    void refreshReturnsUpdatedTokensWithoutCaching() throws Exception {
        var request = new RefreshTokenRequest("old-refresh");
        when(service.refresh(request)).thenReturn(new TokenResponse("new-access", "new-refresh", 300L, 600000L, "Bearer"));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
        verify(service).refresh(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"refreshToken\":null}", "{\"refreshToken\":\" \"}"})
    void refreshRejectsMissingOrBlankToken(String body) throws Exception {
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.trace").doesNotExist());
        verifyNoInteractions(service);
    }

    @Test
    void expiredRefreshTokenReturnsSafeUnauthorizedResponse() throws Exception {
        when(service.refresh(any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expired\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
