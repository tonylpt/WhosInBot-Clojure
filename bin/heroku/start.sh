#!/bin/bash

set -eo pipefail

: ${TELEGRAM_TOKEN:?"TELEGRAM_TOKEN is not specified."}
: ${DATABASE_URL:?"DATABASE_URL is not specified."}

if [[ -z ${JDBC_DATABASE_URL} ]]; then
    JDBC_DATABASE_URL=$(./database_url_to_jdbc.py ${DATABASE_URL})
fi

exec java $JAVA_OPTS -jar whosin-standalone.jar "$@"
