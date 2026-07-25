#!/usr/bin/env bash
# ==============================================================================
# Turf AI Booking — PostgreSQL Restore Utility Script
# ==============================================================================

set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <path_to_backup_file.sql.gz>"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "Error: Backup file '${BACKUP_FILE}' not found."
    exit 1
fi

POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-turfai_db}"
POSTGRES_USER="${POSTGRES_USER:-turfai_app}"

echo "[$(date)] WARNING: Restoring database ${POSTGRES_DB} from ${BACKUP_FILE}..."
read -p "Are you sure you want to overwrite '${POSTGRES_DB}'? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Restore cancelled."
    exit 0
fi

echo "[$(date)] Restoring database..."
gunzip -c "${BACKUP_FILE}" | pg_restore -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --clean --if-exists --no-owner

echo "[$(date)] Database restoration completed successfully."
