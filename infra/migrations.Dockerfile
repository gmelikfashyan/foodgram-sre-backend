# Образ одноразового процесса миграций БД.
# Контекст сборки — каталог с SQL-файлами: backend/src/main/resources/db/migration
FROM flyway/flyway:11.7.2

ARG APP_VERSION=dev
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="foodgram-migrations" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_SHA}"

COPY . /flyway/sql

# Параметры подключения передаются через FLYWAY_URL / FLYWAY_USER / FLYWAY_PASSWORD
CMD ["migrate"]
