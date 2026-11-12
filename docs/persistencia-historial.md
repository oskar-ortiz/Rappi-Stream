# Persistencia de historial (adaptador JPA de `OrderHistoryPort`)

Implementado en `dashboard-aggregator`. Adapta el puerto `OrderHistoryPort` a PostgreSQL mediante Spring Data JPA.

## Modelo de datos

Entidad `OrderHistoryJpaEntity` → tabla `order_history`.

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| `id` | bigint (identity) | no | PK |
| `order_id` | varchar(64) | no | ID del pedido |
| `event_type` | varchar(32) | no | Nombre del enum `EventType` |
| `driver_id` | varchar(64) | sí | Solo para `DRIVER_ASSIGNED` |
| `occurred_at` | timestamp | no | Instante del evento |

Repositorio: `OrderHistoryJpaRepository extends JpaRepository<OrderHistoryJpaEntity, Long>`.

## Mapeo dominio ↔ JPA

- `JpaOrderHistoryAdapter.record(OrderEvent)` construye la entidad a partir del record de dominio (`orderId`, `eventType.name()`, `driverId`, `occurredAt`) y la persiste.
- No hay relaciones con otras entidades; es una tabla de auditoría de eventos.

## Uso desde dashboard-aggregator

`ApplyOrderEventUseCase.consume(OrderEvent)` invoca `orderHistory.record(event)` tras actualizar el estado en memoria y antes de publicar el snapshot por WebSocket. El adaptador JPA reemplaza al `LoggingOrderHistoryAdapter` anterior (desactivado).

## Variables de entorno relevantes

Heredadas de `dashboard-aggregator`: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`. `spring.jpa.hibernate.ddl-auto=update`.

Ver también: [contratos.md](contratos.md), [order-processor.md](order-processor.md).
