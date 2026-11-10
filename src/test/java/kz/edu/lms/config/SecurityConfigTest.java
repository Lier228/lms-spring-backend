package kz.edu.lms.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = OpenApiDocumentationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.docker.compose.enabled=false", "spring.servlet.multipart.max-file-size=1KB",
                "spring.servlet.multipart.max-request-size=4KB"})
class SecurityConfigTest {
    private static final String ISSUER = "https://issuer.example.test/realms/lms";
    private static final String AUDIENCE = "lms-backend";
    private static final String BODY = "{\"name\":\"Security test\",\"sortOrder\":1}";
    private static final RSAKey KEY;
    private static final HttpServer KEYS_SERVER;

    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            KEYS_SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            KEYS_SERVER.createContext("/certs", exchange -> {
                byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            KEYS_SERVER.start();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + KEYS_SERVER.getAddress().getPort() + "/certs");
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> AUDIENCE);
    }

    @Autowired kz.edu.lms.service.AttachmentService attachmentService;
    @Autowired Environment environment;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter;

    @AfterAll
    static void stopKeysServer() { KEYS_SERVER.stop(0); }


    @Test
    void loginIsPublicButOtherAuthRoutesAreNot() throws Exception {
        String base = "http://localhost:" + environment.getProperty("local.server.port");
        var client = HttpClient.newHttpClient();
        var login = HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"student\",\"password\":\"password\"}")).build();
        assertEquals(200, client.send(login, HttpResponse.BodyHandlers.ofString()).statusCode());
        var invalid = HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        assertEquals(400, client.send(invalid, HttpResponse.BodyHandlers.ofString()).statusCode());
        for (String path : java.util.List.of("/api/auth/login", "/api/auth/other")) {
            assertEquals(401, client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }


    @Test
    void refreshIsPublicWithoutAccessToken() throws Exception {
        String base = "http://localhost:" + environment.getProperty("local.server.port");
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create(base + "/api/auth/refresh"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"refreshToken\":\"refresh\"}")).build();
        assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        var invalid = HttpRequest.newBuilder(URI.create(base + "/api/auth/refresh"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        assertEquals(400, client.send(invalid, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    static Stream<Arguments> endpoints() {
        return Stream.of("/course", "/chapter", "/lesson")
                .flatMap(base -> Stream.of(
                        Arguments.of("GET", base, 200),
                        Arguments.of("GET", base + "/1", 200),
                        Arguments.of("POST", base, 201),
                        Arguments.of("PUT", base + "/1", 200),
                        Arguments.of("DELETE", base + "/1", 204)));
    }

    static Stream<Arguments> permissions() {
        return Stream.of("ROLE_USER", "ROLE_TEACHER", "ROLE_ADMIN").flatMap(role ->
                endpoints().map(endpoint -> {
                    Object[] values = endpoint.get();
                    String method = (String) values[0];
                    boolean allowed = role.equals("ROLE_ADMIN") || method.equals("GET");
                    return Arguments.of(role, method, values[1], allowed ? values[2] : 403);
                }));
    }

    @ParameterizedTest
    @MethodSource("permissions")
    void enforcesRolesOnEveryEndpoint(String role, String method, String path, int expectedStatus) throws Exception {
        var response = request(method, path, token(Map.of("roles", List.of(role))));
        assertEquals(expectedStatus, response.statusCode(), response.body());
        assertTrue(response.headers().allValues("Set-Cookie").isEmpty());
        if (expectedStatus == 403) assertTrue(response.body().contains("Access denied"));
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void requiresAccessTokenOnEveryEndpoint(String method, String path, int ignored) throws Exception {
        var response = request(method, path, null);
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Authentication required"));
        assertEquals("Bearer", response.headers().firstValue("WWW-Authenticate").orElseThrow());
        assertFalse(response.body().contains("trace"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "issuer", "audience", "signature", "malformed"})
    void rejectsInvalidTokens(String failure) throws Exception {
        String jwt = failure.equals("malformed") ? "not-a-jwt" : signedToken(
                failure.equals("signature") ? new RSAKeyGenerator(2048).keyID("test-key").generate() : KEY,
                failure.equals("issuer") ? "https://wrong.example.test" : ISSUER,
                failure.equals("audience") ? "another-api" : AUDIENCE,
                Instant.now().plusSeconds(failure.equals("expired") ? -120 : 300),
                Map.of("roles", List.of("ROLE_ADMIN")));
        var response = request("GET", "/course", jwt);
        assertEquals(401, response.statusCode(), response.body());
        assertTrue(response.body().contains("Authentication required"));
        assertFalse(response.body().contains("trace"));
    }

    @Test
    void tokenWithoutApplicationRoleIsForbidden() throws Exception {
        for (Object roles : List.of(Map.of(), Map.of("roles", List.of("offline_access", "ADMIN")),
                Map.of("roles", "ROLE_ADMIN"), "invalid")) {
            assertEquals(403, request("GET", "/course", token(roles)).statusCode());
        }
    }

    @Test
    void createsJwtPrincipalAndAuthoritiesWithoutDuplicatingRolePrefix() throws Exception {
        Jwt jwt = jwtDecoder.decode(token(Map.of("roles", List.of("ROLE_USER", "ROLE_USER", "offline_access"))));
        var authentication = converter.convert(jwt);
        assertNotNull(authentication);
        var jwtAuthentication = assertInstanceOf(
                org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class,
                authentication);
        assertEquals(jwt, jwtAuthentication.getToken());
        var principal = assertInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal.class,
                authentication.getPrincipal());
        assertEquals("test-user-id", principal.getAttribute("sub"));
        assertEquals("test-user-id", authentication.getName());
        assertEquals(List.of("ROLE_USER"), authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(role -> role.startsWith("ROLE_")).toList());
    }

    @Test
    void rolesFromAnotherClaimDoNotGrantAccess() throws Exception {
        var jwt = Jwt.withTokenValue("test").header("alg", "RS256")
                .subject("user").claim("scope", "ROLE_ADMIN")
                .claim("resource_access", Map.of("other-client", Map.of("roles", List.of("ROLE_ADMIN"))))
                .build();
        assertTrue(new KeycloakAuthoritiesConverter().convert(jwt).isEmpty());
    }

    @Test
    void authorizedInvalidRequestStillReturnsValidationError() throws Exception {
        var response = request("POST", "/course", token(Map.of("roles", List.of("ROLE_ADMIN"))), "{}");
        assertEquals(400, response.statusCode(), response.body());
    }


    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_TEACHER", "ROLE_USER", "NONE"})
    void onlyAdminCanCreateUsersAndChooseAnyApplicationRole(String callerRole) throws Exception {
        for (String assignedRole : List.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_USER")) {
            String body = """
                    {"username":"new-user","password":"Password123!","email":"new@example.com",
                     "firstName":"New","lastName":"User","role":"%s"}
                    """.formatted(assignedRole);
            String jwt = callerRole.equals("NONE") ? null : token(Map.of("roles", List.of(callerRole)));
            int expected = callerRole.equals("NONE") ? 401 : callerRole.equals("ROLE_ADMIN") ? 201 : 403;
            var result = request("POST", "/api/users", jwt, body);
            assertEquals(expected, result.statusCode(), result.body());
        }
    }

    private String token(Object realmAccess) throws Exception {
        return signedToken(KEY, ISSUER, AUDIENCE, Instant.now().plusSeconds(300), realmAccess);
    }

    private static String signedToken(RSAKey key, String issuer, String audience, Instant expiration,
                                      Object realmAccess) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("test-user-id")
                .issueTime(Date.from(Instant.now().minusSeconds(600)))
                .expirationTime(Date.from(expiration)).claim("realm_access", realmAccess).build();
        var signed = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        signed.sign(new RSASSASigner(key));
        return signed.serialize();
    }

    private HttpResponse<String> request(String method, String path, String token) throws Exception {
        return request(method, path, token, BODY);
    }

    private HttpResponse<String> request(String method, String path, String token, String body) throws Exception {
        if (path.equals("/lesson") || path.startsWith("/lesson/")) path += "?chapterId=1";
        if (path.equals("/chapter") || path.startsWith("/chapter/")) path += "?courseId=1";
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getProperty("local.server.port") + path)).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, method.equals("POST") || method.equals("PUT")
                ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @ParameterizedTest
    @ValueSource(strings = {"ROLE_USER", "ROLE_TEACHER", "ROLE_ADMIN", "NO_ROLE"})
    void allAuthenticatedUsersCanUpdateTheirOwnProfileAndPassword(String role) throws Exception {
        String jwt = token(Map.of("roles", role.equals("NO_ROLE") ? List.of() : List.of(role)));
        String profile = "{\"email\":\"updated@example.com\",\"firstName\":\"Updated\",\"lastName\":\"User\"}";
        assertEquals(204, request("PUT", "/api/users/me", jwt, profile).statusCode());
        assertEquals(204, request("PUT", "/api/users/me/password", jwt, "{\"newPassword\":\"NewPassword123!\"}").statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_USER", "ROLE_TEACHER", "ROLE_ADMIN", "NONE"})
    void onlyAdminCanChangeExistingUserRoles(String role) throws Exception {
        String jwt = role.equals("NONE") ? null : token(Map.of("roles", List.of(role)));
        int expected = role.equals("NONE") ? 401 : role.equals("ROLE_ADMIN") ? 204 : 403;
        for (String target : List.of("test-user-id", "another-user-id")) {
            var response = request("PUT", "/api/users/" + target + "/role", jwt, "{\"role\":\"ROLE_ADMIN\"}");
            assertEquals(expected, response.statusCode(), response.body());
        }
    }

    @Test
    void selfUpdatesRequireAuthenticationAndDoNotOpenOtherUserRoutes() throws Exception {
        assertEquals(401, request("PUT", "/api/users/me", null, "{}").statusCode());
        assertEquals(401, request("PUT", "/api/users/me/password", null, "{}").statusCode());
        String jwt = token(Map.of("roles", List.of("ROLE_USER")));
        assertEquals(403, request("PUT", "/api/users/another-user-id", jwt, "{}").statusCode());
        assertEquals(403, request("PUT", "/api/users/another-user-id/password", jwt, "{}").statusCode());
    }

    @Test
    void oldCourseCreationRouteIsNotAvailable() throws Exception {
        var response = request("POST", "/api/courses", token(Map.of("roles", List.of("ROLE_ADMIN"))));
        assertEquals(404, response.statusCode());
    }

    @Test
    void oldChapterCreationRouteIsNotAvailable() throws Exception {
        var response = request("POST", "/api/courses/1/chapters", token(Map.of("roles", List.of("ROLE_ADMIN"))));
        assertEquals(404, response.statusCode());
    }

    @Test
    void oldLessonCreationRouteIsNotAvailable() throws Exception {
        var response = request("POST", "/api/chapters/1/lessons", token(Map.of("roles", List.of("ROLE_ADMIN"))));
        assertEquals(404, response.statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_TEACHER", "ROLE_USER", "NO_ROLE", "NONE"})
    void fileEndpointsEnforcePermissions(String role) throws Exception {
        var id = java.util.UUID.randomUUID();
        org.mockito.Mockito.when(attachmentService.download(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
                new kz.edu.lms.service.FileService.Download(new org.springframework.core.io.InputStreamResource(
                        new java.io.ByteArrayInputStream(new byte[]{1, 2})), "lesson.txt", 2));
        org.mockito.Mockito.when(attachmentService.upload(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(
                new kz.edu.lms.dto.attachment.AttachmentResponse(1L, "lesson.txt", "/download?url=" + id, 1L, java.time.LocalDateTime.now()));
        String jwt = role.equals("NONE") ? null : token(Map.of("roles", role.equals("NO_ROLE") ? List.of() : List.of(role)));
        int readStatus = role.equals("NONE") ? 401 : role.equals("NO_ROLE") ? 403 : 200;
        int writeStatus = role.equals("NONE") ? 401 : (role.equals("ROLE_ADMIN") || role.equals("ROLE_TEACHER")) ? 201 : 403;
        assertEquals(readStatus, request("GET", "/download?url=" + id, jwt).statusCode());
        var upload = uploadFile(jwt, "ok");
        assertEquals(writeStatus, upload.statusCode(), upload.body());
    }

    @Test
    void oversizedUploadReturns413WithoutTrace() throws Exception {
        var response = uploadFile(token(Map.of("roles", List.of("ROLE_ADMIN"))), "x".repeat(2048));
        assertEquals(413, response.statusCode(), response.body());
        assertTrue(response.body().contains("413"));
        assertFalse(response.body().contains("trace"));
    }

    private HttpResponse<String> uploadFile(String jwt, String content) throws Exception {
        String body = "--test-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"lesson.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n" + content + "\r\n--test-boundary--\r\n";
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + environment.getProperty("local.server.port") + "/upload?lessonId=1"))
                .header("Content-Type", "multipart/form-data; boundary=test-boundary")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (jwt != null) request.header("Authorization", "Bearer " + jwt);
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void legacyFileRoutesCannotBypassAttachmentChecks() throws Exception {
        for (String role : List.of("ROLE_USER", "ROLE_TEACHER", "ROLE_ADMIN")) {
            String jwt = token(Map.of("roles", List.of(role)));
            assertEquals(403, request("GET", "/file/" + java.util.UUID.randomUUID(), jwt).statusCode());
            assertEquals(403, request("POST", "/file", jwt).statusCode());
        }
    }

}
