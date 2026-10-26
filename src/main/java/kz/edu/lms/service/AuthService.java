package kz.edu.lms.service;

import kz.edu.lms.dto.auth.KeycloakTokenResponse;
import kz.edu.lms.dto.auth.LoginRequest;
import kz.edu.lms.dto.auth.RefreshTokenRequest;
import kz.edu.lms.dto.auth.TokenResponse;
import kz.edu.lms.mapper.AuthMapper;
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

import java.util.Map;

@Service
public class AuthService {
    private final RestClient keycloakRestClient;
    private final AuthMapper authMapper;
    private final String clientId;

    public AuthService(RestClient keycloakRestClient, AuthMapper authMapper,
                       @Value("${keycloak.client-id}") String clientId) {
        this.keycloakRestClient = keycloakRestClient;
        this.authMapper = authMapper;
        this.clientId = clientId;
    }

    public TokenResponse login(LoginRequest request) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", request.username());
        form.add("password", request.password());

        return requestTokens(form, "Invalid username or password");
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", request.refreshToken());
        return requestTokens(form, "Invalid or expired refresh token");
    }

    private TokenResponse requestTokens(LinkedMultiValueMap<String, String> form, String errorMessage) {
        KeycloakTokenResponse tokens;
        try {
            tokens = keycloakRestClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(KeycloakTokenResponse.class);
        } catch (RestClientResponseException exception) {
            if (isInvalidGrant(exception)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, errorMessage);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Authentication service unavailable");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Authentication service unavailable");
        }

        if (tokens == null || !StringUtils.hasText(tokens.accessToken())
                || !StringUtils.hasText(tokens.refreshToken())
                || tokens.expiresIn() == null || tokens.expiresIn() <= 0
                || tokens.refreshExpiresIn() == null || tokens.refreshExpiresIn() <= 0
                || !StringUtils.hasText(tokens.tokenType())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid authentication service response");
        }
        return authMapper.toResponse(tokens);
    }

    private boolean isInvalidGrant(RestClientResponseException exception) {
        if (exception.getStatusCode().value() != 400 && exception.getStatusCode().value() != 401) {
            return false;
        }
        try {
            Map<?, ?> error = exception.getResponseBodyAs(Map.class);
            return error != null && "invalid_grant".equals(error.get("error"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
