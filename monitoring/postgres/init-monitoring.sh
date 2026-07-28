#!/bin/sh
set -eu

escaped_password=$(printf "%s" "$POSTGRES_EXPORTER_PASSWORD" | sed "s/'/''/g")
psql -v ON_ERROR_STOP=1 <<SQL
SELECT 'CREATE ROLE prometheus LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'prometheus') \gexec
ALTER ROLE prometheus PASSWORD '$escaped_password';
GRANT CONNECT ON DATABASE bakabooru TO prometheus;
GRANT pg_monitor TO prometheus;
SQL
