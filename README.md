# Smart Arada — Inventory & Sales Management System

Offline-first inventory and sales management system. Employees can record
sales without internet; operations queue locally and sync to the server
when connectivity returns.

## Stack

- **Backend:** Java 21, Spring Boot 4.1, Maven, Spring Security + JWT
- **Database:** PostgreSQL 17, Flyway migrations
- **Frontend:** React, TypeScript, Vite, Tailwind CSS, PWA
- **Offline:** IndexedDB via Dexie, sync queue with idempotent retries

## Local setup

Copy `.env.example` to `.env` and set a real `JWT_SECRET`, then start the
database with `docker compose up -d`. PostgreSQL listens on host port 55432.

See `CLAUDE.md` for architecture rules and `docs/` for the full specification.

## Status

Phase 1 complete — backend (Spring Boot) and frontend (Vite/React) scaffolds
build, boot, and pass their smoke tests. No domain features yet (products,
sales, inventory, auth, sync land in later phases).

## Repository layout

```
backend/    Java 21 + Spring Boot 4.1 + Maven, package-by-feature under
            com.example.inventory (config, security, auth, user, product,
            inventory, sale, sync, dashboard, exception, common)
frontend/   React + TypeScript + Vite + Tailwind CSS
docs/       Project specification (source of truth)
```

## Local development

```bash
docker compose up -d              # start PostgreSQL (host port 55432)

cd backend && ./mvnw spring-boot:run   # API on http://localhost:8080
cd backend && ./mvnw test               # backend tests (Testcontainers, needs Docker)
cd backend && ./mvnw spotless:check      # verify formatting

cd frontend && npm install
cd frontend && npm run dev               # dev server on http://localhost:5173
cd frontend && npm run typecheck          # tsc --noEmit
cd frontend && npm run lint                # ESLint
cd frontend && npm test                     # Vitest
```
