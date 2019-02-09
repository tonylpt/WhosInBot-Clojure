FROM clojure:lein as builder
WORKDIR /app

COPY project.clj ./project.clj
RUN lein deps

COPY resources ./resources
COPY resources/config.docker.edn resources/config.edn
COPY src ./src

RUN lein do clean, uberjar


FROM openjdk:8-stretch
MAINTAINER lpthanh@gmail.com

ENV TELEGRAM_TOKEN=
ENV JDBC_DATABASE_URL=
ENV JAVA_OPTS=
ENV SENTRY_DSN=

WORKDIR /app

RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

COPY bin/heroku/ .
COPY --from=builder /app/target/uberjar/whosin-standalone.jar .

CMD ["./start.sh"]
