#!/usr/bin/env sh
set -eu

if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
  echo "Creating databases: ${POSTGRES_MULTIPLE_DATABASES}"
  echo "${POSTGRES_MULTIPLE_DATABASES}" | tr ',' ' ' | while read -r database_names; do
    for database_name in ${database_names}; do
      psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" <<-EOSQL
        CREATE DATABASE ${database_name};
        GRANT ALL PRIVILEGES ON DATABASE ${database_name} TO ${POSTGRES_USER};
EOSQL
    done
  done
fi
