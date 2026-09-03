# Backend — Inventory & Sales

Java 21 + Spring Boot 4.1 + Maven. See the root `README.md` and `CLAUDE.md`
for full project context and architecture rules.

## Commands

```bash
./mvnw spring-boot:run     # run the API on http://localhost:8080
./mvnw test                 # unit + integration tests (Testcontainers, needs Docker)
./mvnw compile               # compile only
./mvnw spotless:check         # verify formatting
./mvnw spotless:apply          # auto-format
```

Configuration is read from environment variables (see root `.env.example`);
sensible localhost defaults are baked into `application.yml` so the app
runs against the Docker Compose Postgres instance out of the box.

## Structure

Controller → service → repository layering, package-by-feature under
`com.example.inventory`: `config`, `security`, `auth`, `user`, `product`,
`inventory`, `sale`, `sync`, `dashboard`, `exception`, `common`.

Flyway migrations live in `src/main/resources/db/migration` — none yet;
schema starts in Phase 2.
