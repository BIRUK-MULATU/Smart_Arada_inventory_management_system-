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

Phase 1 — project scaffolding.
