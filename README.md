# LMS Spring Backend

REST API for a learning management system built with Java and Spring Boot. Courses contain ordered chapters, and chapters contain ordered lessons with text or HTML content.

The project includes CRUD operations, request validation, database migrations, API documentation, automated tests, and a Docker Compose setup with PostgreSQL.

## Technology stack

| Component | Technology |
| --- | --- |
| Language | Java 21 |
| Application | Spring Boot 4.1.1, Spring MVC |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL |
| Migrations | Liquibase |
| Validation | Jakarta Bean Validation |
| DTO mapping | MapStruct 1.6.3, Lombok |
| API documentation | OpenAPI, Swagger UI, springdoc 3.1.1 |
| Logging | SLF4J |
| Testing | JUnit, Mockito, MockMvc |
| Build and deployment | Gradle Wrapper, Docker, Docker Compose |

## Features

- Create, read, update, and delete courses, chapters, and lessons.
- Query chapters within a course and lessons within a chapter.
- Paginate and sort collection responses.
- Validate request DTOs and return consistent JSON errors.
- Protect courses and chapters from deletion while they contain child records.
- Track creation and update timestamps with JPA auditing.
- Initialize the database and course content through Liquibase.
- Explore all 15 API operations in Swagger UI.

Spring Security validates Keycloak access tokens and applies ROLE_USER, ROLE_TEACHER, and ROLE_ADMIN permissions. Keycloak runs in Docker Compose with its own PostgreSQL database.

## Run with Docker Compose

Start Docker Desktop. Use JDK 21 and the included Gradle Wrapper to build the executable JAR, then create the image from the project root:

```powershell
.\gradlew.bat bootJar
docker compose up -d --build
docker compose logs -f app
```

The Dockerfile copies the prebuilt `build/libs/lms-backend-0.0.1-SNAPSHOT.jar` into a Java 21 runtime image and runs it as a non-root user. If that JAR is already up to date, skip `bootJar`. Docker does not compile the application.

Compose builds the image, starts PostgreSQL, and waits for the database health check before starting the application. Liquibase applies pending migrations, and Hibernate validates the schema. After changing source code or migrations, rebuild the JAR before rebuilding the image.

If the JAR name changes, update `JAR_FILE` in the Dockerfile or pass the new path when building:

```powershell
docker compose build --build-arg JAR_FILE=build/libs/lms-backend-1.0.0.jar app
docker compose up -d --no-build
```

| Resource | Default address |
| --- | --- |
| Swagger UI | [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI JSON | [localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Courses | [localhost:8080/api/courses/](http://localhost:8080/api/courses/) |
| PostgreSQL from the host | `localhost:5433` |
| Keycloak Admin Console | [localhost:8082/admin/](http://localhost:8082/admin/) |

If port `8080` is occupied, select a different host port before starting Compose:

```powershell
$env:APP_PORT = "8081"
docker compose up -d --build
```

Use `http://localhost:8081` for the application URLs in that case. Inside Docker, the app still listens on `8080` and connects to PostgreSQL at `postgres:5432`.

### Configuration

Set these variables in your shell or in a local `.env` file next to `compose.yaml`:

| Variable | Default | Purpose |
| --- | --- | --- |
| `APP_IMAGE` | `lms-backend:local` | Application image name and tag |
| `APP_PORT` | `8080` | Application port on the host |
| `POSTGRES_PORT` | `5433` | PostgreSQL port on the host |
| `POSTGRES_DB` | `lms_db` | Database name |
| `POSTGRES_USER` | `postgres` | Database user |
| `POSTGRES_PASSWORD` | `postgres` | Database password |

The defaults are for local development. PostgreSQL initialization variables apply when the database is first created; changing them does not update existing database credentials.

### Stop and resume

```powershell
docker compose stop
docker compose start
```

The LMS PostgreSQL service uses the image's anonymous volume. Keeping the containers with `stop` preserves their volume association. After removing and recreating the database container, Compose may attach a new anonymous volume. Back up existing data before changing database storage or removing containers and volumes.

## Keycloak and API security

Compose includes Keycloak 26.8.1 and a separate PostgreSQL 17 service named `keycloak-db`. Start them from the project root:

```powershell
docker compose up -d keycloak
docker compose logs -f keycloak
```

Open [the Admin Console](http://localhost:8082/admin/) and sign in with username `admin` and password `admin`. These are local development defaults; Keycloak runs in `start-dev` mode and its port is bound to localhost.

Optional shell or `.env` variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `KEYCLOAK_PORT` | `8082` | Keycloak port on the host |
| `KEYCLOAK_ADMIN` | `admin` | Initial administrator username |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Initial administrator password |
| `KEYCLOAK_DB_PASSWORD` | `keycloak` | Password shared by Keycloak and its database |

Keycloak waits for its database health check and connects to `keycloak-db:5432` over a private Docker network. The backend does not share that network, and the Keycloak database has no published port. Database data is stored in the named volume `keycloak-data`, which Compose prefixes with the project name. Keep this volume to preserve users and settings when recreating containers. Changing initialization variables does not reset an existing administrator or database password.

At first startup, Keycloak imports `keycloak/lms-realm.json`, which creates realm `lms` and the `lms-postman` client. Apply `keycloak/insert.sql` to create the three roles and local development accounts; see [the LT10 SQL setup guide](docs/keycloak-sql.md). An existing `lms` realm is left unchanged by startup import.

| Role | Permissions | Demo login | Demo password |
| --- | --- | --- | --- |
| `ROLE_USER` | Read courses, chapters, and lessons | `student` | `student123` |
| `ROLE_TEACHER` | Read, create, and update | `teacher` | `teacher123` |
| `ROLE_ADMIN` | Read, create, update, and delete | `lms-admin` | `admin123` |

These accounts belong to realm `lms`. The bootstrap `admin` account manages Keycloak in realm `master`.

See [authentication setup and Postman instructions](docs/authentication.md) for obtaining access and refresh tokens, calling the API, and checking permissions. Swagger UI and OpenAPI documentation remain public; API requests require an access token.

## Run locally with Java

Install JDK 21 and use the included Gradle Wrapper. Keep PostgreSQL in Docker while running the application from the IDE or terminal:

```powershell
docker compose stop app
docker compose up -d postgres
$env:SPRING_DOCKER_COMPOSE_ENABLED = "false"
.\gradlew.bat bootRun
```

For IDE startup, set `SPRING_DOCKER_COMPOSE_ENABLED=false` in the run configuration. The default local JDBC URL is `jdbc:postgresql://localhost:5433/lms_db`, with username and password `postgres`.

If you changed the database configuration, also set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` for the local application. Use `SERVER_PORT` to override its HTTP port; `APP_PORT` controls the Docker port mapping only.

On macOS or Linux, replace `.\gradlew.bat` with `./gradlew` and use your shell's environment-variable syntax.

## API

All paths below are relative to the application address. Request and response schemas are available in Swagger UI.

| Method | Path | Operation |
| --- | --- | --- |
| GET | `/api/courses/` | List courses |
| POST | `/api/courses` | Create a course |
| GET | `/api/courses/{id}` | Get a course |
| PUT | `/api/courses/{id}` | Update a course |
| DELETE | `/api/courses/{id}` | Delete a course |
| GET | `/api/courses/{courseId}/chapters` | List chapters in a course |
| POST | `/api/courses/{courseId}/chapters` | Create a chapter in a course |
| GET | `/api/courses/{courseId}/chapters/{id}` | Get a chapter in a course |
| PUT | `/api/courses/{courseId}/chapters/{id}` | Update a chapter in a course |
| DELETE | `/api/courses/{courseId}/chapters/{id}` | Delete a chapter in a course |
| GET | `/api/chapters/{chapterId}/lessons` | List lessons in a chapter |
| POST | `/api/chapters/{chapterId}/lessons` | Create a lesson in a chapter |
| GET | `/api/chapters/{chapterId}/lessons/{id}` | Get a lesson in a chapter |
| PUT | `/api/chapters/{chapterId}/lessons/{id}` | Update a lesson in a chapter |
| DELETE | `/api/chapters/{chapterId}/lessons/{id}` | Delete a lesson in a chapter |

The course list URL includes a trailing slash. Chapters and lessons are scoped to their parent: an ID belonging to another parent returns `404`.

### Pagination and sorting

Collection endpoints return a Spring Data `Page` with items in `content` and pagination metadata.

- `page`: zero-based page number; default `0`.
- `size`: default `10`, maximum `100`.
- `sort`: `field,asc` or `field,desc`; repeat the parameter for multiple fields.
- Default ordering: courses by `id`; chapters and lessons by `sortOrder`, then `id`.

```http
GET /api/courses/?page=0&size=10&sort=id,desc
GET /api/courses/1/chapters?page=0&size=20&sort=sortOrder,asc&sort=id,asc
GET /api/chapters/1/lessons?page=0&size=20
```

### Create a lesson

Send this request with the ID of an existing chapter:

```http
POST /api/chapters/1/lessons
Content-Type: application/json

{
  "name": "Java Collections Overview",
  "description": "Choose between lists, sets, and maps.",
  "content": "<h2>Collections</h2><p>A List preserves element order and allows duplicates.</p>",
  "sortOrder": 5
}
```

Names are required and limited to 255 characters. Chapter and lesson requests require a `sortOrder` of at least `1`. Descriptions and lesson content are optional. `PUT` replaces editable fields, so omitted optional fields are cleared.

### Responses and errors

Successful reads and updates return `200`, creation returns `201`, and deletion returns `204` without a response body.

Errors handled by the controller advice use this structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Course not found"
}
```

| Status | Meaning |
| --- | --- |
| `400 Bad Request` | Invalid request data or parameters |
| `401 Unauthorized` | Missing, invalid, or expired access token |
| `403 Forbidden` | The authenticated user lacks the required role |
| `404 Not Found` | Resource is missing or does not belong to the specified parent |
| `409 Conflict` | Course still has chapters, or chapter still has lessons |
| `500 Internal Server Error` | Unexpected server failure |

Unexpected errors return a generic message. Stack traces stay in server logs. Delete child records before deleting their parent.

## Course content and migrations

A fresh database receives the **Java Developer** course with **13 chapters and 56 lessons** in English. Topics cover Java fundamentals, OOP, collections, exceptions, concurrency, SQL, Spring Boot, REST APIs, JPA, testing, logging, documentation, and Docker.

The 52 expanded lessons include learning goals, explanations, code examples, practice tasks, acceptance criteria, and common mistakes. See the [curriculum guide](docs/java-developer-curriculum.md) for the outline and migration behavior.

Liquibase changesets live in `src/main/resources/db/changelog`. Migration `003` extends the existing Java Developer course while preserving existing lesson text. Rebuild the JAR and application image after adding migrations so future deployments include them.

## Tests

Run service, controller, error-handler, and OpenAPI tests without a database:

```powershell
.\gradlew.bat test --tests "*ServiceTest" --tests "*ControllerTest" --tests "*GlobalExceptionHandlerTest" --tests "*OpenApiDocumentationTest" --tests "*SecurityConfigTest"
```

Security tests use signed JWTs and a local public-key endpoint to check token validation and role permissions on every API endpoint. Service tests use mocked repositories and real MapStruct mappers. Controller tests use MockMvc with mocked services to check requests, validation, responses, and errors.

The full `test` task also includes `LmsBackendApplicationTests`, which connects to PostgreSQL and saves a course without rolling it back. Use a separate test database and set the datasource environment variables before running:

```powershell
.\gradlew.bat test
```

Neither `bootJar` nor the Docker image build runs tests. Run the isolated suite before publishing an image.

## Logging

- `INFO`: messages about service operations.
- `DEBUG`: request and object details.
- `ERROR`: validation failures, missing resources, and exceptions.

Application logging defaults to `DEBUG`; framework logging defaults to `INFO`. Levels are configured in `application.properties`.

## Publish to Docker Hub

Replace `YOUR_DOCKERHUB_USERNAME` with your Docker Hub account and choose a release tag:

```powershell
docker login
$env:APP_IMAGE = "YOUR_DOCKERHUB_USERNAME/lms-backend:1.0.0"
.\gradlew.bat bootJar
docker compose build app
docker compose push app
```

On another computer, copy `compose.yaml` and the `keycloak` directory, set `APP_IMAGE` to the published image, and run from the directory containing the Compose file:

```powershell
$env:APP_IMAGE = "YOUR_DOCKERHUB_USERNAME/lms-backend:1.0.0"
docker compose pull
docker compose up -d --no-build
```

The image contains the application and migrations. Database contents are stored separately; Liquibase initializes a fresh database on startup.

## Project structure

```text
src/main/java/kz/edu/lms/
  config/       Security and OpenAPI configuration
  controller/   REST endpoints
  dto/          Request and response models
  entity/       JPA entities and audit fields
  exception/    Error responses and controller advice
  mapper/       MapStruct mappings
  repository/   Spring Data JPA repositories
  service/      Business operations and transactions
src/main/resources/
  application.properties
  db/changelog/ Liquibase schema and data migrations
src/test/java/  Service, controller, API, and database tests
docs/          Curriculum and authentication documentation
keycloak/      Local realm configuration
Dockerfile
compose.yaml
```
