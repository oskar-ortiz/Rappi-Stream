#!/bin/sh
set -eu

attempt=0
while ! rabbitmqadmin \
  --host rabbitmq \
  --username "$RABBITMQ_DEFAULT_USER" \
  --password "$RABBITMQ_DEFAULT_PASS" \
  --vhost "$RABBITMQ_DEFAULT_VHOST" \
  list vhosts >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 30 ]; then
    echo "RabbitMQ management API did not become available" >&2
    exit 1
  fi
  sleep 2
done

rabbitmqadmin \
  --host rabbitmq \
  --username "$RABBITMQ_DEFAULT_USER" \
  --password "$RABBITMQ_DEFAULT_PASS" \
  --vhost "$RABBITMQ_DEFAULT_VHOST" \
  import /etc/rabbitmq/definitions.json
