#!/bin/bash

set -eu

if [[ -n ${POSTGRES_TEST_DB} ]]; then
    echo "Creating test database ${POSTGRES_TEST_DB}"
    psql -v ON_ERROR_STOP=1 <<-EOSQL
	    CREATE DATABASE ${POSTGRES_TEST_DB};
	    GRANT ALL PRIVILEGES ON DATABASE ${POSTGRES_TEST_DB} TO ${POSTGRES_USER};
	EOSQL
fi
