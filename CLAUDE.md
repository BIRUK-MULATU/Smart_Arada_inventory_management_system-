# Inventory & Sales Management System

Offline-first inventory and sales system. Full requirements live in
`docs/Claude_Code_Project_Specification_Inventory_Sales_System.docx` —
that document is the source of truth. Read it before starting any phase.

## Stack (locked)

| Layer      | Choice                                                        |
|------------|---------------------------------------------------------------|
| Backend    | Java 21 (Temurin), Spring Boot 4.1.x, Maven                    |
| Security   | Spring Security, JWT, BCrypt                                   |
| Data       | PostgreSQL 17 on host port 55432 (5432-5435 are taken by other local databases), Spring Data JPA / Hibernate, Flyway migrations  |
| Frontend   | React + TypeScript + Vite + Tailwind CSS, React Router         |
| Offline    | IndexedDB via Dexie, service worker / PWA                      |
| Testing    | JUnit 5, Spring Boot Test, Testcontainers, Vitest              |

Monorepo layout: `backend/` and `frontend/` at the root, plus `docs/`.

## Hard rules

- TypeScript everywhere on the frontend. No `.js` application files.
- Never use `any` to silence a type error. Fix the type.
- The server is authoritative for auth, permissions, inventory and business
  rules. Frontend checks are UX only, never security.
- Every private endpoint is protected. Roles (ADMIN / EMPLOYEE) are enforced
  server-side — an employee must not reach an admin endpoint even with a
  hand-crafted HTTP request.
- All multi-step sale operations run inside `@Transactional`. Partial writes
  are never acceptable.
- Stock can never go negative.
- Every sale carries a client transaction ID and must be idempotent. Retries
  must never create a duplicate sale.
- Offline data is never silently discarded. Conflicts get a status and stay
  visible for review.
- Money is `BigDecimal` / `DECIMAL`. Never float or double.
- Schema changes only through Flyway migrations. Never edit an applied
  migration — add a new one.
- Secrets come from environment variables. Nothing real gets committed;
  `.env.example` documents the shape.

## Working agreement

- Inspect the repo before creating or replacing files.
- One phase at a time. Do not build the whole system in one go.
- Explain significant architectural decisions before implementing them, and
  ask before making a business-impacting choice.
- After each milestone: report files changed, commands run, tests run, and
  what the expected result is.
- Run the build, type check and relevant tests after changes.
- Keep API docs and this file current when architecture shifts.

## Phases

1. Repo setup, standards, local dev environment
2. Spring Boot + Maven + PostgreSQL + Flyway foundation
3. Users, BCrypt, Spring Security, JWT, roles, login/logout
4. Product CRUD + image references
5. Inventory and stock history
6. Online sales with transactional stock deduction
7. Admin dashboard and analytics
8. React/TypeScript UI + API integration
9. IndexedDB, PWA, offline sales queue, sync engine
10. Idempotency, conflict handling, retries, sync monitoring
11. Testing, security hardening, performance
12. Deployment, backups, monitoring, docs

## Local commands

```bash
docker compose up -d          # start PostgreSQL
docker compose down           # stop it
docker compose down -v        # stop and wipe the database volume

cd backend && ./mvnw spring-boot:run     # run the API
cd backend && ./mvnw test                # backend tests

cd frontend && npm run dev               # vite dev server
cd frontend && npm run typecheck         # tsc --noEmit
cd frontend && npm test                  # frontend tests
```

Backend: http://localhost:8080 · Frontend: http://localhost:5173
