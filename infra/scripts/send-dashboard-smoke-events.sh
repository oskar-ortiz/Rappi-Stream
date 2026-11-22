#!/usr/bin/env bash
set -euo pipefail

container="${KAFKA_CONTAINER:-rappi-stream-kafka-1}"
topic="${KAFKA_TOPIC:-order_events}"
prefix="${SMOKE_PREFIX:-smoke-order}"
base_epoch="$(date -u +%s)"

for index in 1 2 3; do
  order_id="${prefix}-${index}"
  driver_id="smoke-driver-${index}"
  for step in 0 1 2; do
    case "$step" in
      0) event_type="DRIVER_ASSIGNED" ;;
      1) event_type="DRIVER_ARRIVING" ;;
      2) event_type="ORDER_DELIVERED" ;;
    esac
    timestamp="$(date -u -d "@$((base_epoch + step + (index * 3)))" +'%Y-%m-%dT%H:%M:%SZ')"
    if [[ "$event_type" == "DRIVER_ASSIGNED" ]]; then
      payload="{\"event_type\":\"${event_type}\",\"order_id\":\"${order_id}\",\"driver_id\":\"${driver_id}\",\"occurred_at\":\"${timestamp}\"}"
    else
      payload="{\"event_type\":\"${event_type}\",\"order_id\":\"${order_id}\",\"occurred_at\":\"${timestamp}\"}"
    fi
    printf '%s:%s\n' "$order_id" "$payload"
  done
done | docker exec -i "$container" kafka-console-producer \
  --bootstrap-server localhost:29092 \
  --topic "$topic" \
  --property parse.key=true \
  --property key.separator=:

echo "Published DRIVER_ASSIGNED -> DRIVER_ARRIVING -> ORDER_DELIVERED for 3 smoke orders."
