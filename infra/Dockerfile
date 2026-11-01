FROM nginx:1.25.4-alpine

ARG APP_VERSION=dev
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="foodgram-gateway" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_SHA}"

COPY infra/nginx.conf /etc/nginx/conf.d/default.conf
