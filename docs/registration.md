# LT13: User registration

Only an authenticated user with `ROLE_ADMIN` can create accounts and choose their application role. Users are stored in Keycloak; the backend does not access the Keycloak database. Public Keycloak self-registration is disabled.

## Request

POST `http://localhost:8081/api/users` (8080 with the default port).
In Postman select **Authorization > Bearer Token** and use the administrator's `accessToken` from `/api/auth/login`. Select **Body > raw > JSON**:

```json
{
  "username": "new-student",
  "password": "Student123!",
  "email": "new-student@example.com",
  "firstName": "New",
  "lastName": "Student",
  "role": "ROLE_USER"
}
```

All fields are required. Allowed roles: `ROLE_USER`, `ROLE_TEACHER`, `ROLE_ADMIN`.
Success returns 201 with the Keycloak user ID, username, email, first name, last name, and role. Passwords are never returned. The new account can sign in through `/api/auth/login` immediately.

- 400: invalid input, role, or Keycloak password policy rejection.
- 401: missing or invalid access token.
- 403: the caller is not ROLE_ADMIN.
- 409: username or email already exists.
- 502: Keycloak request failed.

The account is created disabled, assigned its role, then enabled. If assignment or enabling fails, the backend attempts to delete the incomplete account. Cleanup failures are logged with the user ID.

## Keycloak service client

The backend uses the confidential client `lms-user-management` with service accounts enabled. Its service account has `manage-users` and `view-realm` client roles from `realm-management`. Standard flow and Direct Access Grants are disabled for this client. Application users do not receive these management roles.

Store the client secret in the project-root `.env` file:

```dotenv
KEYCLOAK_ADMIN_CLIENT_SECRET=<client secret>
```

The file is excluded from Git and the Docker build context. Docker Compose passes the value to the backend and the Keycloak realm import. For a new installation, generate a random secret and set this variable before the first Keycloak startup. The realm import creates the client and service account; apply the LT10 SQL script for application roles and demo users.

Existing realms are not changed by startup import. In the Keycloak Admin Console, create an OpenID Connect client `lms-user-management`, enable Client authentication and Service accounts roles, disable Standard flow and Direct access grants, assign the two `realm-management` roles listed above, and copy its Credentials > Client secret into `.env`. Disable Realm settings > Login > User registration.

When running from the IDE, set `KEYCLOAK_ADMIN_CLIENT_SECRET` in the run configuration; `.env` is read by Docker Compose, not automatically by Spring Boot. `KEYCLOAK_ADMIN_URL` defaults to `http://localhost:8082/admin/realms/lms`; Compose uses `http://keycloak:8080/admin/realms/lms`.

[Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html)

## LT14: Profile, password, and role updates

All requests require **Authorization > Bearer Token** with the caller's access token. Successful updates return **204 No Content**.

**PUT `/api/users/me`** updates the authenticated user's profile:

```json
{
  "email": "updated@example.com",
  "firstName": "Updated",
  "lastName": "Student"
}
```

All three fields are required. The username is unchanged. The target user ID comes from the JWT subject. Extra fields such as `id`, `role`, `enabled`, or `credentials` are not forwarded to Keycloak.

**PUT `/api/users/me/password`** updates the authenticated user's password:

```json
{
  "newPassword": "NewPassword123!"
}
```

The password must be nonblank and satisfy Keycloak's configured password policy. The same access token remains usable until expiry; this endpoint does not revoke existing sessions.

**PUT `/api/users/{id}/role`** is available only to ROLE_ADMIN. Use the Keycloak user ID returned by registration:

```json
{
  "role": "ROLE_TEACHER"
}
```

This replaces directly assigned application roles (ROLE_ADMIN, ROLE_TEACHER, ROLE_USER), preserving other Keycloak roles. If adding the new role fails, the service attempts to restore the previous roles. These Keycloak requests are not a database transaction. The changed role appears in newly issued tokens; existing access tokens retain their claims until expiry (up to 5 minutes).

A non-admin receives 403 for role updates. Missing authentication returns 401, invalid data 400, missing users 404, conflicting email 409, and Keycloak failures 502.

The `lms-postman` client must include the default client scope `basic` so access tokens contain the `sub` user ID. This is included in the realm import. For an existing realm, assign `basic` as a default client scope and obtain new tokens. Self-update requests without a user ID in the token return 401.
