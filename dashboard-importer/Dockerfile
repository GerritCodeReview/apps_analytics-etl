FROM taskrabbit/elasticsearch-dump:v3.3.1

RUN apk add --no-cache curl

COPY docker-entrypoint.sh wait-for-elasticsearch /
COPY ./kibana-config /kibana-config/
COPY ./elasticsearch-mappings /elasticsearch-mappings/

ENTRYPOINT /docker-entrypoint.sh
