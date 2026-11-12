# Handoff — Persona A

## Para Oskar (infra/Compose)

### Health indicators
- `order-api`: `/actuator/health` (exposición controlada por config).
- `order-processor`: indicadores `rabbitListener`, `db`, `kafka`; grupo de readiness `readiness` incluye los tres. Un proceso vivo sin BD/Kafka/Rabbit reporta DOWN.

### Variables de entorno nuevas/relevantes
- `NEW_ORDERS_QUEUE`, `BROKER_CONFIRM_TIMEOUT_MS`, `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS`.
- `order-processor`: `KAFKA_TOPIC` (default `order_events`), `SPRING_DATASOURCE_*`.

### RabbitMQ: cola y DLX
- Cola durable `new_orders_queue` con DLX `new_orders_dlx` y DLQ `new_orders_queue_dlq`.
- Si `rabbitmq-init` crea la cola sin esos argumentos, la declaración en la app puede fallar por mismatch (PRECONDITION_FAILED). Coordinar nombres/args.

### DDL
- `spring.jpa.hibernate.ddl-auto=update` en `order-processor` y `dashboard-aggregator`. Para producción, cambiar a `validate` y crear tablas en `infra/postgres/init.sql` (tablas `orders`, `order_items`, `order_history`).

## Para Nicolas (mensajería)

### Contrato exacto de `DRIVER_ASSIGNED`
```json
{
  "event_type": "DRIVER_ASSIGNED",
  "order_id": "order-123",
  "driver_id": "driver-456",
  "occurred_at": "2026-01-15T12:30:00Z"
}
```
- Topic: `order_events`.
- Key de partición: `order_id` (string).
- Serialización JSON, sin type headers (`spring.json.add.type.headers=false`).
- Producer: `acks=all`, idempotencia habilitada.

## Checklist de verificación manual
1. `mvn clean package` en `order-api`, `order-processor`, `dashboard-aggregator` (verde).
2. POST válido → 202 + `Location`, mensaje en `new_orders_queue`.
3. POST inválido → 400; Content-Type incorrecto → 415.
4. RabbitMQ caído → 503 sin publicar.
5. Mensaje inválido en cola → rechazado, consumer sigue vivo.
6. Mensaje válido → fila en PostgreSQL + evento `DRIVER_ASSIGNED` con key `order_id`.
7. `/actuator/health` en order-api y order-processor.

Ver también: [order-api.md](order-api.md), [order-processor.md](order-processor.md), [persistencia-historial.md](persistencia-historial.md), [decisiones-persona-a.md](decisiones-persona-a.md).
