# Keycloak seed data (LT10)

`keycloak/insert.sql` inserts initial data into Keycloak's own PostgreSQL database. It targets the Keycloak 26.8.1 schema and is executed separately from the LMS Liquibase migrations.

The realm JSON creates realm `lms` and its Postman client. SQL creates three realm roles and three local development accounts:

| Username | Initial password | Role |
| --- | --- | --- |
| `student` | `student123` | `ROLE_USER` |
| `teacher` | `teacher123` | `ROLE_TEACHER` |
| `lms-admin` | `admin123` | `ROLE_ADMIN` |

`ROLE_ADMIN` manages LMS content. Keycloak's administration account remains in realm `master`.

## Apply the script

Run these commands in PowerShell from the project root.

First start Keycloak so it can create its tables and import realm `lms`:

```powershell
docker compose up -d keycloak
docker compose logs -f keycloak
```

Wait until the Admin Console opens at http://localhost:8082/admin/. Press Ctrl+C to leave log viewing. Stop Keycloak while writing directly to its database:

```powershell
docker compose stop keycloak
Get-Content -Raw -Encoding UTF8 .\keycloak\insert.sql | docker compose exec -T keycloak-db psql -U keycloak -d keycloak -v ON_ERROR_STOP=1
```

A successful run ends with `DO` and `COMMIT`. Start Keycloak again to load the inserted data:

```powershell
docker compose start keycloak
```

Existing users, passwords, and settings are preserved. Running the script again adds missing roles and role assignments without duplicating existing rows. A seed password is written only for a newly created account.

The script stores salted PBKDF2-SHA256 hashes, not plaintext passwords. Keycloak can upgrade a hash on successful login according to its password policy.

## Database separation

`keycloak-db` is connected only to the internal `keycloak-internal` network and has no published port. Keycloak connects to both that network and the application's default network. The backend calls Keycloak over HTTP to verify tokens and has no direct network connection to its database.

SQL is run by the operator through `docker compose exec`, using the database container's own PostgreSQL client. No Keycloak database credentials or datasource are added to the LMS backend.

The script is intentionally not placed in `/docker-entrypoint-initdb.d`: PostgreSQL initializes before Keycloak creates its tables. Run SQL after the first Keycloak startup.

## Verify

Sign in through Postman using the instructions in [authentication.md](authentication.md). Tokens should contain the user's role in `realm_access.roles`.

To inspect the assignments:

```powershell
docker compose exec -T keycloak-db psql -U keycloak -d keycloak -c "SELECT u.username, r.name AS role FROM user_entity u JOIN realm re ON re.id = u.realm_id JOIN user_role_mapping m ON m.user_id = u.id JOIN keycloak_role r ON r.id = m.role_id WHERE re.name = 'lms' AND r.name IN ('ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_USER') ORDER BY u.username;"
```
