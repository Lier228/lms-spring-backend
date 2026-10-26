# Authentication

The API uses Spring Security OAuth2 Resource Server. Keycloak issues access and refresh tokens. Spring validates the access token's signature, expiration, issuer, and `lms-backend` audience. Realm roles become `GrantedAuthority` values without adding another `ROLE_` prefix.

Spring Security stores authentication in `JwtAuthenticationToken`. Its `getToken()` method provides the JWT; the principal exposes token claims through `OAuth2AuthenticatedPrincipal`. The authentication name is the user's `sub` identifier. The API uses bearer authentication without HTTP sessions.

## Local setup

From the project root, start Keycloak and its database:

```powershell
docker compose up -d keycloak
docker compose logs -f keycloak
```

The first startup imports `keycloak/lms-realm.json`, which creates realm `lms` and the public OpenID Connect client `lms-postman`. Then apply [keycloak/insert.sql](../keycloak/insert.sql) as described in [the SQL setup guide](keycloak-sql.md) to create the roles and development accounts:

| Username | Password | Realm role |
| --- | --- | --- |
| `student` | `student123` | `ROLE_USER` |
| `teacher` | `teacher123` | `ROLE_TEACHER` |
| `lms-admin` | `admin123` | `ROLE_ADMIN` |

The Keycloak administration account remains `admin` / `admin` in realm `master`, unless overridden. Open http://localhost:8082/admin/ to manage the configuration.

If `lms` already exists, startup import skips it. Existing realms must be updated through the Admin Console instead of expecting the import to overwrite them.

Build the backend JAR and recreate its container:

```powershell
.\gradlew.bat bootJar
docker compose up -d --build app
```

For IDE startup, keep PostgreSQL and Keycloak running, set `SPRING_DOCKER_COMPOSE_ENABLED=false`, and run the application normally.

## Login through the backend

Send **POST** `http://localhost:8081/api/auth/login` with **No Auth** and **Body > raw > JSON** (use port 8080 if running with the default port):

```json
{
  "username": "lms-admin",
  "password": "admin123"
}
```

Response (`200 OK`):

```json
{
  "accessToken": "<JWT>",
  "refreshToken": "<refresh token>",
  "expiresIn": 300,
  "refreshExpiresIn": 604800,
  "tokenType": "Bearer"
}
```

Use `accessToken` as the Bearer token for API requests. Blank fields return 400; incorrect credentials return 401; Keycloak connection or response failures return 502. Passwords and tokens are not logged by the authentication service. Users and roles remain in Keycloak.

The realm uses a 5-minute access token lifetime and 168-hour SSO/client session idle and maximum lifetimes. Refresh tokens are bounded by the session maximum; a token obtained later through refresh has the remaining session lifetime.

For an existing realm, configure **Realm settings > Tokens > Access Token Lifespan** to 5 minutes, and **Realm settings > Sessions**: SSO Session Idle, SSO Session Max, Client Session Idle and Client Session Max to 7 days. Leave client-specific lifetime overrides unset. The import JSON applies these values to new realms only.

Inside Docker, `KEYCLOAK_TOKEN_URL` uses `http://keycloak:8080/realms/lms/protocol/openid-connect/token`. For IDE startup it defaults to localhost:8082. `KEYCLOAK_CLIENT_ID` defaults to `lms-postman`.

## Obtain tokens in Postman

### Username and password (local development)

The local realm enables Direct Access Grants for `lms-postman`. Create a **POST** request to:

```text
http://localhost:8082/realms/lms/protocol/openid-connect/token
```

Select **Authorization > No Auth** and **Body > x-www-form-urlencoded**, then add:

| Field | Value |
| --- | --- |
| `grant_type` | `password` |
| `client_id` | `lms-postman` |
| `username` | `lms-admin` |
| `password` | `admin123` |

Select **Send**. The response contains `access_token` and `refresh_token`. Use `student` or `teacher` and their passwords from the table above to check other roles. Send these fields as form data, not as a JSON body.

For an API request, select **Authorization > Bearer Token** and paste `access_token`. In Swagger UI, select **Authorize** and paste the access token without the `Bearer` prefix.

This password flow is for local development. Use Authorization Code with PKCE for application sign-in.

### Browser sign-in with PKCE

Create a request and select **Authorization > OAuth 2.0 > Configure New Token**:

| Setting | Value |
| --- | --- |
| Grant Type | Authorization Code (With PKCE) |
| Callback URL | `https://oauth.pstmn.io/v1/callback` |
| Authorize using browser | Enabled |
| Auth URL | `http://localhost:8082/realms/lms/protocol/openid-connect/auth` |
| Access Token URL | `http://localhost:8082/realms/lms/protocol/openid-connect/token` |
| Client ID | `lms-postman` |
| Client Secret | Leave empty |
| Code Challenge Method | SHA-256 / S256 |
| Scope | `openid profile email` |

Select **Get New Access Token**, sign in with one of the `lms` demo accounts, then select **Use Token**. This authorization code flow requires PKCE.

Postman sends `Authorization: Bearer <access_token>` to the API. Use the access token, not the ID token or refresh token. In Swagger UI, select **Authorize** and paste the access token.

## Refresh an access token

Send **POST** `http://localhost:8081/api/auth/refresh` (8080 with the default port).
Select **No Auth** and **Body > raw > JSON**:

```json
{
  "refreshToken": "<refreshToken from the login response>"
}
```

The response has the same fields as login: `accessToken`, `refreshToken`, `expiresIn`, `refreshExpiresIn`, and `tokenType`. Save both returned tokens and use the new access token for API calls. An access token is not required for this request; do not send an expired Bearer token.

A missing or blank refresh token returns 400. An invalid or expired refresh token returns 401; sign in again. Refreshing does not extend the session beyond its 168-hour maximum.

## Permissions

| HTTP operation | ROLE_USER | ROLE_TEACHER | ROLE_ADMIN |
| --- | --- | --- | --- |
| GET | Allowed | Allowed | Allowed |
| POST | Forbidden | Allowed | Allowed |
| PUT | Forbidden | Allowed | Allowed |
| DELETE | Forbidden | Forbidden | Allowed |

These rules apply to courses, chapters, and lessons. Swagger UI and `/v3/api-docs` are public.

Check the following cases against the current application port (8080 by default; 8081 if configured):

1. GET `/api/courses/` without a token returns 401.
2. The same request with a student access token returns 200.
3. POST `/api/courses` with a valid JSON body and a student token returns 403.
4. A teacher token allows POST and PUT, but DELETE returns 403.
5. An administrator token allows DELETE; existing business rules still apply, including 409 for nonempty courses or chapters.
6. An invalid or expired access token returns 401.

Security errors use the same `status`, `error`, and `message` JSON fields as other API errors.

## Addresses in Docker and the IDE

The token issuer is `http://localhost:8082/realms/lms`, matching the address used by Postman. Inside Docker, the API retrieves signing keys from `http://keycloak:8080/realms/lms/protocol/openid-connect/certs`. The issuer remains the public address; it is not replaced with the internal container address.

The Compose file sets both addresses and fixes Keycloak's public hostname. For IDE startup, the default issuer and key endpoint use localhost:8082. If the port changes, set `KEYCLOAK_ISSUER_URI` and `KEYCLOAK_JWK_SET_URI` in the IDE environment as well.

## References

- [Spring Security JWT resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Keycloak OpenID Connect endpoints](https://www.keycloak.org/securing-apps/oidc-layers)
