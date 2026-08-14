#!/bin/bash
# =============================================================================
# Enterprise Order Processing Platform - SQL Server Database Initialization
# =============================================================================
#
# One-shot helper that creates the application databases on the SQL Server
# container. Runs as a separate compose service after SQL Server is healthy.
#
# Usage:
#   docker-compose --profile init up sqlserver-init
# =============================================================================

set -e

# Locate sqlcmd (path differs between mssql-tools and mssql-tools18)
if [ -x /opt/mssql-tools18/bin/sqlcmd ]; then
  SQLCMD="/opt/mssql-tools18/bin/sqlcmd"
elif [ -x /opt/mssql-tools/bin/sqlcmd ]; then
  SQLCMD="/opt/mssql-tools/bin/sqlcmd"
else
  echo "[init] ERROR: sqlcmd not found."
  exit 1
fi

HOST="${SQLSERVER_HOST:-sqlserver}"
PORT="${SQLSERVER_PORT:-1433}"
SA_PASSWORD="${SA_PASSWORD:-}"

if [ -z "$SA_PASSWORD" ]; then
  echo "[init] ERROR: SA_PASSWORD is not set."
  exit 1
fi

CMD() {
  "$SQLCMD" -C -S "$HOST,$PORT" -U sa -P "$SA_PASSWORD" -b -Q "$1"
}

echo "[init] Creating databases on $HOST:$PORT..."

for db in order_db payment_db saga_db; do
  if CMD "SELECT 1 FROM sys.databases WHERE name='$db'" | grep -q "1"; then
    echo "[init] Database '$db' already exists. Skipping."
  else
    echo "[init] Creating database '$db'..."
    CMD "CREATE DATABASE [$db]"
  fi
done

echo "[init] Database initialization complete."
