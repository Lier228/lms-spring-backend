package kz.edu.lms.service;

import kz.edu.lms.dto.auth.LoginRequest;
import kz.edu.lms.dto.auth.RefreshTokenRequest;
import kz.edu.lms.mapper.AuthMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AuthServiceTest {
    private MockRestServiceServer server;
    private AuthService service;
    private final LoginRequest request = new LoginRequest("user+test", "p&ss=word+ test");

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://keycloak.test/token");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new AuthService(builder.build(), Mappers.getMapper(AuthMapper.class), "lms-postman");
    }

    @Test
    void sendsEncodedCredentialsAndMapsTokens() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", "lms-postman");
        form.add("username", request.username());
        form.add("password", request.password());
        server.expect(requestTo("http://keycloak.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(form))
                .andRespond(withSuccess("""
                    {"access_token":"access","refresh_token":"refresh","expires_in":300,
                     "refresh_expires_in":604800,"token_type":"Bearer","session_state":"ignored"}
                    """, MediaType.APPLICATION_JSON));
        var result = service.login(request);
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        assertEquals(300L, result.expiresIn());
        assertEquals(604800L, result.refreshExpiresIn());
        assertEquals("Bearer", result.tokenType());
        server.verify();
    }

    @Test
    void invalidCredentialsReturnUnauthorizedWithoutUpstreamDetails() {
        server.expect(requestTo("http://keycloak.test/token")).andRespond(withBadRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"invalid_grant\",\"error_description\":\"private detail\"}"));
        var error = assertThrows(ResponseStatusException.class, () -> service.login(request));
        assertEquals(401, error.getStatusCode().value());
        assertEquals("Invalid username or password", error.getReason());
        assertNull(error.getCause());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid_client", "unauthorized_client"})
    void clientConfigurationErrorsReturnBadGateway(String code) {
        server.expect(requestTo("http://keycloak.test/token")).andRespond(withBadRequest()
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"" + code + "\"}"));
        assertBadGateway();
    }

    @Test
    void serverErrorReturnsBadGateway() {
        server.expect(requestTo("http://keycloak.test/token")).andRespond(withServerError());
        assertBadGateway();
    }

    @Test
    void connectionFailureReturnsBadGateway() {
        server.expect(requestTo("http://keycloak.test/token")).andRespond(withException(new IOException("connection refused")));
        assertBadGateway();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "not-json", "{\"access_token\":\"access\"}",
            "{\"access_token\":\"access\",\"refresh_token\":\"refresh\",\"expires_in\":0,\"refresh_expires_in\":604800,\"token_type\":\"Bearer\"}"})
    void incompleteOrMalformedResponsesReturnBadGateway(String body) {
        server.expect(requestTo("http://keycloak.test/token")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertBadGateway();
    }


    @Test
    void refreshSendsTokenAndReturnsUpdatedPair() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", "lms-postman");
        form.add("refresh_token", "old+refresh/token=");
        server.expect(requestTo("http://keycloak.test/token"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().formData(form))
                .andRespond(withSuccess("""
                    {"access_token":"new-access","refresh_token":"new-refresh","expires_in":300,
                     "refresh_expires_in":600000,"token_type":"Bearer"}
                    """, MediaType.APPLICATION_JSON));
        var result = service.refresh(new RefreshTokenRequest("old+refresh/token="));
        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
        assertEquals(300L, result.expiresIn());
        assertEquals(600000L, result.refreshExpiresIn());
        server.verify();
    }

    @Test
    void invalidRefreshTokenReturnsUnauthorized() {
        server.expect(requestTo("http://keycloak.test/token")).andRespond(withBadRequest()
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"invalid_grant\"}"));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.refresh(new RefreshTokenRequest("expired")));
        assertEquals(401, error.getStatusCode().value());
        assertEquals("Invalid or expired refresh token", error.getReason());
        assertNull(error.getCause());
        server.verify();
    }

    @Test
    void refreshConnectionFailureReturnsBadGateway() {
        server.expect(requestTo("http://keycloak.test/token")).andRespond(withException(new IOException("connection refused")));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.refresh(new RefreshTokenRequest("refresh")));
        assertEquals(502, error.getStatusCode().value());
        assertNull(error.getCause());
        server.verify();
    }

    private void assertBadGateway() {
        var error = assertThrows(ResponseStatusException.class, () -> service.login(request));
        assertEquals(502, error.getStatusCode().value());
        assertNull(error.getCause());
        server.verify();
    }
}
