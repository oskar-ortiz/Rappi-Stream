#!/bin/sh
set -eu

: "${KAFKA_BOOTSTRAP_SERVERS:=kafka:29092}"
: "${KAFKA_TOPIC:=order_events}"
: "${KAFKA_PARTITIONS:=3}"
: "${KAFKA_REPLICATION_FACTOR:=1}"

kafka-topics \
  --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
  --create \
  --if-not-exists \
  --topic "$KAFKA_TOPIC" \
  --partitions "$KAFKA_PARTITIONS" \
  --replication-factor "$KAFKA_REPLICATION_FACTOR"
