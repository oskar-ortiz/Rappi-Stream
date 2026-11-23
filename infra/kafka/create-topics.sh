#!/bin/sh
# Termina ante errores de comandos o variables sin definir.
set -eu

# Valores por defecto si las variables no existen o estan vacias.
: "${KAFKA_BOOTSTRAP_SERVERS:=kafka:29092}"
: "${KAFKA_TOPIC:=order_events}"
# Tres particiones permiten repartir consumo; no son tres brokers ni tres replicas.
: "${KAFKA_PARTITIONS:=3}"
# Factor 1: una sola copia por particion, sin redundancia entre brokers.
: "${KAFKA_REPLICATION_FACTOR:=1}"

# Crea si no existe; no modifica automaticamente un topic previamente creado.
kafka-topics \
  --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
  --create \
  --if-not-exists \
  --topic "$KAFKA_TOPIC" \
  --partitions "$KAFKA_PARTITIONS" \
  --replication-factor "$KAFKA_REPLICATION_FACTOR"

