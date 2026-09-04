# Deployment runbook

Production target: a single self-hosted VPS running Docker Compose. Caddy
terminates TLS and routes traffic; the backend, frontend, database, and a
backup service all run as containers on the same host. See the "Production
(Phase 12)" section of `CLAUDE.md` for the quick command reference - this
document is the full walkthrough.

## Architecture

```
Internet --443/80--> Caddy (TLS, auto-renews) --+-- /api/*  --> backend:8080 (Spring Boot)
                                                 +-- /*      --> frontend:80 (nginx, static SPA)
                                                                      |
                                                                 backend:8080 --> db:5432 (Postgres 17)
                                                                      |
                                                                 backup service --> ./backups (host bind mount)
```

The frontend is built with `VITE_API_BASE_URL=/api` (relative, same-origin
through Caddy), so it never needs to know its own public domain at build
time, and there's no cross-origin API traffic in production.

## Prerequisites

- A VPS with Docker and the Docker Compose plugin installed.
- A domain name with an `A` record pointing at the VPS's public IP.
- Ports 80 and 443 open (Caddy needs both for the ACME TLS challenge and
  HTTPS traffic).

## First deploy

1. Clone the repo onto the VPS.
2. Copy `.env.example` to `.env` and fill in real values:
   - `POSTGRES_USER` / `POSTGRES_PASSWORD` - pick a real password, not the
     `devpassword` default.
   - `JWT_SECRET` - generate with `openssl rand -base64 48`.
   - `DOMAIN` - your public domain, e.g. `inventory.example.com`.
   - `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` - set these for
     the first deploy only, so the app creates your first admin account on
     startup.
   - `RETENTION_DAYS` - how many days of backups to keep (defaults to 14).
   - Leave `VITE_API_BASE_URL` and local-dev-only vars (`DB_URL` with
     `localhost`, `CORS_ALLOWED_ORIGINS` with `localhost:5173`) as-is -
     `docker-compose.prod.yml` overrides the ones that matter for
     production and ignores the rest.
3. Start the stack:
   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   ```
   This builds the backend and frontend images, starts Postgres, waits for
   it to report healthy, then starts the backend (which runs Flyway
   migrations automatically on boot - no manual migration step), the
   frontend, Caddy, and the backup service.
4. Confirm everything is healthy:
   ```bash
   docker compose -f docker-compose.prod.yml ps
   ```
   All services should show `healthy` (or `running` for services without a
   healthcheck, like `caddy` and `backup`).
5. Visit `https://<your-domain>` - Caddy will have already requested and
   installed a Let's Encrypt certificate. Log in with the bootstrap admin
   credentials.
6. **Immediately after confirming the admin account works**, blank
   `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD` in `.env` and
   redeploy (see "Redeploying" below) - the bootstrap only needs to run
   once, and leaving real credentials in a plaintext `.env` longer than
   necessary is unnecessary risk.

## Redeploying (after a `git pull`)

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

This rebuilds any changed images and restarts only the services whose image
or config changed. Database migrations run automatically as part of the
backend's normal startup - no separate step needed.

## Backups

The `backup` service runs `pg_dump` once a day, writing timestamped custom-
format dumps to `./backups` on the host (a bind mount, so they survive even
if a named volume is ever removed), and prunes anything older than
`RETENTION_DAYS`.

**Restore from a dump:**

```bash
# Stop the backend so nothing writes to the database mid-restore
docker compose -f docker-compose.prod.yml stop backend

docker compose -f docker-compose.prod.yml exec -T db pg_restore \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists \
  < ./backups/inventory-<timestamp>.dump

docker compose -f docker-compose.prod.yml start backend
```

Backups are local-disk only for now. If the VPS itself is lost, the backups
are lost with it - copying `./backups` off-server periodically (e.g. with
`rclone` to an S3-compatible bucket) is a reasonable next step if that risk
matters for your situation, without needing to change the backup script
itself.

## Monitoring

- Every service has a Docker healthcheck; `docker compose -f
  docker-compose.prod.yml ps` is the fastest way to see if anything is
  unhealthy. The backend's healthcheck hits `/actuator/health`, which
  reports `DOWN` if it can't reach the database - so a backend problem and
  a database problem are both visible from the same check.
- Container logs are capped (10MB × 3 files per service) so a noisy service
  can't fill the disk unattended. Tail them with `docker compose -f
  docker-compose.prod.yml logs -f <service>`.
- There's no metrics dashboard (Prometheus/Grafana) by design - that's more
  infrastructure than a small-business deployment needs right now. The
  cheapest useful addition is a free external uptime pinger (e.g.
  UptimeRobot) pointed at `https://<your-domain>` to get an alert if the
  site goes down.

## Notes

- Rotating `JWT_SECRET` invalidates every currently-issued token - all
  logged-in users get signed out. Only do this deliberately (e.g. if the
  secret leaked).
- Postgres isn't exposed to the host in production (no `ports:` mapping in
  `docker-compose.prod.yml`, unlike the local-dev `docker-compose.yml`) -
  only containers on the compose network can reach it.
- Product images live in the `product-images` named volume, mounted at
  `/data/product-images` in the backend container. `docker compose -f
  docker-compose.prod.yml down` (without `-v`) leaves it intact; only
  `down -v` would destroy it.
