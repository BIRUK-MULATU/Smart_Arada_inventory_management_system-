#!/bin/sh
set -eu

while true; do
  timestamp=$(date +%Y%m%d-%H%M%S)
  pg_dump -h "$PGHOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -F c \
    -f "/backups/inventory-${timestamp}.dump"
  find /backups -name 'inventory-*.dump' -mtime +"${RETENTION_DAYS}" -delete
  sleep 86400
done
