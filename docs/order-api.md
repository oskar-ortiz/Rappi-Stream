# order-api

Servicio de entrada HTTP. Recibe solicitudes de pedido, valida fail-fast, genera `order_id` (UUID v4) y `created_at`, y publica `OrderCreatedCommand` en la cola durable `new_orders_queue` de RabbitMQ. Solo responde `202 Accepted` tras la confirmación del broker.

## Estructura de paquetes

```
com.rappistream.orderapi
├── domain          # records inmutables con validación fail-fast
├── ports           # NewOrderCommandPort
├── application     # CreateOrderService, BrokerUnavailableException
└── infrastructure
    ├── amqp        # RabbitOrderCommandAdapter
    ├── config      # RabbitConfig (cola durable)
    └── web         # OrderController, GlobalExceptionHandler
```

## Endpoint `POST /order`

- URL local: `http://localhost:8082/order` (puerto host `ORDER_API_PORT`, contenedor `SERVER_PORT=8080`).
- Content-Type obligatorio: `application/json`.
- El cliente no envía `order_id` ni `created_at`; el servidor los genera.

### Request

| Campo | Tipo | Reglas |
|---|---|---|
| `customer_id` | string | no nulo/blanco, máx. 64 |
| `customer_phone` | string | E.164 `^\+[1-9]\d{7,14}$` |
| `address` | string | no nulo/blanco, máx. 200 |
| `items` | array | 1 a 50 elementos |
| `items[].sku` | string | no nulo/blanco, máx. 64 |
| `items[].quantity` | integer | 1 a 99 |

### Validación fail-fast

El cuerpo se deserializa a `CreateOrderRequest` (record). Su constructor compacto valida nulos, blancos, longitudes, rangos y formato E.164 antes de publicar. Si falla, no se publica nada.

### Ejemplo curl

```bash
curl -i -X POST "http://localhost:8082/order" \
  -H "Content-Type: application/json" \
  -d '{"customer_id":"customer-456","customer_phone":"+14155552671","address":"123 Market Street, San Francisco","items":[{"sku":"sku-001","quantity":2}]}'
```

## Respuestas

| Código | Causa | Cuerpo (ejemplo) |
|---|---|---|
| 202 Accepted | Solicitud válida y publicada (tras confirm del broker) | `{"order_id":"...","status":"RECEIVED","received_at":"2026-..."}` + header `Location: /order/{order_id}` |
| 400 Bad Request | Validación fallida o JSON malformado | `{"error":"VALIDATION_ERROR","message":"Request validation failed","details":[{"field":"customer_phone","reason":"..."}],"timestamp":"..."}` o `INVALID_JSON` |
| 415 Unsupported Media Type | Content-Type no es `application/json` | envelope con `error` (ver implementación) |
| 503 Service Unavailable | RabbitMQ no disponible o sin confirmación | `{"error":"BROKER_UNAVAILABLE","message":"The order could not be accepted because the message broker is unavailable","details":[],"timestamp":"..."}` |
| 500 Internal Server Error | Error inesperado | `{"error":"INTERNAL_ERROR","message":"Internal server error","details":[],"timestamp":"..."}` |

## Publicación en `new_orders_queue`

- Exchange por defecto (`""`), routing key = nombre de cola.
- Cola durable, mensajes persistentes (`delivery_mode=2`).
- `content_type=application/json`, `message_id=order_id`.
- Publisher confirms correlacionados con timeout corto (`BROKER_CONFIRM_TIMEOUT_MS`, por defecto 5000 ms). Si no hay confirmación, se lanza `BrokerUnavailableException` → 503.

## `/actuator/health`

- Endpoint: `GET /actuator/health`.
- Exposición controlada por configuración (`management.endpoints.web.exposure.include=health`).
- Detalle controlado por `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` (por defecto `never`).

## Variables de entorno

| Variable | Descripción | Por defecto |
|---|---|---|
| `SPRING_RABBITMQ_HOST` | Host de RabbitMQ | `localhost` |
| `SPRING_RABBITMQ_PORT` | Puerto de RabbitMQ | `5672` |
| `SPRING_RABBITMQ_USERNAME` | Usuario | `rappi` |
| `SPRING_RABBITMQ_PASSWORD` | Contraseña | `change-me` |
| `SPRING_RABBITMQ_CONNECTION_TIMEOUT` | Timeout de conexión (ms) | `5000` |
| `SERVER_PORT` | Puerto del contenedor | `8080` |
| `NEW_ORDERS_QUEUE` | Nombre de la cola | `new_orders_queue` |
| `BROKER_CONFIRM_TIMEOUT_MS` | Timeout de confirmación del broker | `5000` |
| `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | Detalle del health | `never` |

## Puerto

- Interfaz: `NewOrderCommandPort.publish(OrderCreatedCommand)`.
- Adaptador: `RabbitOrderCommandAdapter` (AMQP). El controlador no conoce RabbitMQ.

Ver también: [contratos.md](contratos.md), [order-processor.md](order-processor.md).
