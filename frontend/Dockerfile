# syntax=docker/dockerfile:1

FROM nginx:1.27-alpine

# Only substitute variables we explicitly define - protects nginx's own
# "$variables" (like $host, $remote_addr) from being touched by envsubst.
ENV NGINX_ENVSUBST_FILTER='^GATEWAY_'
ENV GATEWAY_HOST=gateway
ENV GATEWAY_PORT=8080

# Remove the image's default server block so it doesn't conflict with ours
RUN rm -f /etc/nginx/conf.d/default.conf

# Template is rendered to /etc/nginx/conf.d/default.conf by the official
# nginx entrypoint (docker-entrypoint.d/20-envsubst-on-templates.sh) at
# container startup, using GATEWAY_HOST / GATEWAY_PORT from the environment.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

COPY src/ /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=10s \
    CMD wget -q -O- http://localhost:80/ || exit 1
