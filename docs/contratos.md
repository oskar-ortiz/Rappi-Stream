# Contratos de mensajería

> **Gobernanza:** cambios a este documento requieren acuerdo de las tres personas mediante pull request a `dev`. No se deben cambiar los formatos de mensaje unilateralmente.

Todos los mensajes son records inmutables, serializados como JSON con nombres `snake_case`. Los consumidores validan la forma del mensaje **fail-fast** y rechazan mensajes inválidos; la estrategia de reintentos y mensajes no procesables se acordará al implementar los servicios.

## Kafka

- Topic: `order_events`
- Clave de partición: `order_id` (string); conservarla para mantener el orden de eventos por pedido.
- Los eventos son un registro inmutable del ciclo de vida del pedido.

### Eventos

```json
{
  "event_type": "DRIVER_ASSIGNED",
  "order_id": "order-123",
  "driver_id": "driver-456",
  "occurred_at": "2026-01-15T12:30:00Z"
}
```

```json
{
  "event_type": "DRIVER_ARRIVING",
  "order_id": "order-123",
  "occurred_at": "2026-01-15T12:40:00Z"
}
```

```json
{
  "event_type": "ORDER_DELIVERED",
  "order_id": "order-123",
  "occurred_at": "2026-01-15T12:50:00Z"
}
```

### Estados de la vista del dashboard

El aggregator materializa los eventos con esta correspondencia:

| Evento Kafka | Estado visible |
|---|---|
| `DRIVER_ASSIGNED` | `EN_CAMINO` |
| `DRIVER_ARRIVING` | `LLEGANDO` |
| `ORDER_DELIVERED` | `ENTREGADO` |

El primer evento de cada pedido debe ser `DRIVER_ASSIGNED`; los siguientes deben respetar ese orden y no repetirse. Una transición inválida se registra y se ignora sin detener el consumidor.

> **Pendiente de alineación — Persona A:** confirmar y alinear estos estados con la entidad `Pedido` de `order-api`/`order-processor` antes de implementar persistencia o cambiar el ciclo de vida del dominio.

### Actualización WebSocket del dashboard

El aggregator envía cada snapshot consolidado a `dashboard-backend` por el WebSocket interno `/ws/ingest`. El backend difunde el mismo record JSON a `/ws/dashboard` y también lo devuelve en `GET /api/orders`.

```json
{
  "order_id": "order-123",
  "status": "EN_CAMINO",
  "driver_id": "driver-456",
  "timestamp": "2026-01-15T12:30:00Z"
}
```

Los valores de `status` son `EN_CAMINO`, `LLEGANDO` y `ENTREGADO`. El backend conserva el timestamp más reciente por pedido y entrega el snapshot existente a cada nuevo cliente WebSocket.

## RabbitMQ

| Tipo | Nombre | Detalle |
|---|---|---|
| Cola de comandos | `new_orders_queue` | Comando de creación de pedido |
| Exchange | `notifications` | Tipo `topic`, durable |
| Binding | `sms_queue` | Routing key `notify.client.*` |
| Binding | `push_queue` | Routing key `notify.driver.*` |

### Comando: crear pedido

Se publica en `new_orders_queue`.

```json
{
  "order_id": "order-123",
  "customer_id": "customer-456",
  "customer_phone": "+0000000000",
  "address": "Dirección de entrega",
  "items": [
    {
      "sku": "sku-001",
      "quantity": 2
    }
  ],
  "created_at": "2026-01-15T12:00:00Z"
}
```

### Payload de notificación

Se publica en el exchange `notifications` usando `notify.client.sms` o `notify.driver.push`.

```json
{
  "notification_id": "notification-123",
  "order_id": "order-123",
  "recipient_type": "client",
  "recipient_id": "customer-456",
  "channel": "sms",
  "message": "Tu pedido está en camino",
  "created_at": "2026-01-15T12:40:00Z"
}
```

Los nombres y valores de enumeración de los eventos son mayúsculos; los nombres de propiedades JSON son siempre `snake_case`.
