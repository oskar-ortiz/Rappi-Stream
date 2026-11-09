# Arquitectura inicial

El repositorio organiza la futura plataforma como ocho aplicaciones Spring Boot independientes dentro de un único reactor Maven. Esta entrega deja clases de arranque y configuración, pero no procesa pedidos ni publica mensajes.

## Componentes y responsabilidades

| Componente | Responsabilidad prevista |
|---|---|
| `order-api` | Recibir solicitudes de pedido y emitir el comando por RabbitMQ |
| `order-processor` | Procesar comandos, producir eventos Kafka y persistir auditoría |
| `driver-simulator` | Simular eventos de asignación, llegada y entrega |
| `notification-router` | Consumir eventos y publicar notificaciones en RabbitMQ |
| `sms-worker` | Consumir notificaciones SMS |
| `push-notification-worker` | Consumir notificaciones push |
| `dashboard-aggregator` | Consumir eventos, persistir datos de dashboard y coordinar actualizaciones WebSocket |
| `dashboard-backend` | Exponer el backend del dashboard y su canal WebSocket |

## Infraestructura y camino de datos

RabbitMQ transporta el comando de creación de pedido por `new_orders_queue` y los mensajes de notificación por el exchange topic `notifications`. Kafka mantiene el registro de eventos `order_events`, particionado por `order_id`. PostgreSQL aloja el esquema `rappi`, que se crea al iniciar la base; las tablas se incorporarán con las entidades JPA en una fase posterior.

En local, Docker Compose levanta únicamente ZooKeeper, Kafka, RabbitMQ y PostgreSQL. `kafka-init` crea el topic al estar Kafka saludable. Los brokers y la base usan volúmenes con nombre y una red bridge interna explícita.

## Responsabilidades del equipo

- **Persona A — `feature/dominio`:** `order-api`, `order-processor` y persistencia JPA de `dashboard-aggregator`.
- **Persona B — `feature/mensajeria`:** `driver-simulator`, `notification-router`, `sms-worker` y `push-notification-worker`.
- **Persona C — `feature/infra`:** `dashboard-backend`, integración Kafka/WebSocket de `dashboard-aggregator`, Docker/Compose y mantenimiento del repositorio.

`dashboard-aggregator` tiene propiedad compartida: acuerden cambios entre A (JPA) y C (Kafka/WebSocket) antes de integrarlos.

## Puntos de sincronización

1. El formato de eventos, comando y payload de notificación se acuerda entre las tres personas antes de cambiar [contratos.md](contratos.md).
2. Los contratos se versionan por PR hacia `dev`; `docs/flujo-de-trabajo.md` define la política de revisión y archivos compartidos.
3. Los cambios de esquema de persistencia deben mantener compatibilidad con los eventos y el dashboard.
4. Infraestructura y aplicaciones se integrarán gradualmente; la fase actual no agrega servicios de aplicación a Compose.
