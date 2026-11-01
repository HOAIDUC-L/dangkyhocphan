# Repository Guidance

## Project Shape

- This is a Java 17 Maven multi-module project: `common`, `server`, and `client`.
- `common` contains shared models, DTOs, protocol types, and utilities.
- `server` owns TCP server behavior, business services, session management, repositories, and PostgreSQL access.
- `client` owns the Swing student UI and client-side networking. It must not connect to PostgreSQL or contain database credentials.
- Keep changes within the module that owns the behavior. Update shared contracts in `common` only when both client and server need the change.

## Architectural Invariants

- Do not introduce Spring Boot, Hibernate, JPA, or RMI. The server uses Java TCP sockets, JDBC, and PostgreSQL; the UI uses Swing.
- The server is authoritative for authentication, authorization, enrollment rules, and available course slots.
- TCP messages are UTF-8 JSON objects framed by one newline. Preserve request/response correlation and existing action/error-code contracts when changing protocol behavior.
- Use prepared statements for database values. Keep enrollment and slot updates transactional, preserving row-locking and slot-count invariants.
- Keep socket I/O off Swing's Event Dispatch Thread; marshal UI updates back onto the EDT.

## Documentation and Configuration

- Check `docs/architecture.md`, `docs/protocol.md`, and `docs/database.md` before changing their respective contracts.
- Database schema and sample data live in `database/`.
- Treat local `application.properties` values as environment-specific. Never commit real database passwords, tokens, or other secrets; update example configuration when configuration keys change.

## Build and Tests

Run commands from the repository root with Maven 3.8+ and JDK 17+:

- Full verification: `mvn clean package`
- Tests only: `mvn test`
- A module and its required upstream modules: `mvn -pl <module> -am test` (for example, `mvn -pl server -am test`)

Add or update focused JUnit 5 tests for behavior changes. Database-backed checks may require a configured PostgreSQL instance; do not mistake an unavailable local database for a code failure.
