#!/bin/sh
set -eu

primary_user="${POSTGRES_USER:-postgres}"
primary_password="${POSTGRES_PASSWORD:-123456}"
database_name="${POSTGRES_DB:-irms}"

for alias_user in postgres postgre; do
  if [ "$alias_user" = "$primary_user" ]; then
    continue
  fi

  psql -v ON_ERROR_STOP=1 --username "$primary_user" --dbname "$database_name" <<SQL
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${alias_user}') THEN
        CREATE ROLE ${alias_user} LOGIN SUPERUSER PASSWORD '${primary_password}';
    ELSE
        ALTER ROLE ${alias_user} WITH LOGIN SUPERUSER PASSWORD '${primary_password}';
    END IF;
END
\$\$;
SQL
done
