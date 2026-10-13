# Java Developer curriculum

52 new English lessons for Java 21 and Spring Boot. With the original seed, the course has 13 chapters and 56 lessons.

Each lesson includes a learning goal, explanation, worked example, practice task, acceptance criteria and common mistake. The code fragments are teaching examples; framework snippets assume project imports and dependencies.

## 1. Java Development Essentials

Set up a Java 21 development environment, understand types and methods, and build a reproducible Gradle workflow before developing the LMS.

- **JDK, JVM and Your First Program**: Compile and run a Java program and distinguish source code, bytecode and the runtime.
- **Variables, Numeric Types and Conversion**: Select suitable types and avoid accidental integer division and overflow.
- **Strings, Null and Immutable Values**: Compare text by value and normalize user input deliberately.
- **Methods, Packages and the Gradle Wrapper**: Organize reusable logic and run the same build outside the IDE.

## 2. Control Flow

Use conditions, loops and small functions to implement predictable validation and collection processing.

- **Switch Expressions and Exhaustive Choices**: Use a switch expression to map a finite domain to a value.
- **Loop Invariants and Boundary Cases**: Reason about what remains true while a loop processes data.
- **Guard Clauses and Validation Order**: Reject invalid inputs early and keep the successful path readable.
- **Workshop: Build a Console Gradebook**: Combine methods, loops and validation into a small complete program.

## 3. Object-Oriented Programming

Model domain behavior, use interfaces and composition, and understand identity and value objects.

- **Interfaces and Dependency Boundaries**: Depend on a behavior contract so implementations can change independently.
- **Composition, Inheritance and Responsibilities**: Choose object relationships that match the domain instead of reusing code through inheritance.
- **Records, Equality and Hash Codes**: Distinguish value-based DTOs from mutable entities with generated identity.
- **Workshop: Model Course Enrollment Rules**: Express a business invariant through a small object API.

## 4. Collections and Generics

Choose lists, sets and maps, use generic contracts, and transform course data without losing ordering or type safety.

- **Lists, Sets and Choosing a Collection**: Select a collection according to ordering, duplicates and lookup requirements.
- **Maps and Grouping by Identifier**: Use key-value lookup and define a policy for duplicate keys.
- **Generics and Type-Safe Reuse**: Write a reusable method without raw collections or unchecked casts.
- **Streams, Sorting and Collecting Results**: Build a readable data pipeline and make ordering explicit.

## 5. Exceptions, Files and Time

Design error contracts, release resources reliably and handle text files and timestamps explicitly.

- **Exception Types and Preserving Causes**: Choose meaningful exceptions and retain diagnostic context when translating them.
- **Try-with-Resources and Reliable Cleanup**: Close resources even when processing fails.
- **UTF-8 Files and Safe Path Handling**: Read and write predictable text and restrict paths to an intended directory.
- **LocalDateTime, Instant and Time Zones**: Represent dates and instants according to their meaning.

## 6. Modern Java and Concurrency

Use lambdas and Optional deliberately, understand shared state and apply virtual threads to appropriate workloads.

- **Lambdas and Functional Interfaces**: Pass behavior as a value without hiding complex business logic.
- **Optional and Missing Data Contracts**: Use Optional for a possibly absent result and handle absence at the correct layer.
- **Race Conditions and Atomic Operations**: Recognize that a compound operation can fail under concurrent execution.
- **Virtual Threads and Blocking Work**: Use Java 21 virtual threads while keeping resource limits explicit.

## 7. SQL and PostgreSQL

Design related tables, query course data, enforce constraints and manage changes with Liquibase.

- **Tables, Keys and Relationships**: Translate the LMS model into a relational schema.
- **SELECT, JOIN and Stable Ordering**: Retrieve related rows without relying on physical storage order.
- **Transactions, Constraints and Indexes**: Protect multi-step writes and index real query patterns.
- **Liquibase Changesets and Seed Data**: Evolve schema and content without rewriting applied migration history.

## 8. Spring Boot Fundamentals

Understand startup, constructor injection, external configuration and transactional service boundaries.

- **Application Startup and Component Scanning**: Explain how Spring discovers controllers, services and configuration.
- **Constructor Injection and Required Dependencies**: Make collaborators explicit and test services without Spring startup.
- **Properties, Environment Variables and Profiles**: Run the same application in an IDE and inside containers.
- **Service Transactions and Layered Responsibilities**: Place business operations at a clear transaction boundary.

## 9. REST APIs and Validation

Build HTTP contracts, validate DTOs, query nested resources and return consistent errors.

- **HTTP Methods and Resource Routes**: Choose methods and status codes that match each operation.
- **Request DTOs and Bean Validation**: Reject invalid input before invoking business operations.
- **Pagination and Parent-Scoped Queries**: Bound query results while preserving order and parent ownership.
- **Controller Advice and Error Contracts**: Provide useful errors without exposing stack traces or SQL.

## 10. Persistence with JPA

Map entities and relationships, use repositories, update managed objects and diagnose lazy-loading query problems.

- **Entity Mapping and Generated Identifiers**: Map entity fields to schema constraints without mixing in API concerns.
- **Derived Queries and Ownership Checks**: Express repository lookups that match nested API contracts.
- **Dirty Checking, Flush and Auditing**: Understand when managed changes reach SQL and when response timestamps are populated.
- **Lazy Loading, N+1 and DTO Boundaries**: Detect unnecessary queries before adding fetch strategies.

## 11. Testing with JUnit and Mockito

Write regression tests for services, HTTP contracts, validation and database-specific behavior.

- **JUnit Assertions and Test Structure**: Write tests that describe behavior with deterministic inputs.
- **Mockito Repositories and Real Mappers**: Isolate storage while checking actual DTO conversion.
- **MockMvc and Controller Contracts**: Test routing, validation and advice through the HTTP layer.
- **Integration Tests and a Regression Workflow**: Separate fast isolated checks from tests requiring real infrastructure.

## 12. Observability and API Documentation

Use purposeful logs, define OpenAPI contracts and investigate failures from evidence.

- **SLF4J Levels and Useful Messages**: Choose INFO, DEBUG and ERROR messages that help explain application behavior.
- **OpenAPI Schemas and Endpoint Documentation**: Describe actual requests, responses and constraints in Swagger UI.
- **Troubleshooting from HTTP to Database**: Narrow a failure using the request, response and server logs.
- **Operational Review and API Acceptance**: Validate a release using a repeatable sequence of realistic actions.

## 13. Docker and Delivery

Package the service, connect it to PostgreSQL, protect persistent data and publish a reproducible release.

- **Multi-Stage Java Images**: Separate build tools from the runtime image.
- **Compose Networking, Ports and Readiness**: Connect containers using service names and diagnose host-port conflicts.
- **Persistent Volumes, Backups and Migrations**: Keep database data outside replaceable containers and test recovery.
- **Capstone: Publish and Demonstrate the LMS**: Deliver a tagged image and a repeatable end-to-end demonstration.

## Migration behavior

Migration 003 extends the uniquely named Java Developer course, or creates it if absent. It fails if several courses have that name. Existing chapters are reused and matching lesson titles are skipped. Existing lesson text is preserved. The new foundations chapter comes first; other chapter positions shift by one. Only the unchanged original seed course description is replaced.

The SQL runs in one transaction. It has no automatic destructive rollback. Rebuild the Docker image to include the migration on another installation. Original migrations 001 and 002 are unchanged.

Verification: 13 chapters and 56 lessons on the original seed; rerunning SQL creates no duplicates; existing lesson contents are unchanged.

## Further reading

- Java: https://dev.java/learn/
- Transactions: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
- JPA: https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html
- Compose readiness: https://docs.docker.com/compose/how-tos/startup-order/
