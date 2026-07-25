#!/usr/bin/env bash
# ==============================================================================
# Turf AI Booking — PostgreSQL Automated Backup Script
# ==============================================================================

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/turfai_db}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/turfai_db_${TIMESTAMP}.sql.gz"

POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-turfai_db}"
POSTGRES_USER="${POSTGRES_USER:-turfai_app}"

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Starting PostgreSQL backup for ${POSTGRES_DB}..."

pg_dump -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -F c | gzip > "${BACKUP_FILE}"

echo "[$(date)] Backup completed successfully: ${BACKUP_FILE}"
echo "[$(date)] Backup size: $(du -sh "${BACKUP_FILE}" | cut -f1)"

# Enforce backup retention policy
echo "[$(date)] Cleaning up backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -name "turfai_db_*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete

echo "[$(date)] Backup process finished."
