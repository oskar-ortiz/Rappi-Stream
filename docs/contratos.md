# Contratos de mensajería

> **Gobernanza:** cambios a este documento requieren acuerdo de las tres personas mediante pull request a `dev`. No se deben cambiar los formatos de mensaje unilateralmente.

## Índice

- [API REST: POST /order](#api-rest-post-order)
- [Kafka](#kafka)
- [RabbitMQ](#rabbitmq)

> Cualquier cambio de este contrato se acuerda entre las tres personas por PR.

Todos los mensajes son records inmutables, serializados como JSON con nombres `snake_case`. Los consumidores validan la forma del mensaje **fail-fast** y rechazan mensajes inválidos; la estrategia de reintentos y mensajes no procesables se acordará al implementar los servicios.

## API REST: POST /order

### Resumen

`order-api` recibe una solicitud para crear un pedido y, si es válida, publica un `OrderCreatedCommand` en la cola durable `new_orders_queue`. La API acepta `Content-Type: application/json` y todos los nombres de propiedades JSON son `snake_case`.

| Propiedad | Valor |
|---|---|
| URL base local | `http://localhost:8082` (`ORDER_API_PORT`, valor de `.env.example` y valor por defecto de Compose) |
| Endpoint | `POST http://localhost:8082/order` |
| Puerto del contenedor | `8080` (`SERVER_PORT`) |
| Perfil Compose | `full` |
| Tipo de contenido | `application/json` |

Si `ORDER_API_PORT` se cambia en `.env`, cambia también el puerto de la URL base local. Los clientes no proporcionan `order_id` ni `created_at`: `order-api` los genera en el servidor.

### Request

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---:|---|
| `customer_id` | string | Sí | No nulo ni en blanco; máximo 64 caracteres. |
| `customer_phone` | string | Sí | Formato E.164: `^\+[1-9]\d{7,14}$`. |
| `address` | string | Sí | No nula ni en blanco; máximo 200 caracteres. |
| `items` | array de objetos | Sí | Entre 1 y 50 elementos. |
| `items[].sku` | string | Sí | No nulo ni en blanco; máximo 64 caracteres. |
| `items[].quantity` | integer | Sí | Entero entre 1 y 99, inclusive. |

Ejemplo válido (sin los campos generados por el servidor):

```json
{
  "customer_id": "customer-456",
  "customer_phone": "+14155552671",
  "address": "123 Market Street, San Francisco",
  "items": [
    {
      "sku": "sku-001",
      "quantity": 2
    }
  ]
}
```

### Validación Fail-Fast

El cuerpo se deserializa a un `record` inmutable (incluyendo un record para cada artículo) cuyos constructores compactos validan nulidad, blancos, longitudes, rangos y formato E.164 antes de publicar nada. Si falla la deserialización o cualquier regla, la solicitud se rechaza y **nunca se publica un mensaje en RabbitMQ**.

### Respuestas

#### `202 Accepted`

Se devuelve únicamente después de que RabbitMQ confirme que el comando se publicó en `new_orders_queue`; indica recepción/publicación, no que `order-processor` haya procesado o persistido el pedido.

Cabecera:

```http
Location: /order/550e8400-e29b-41d4-a716-446655440000
```

El `Location` es informativo: **por ahora no existe un endpoint `GET /order/{order_id}`**.

Cuerpo:

```json
{
  "order_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RECEIVED",
  "received_at": "2026-01-15T12:00:00Z"
}
```

`order_id` es un UUID v4 generado por el servidor. `received_at` es la hora UTC en que la API aceptó la solicitud.

#### `400 Bad Request`

Se devuelve para validaciones fallidas o JSON malformado. Una validación fallida usa este formato estándar:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "items[0].quantity",
      "reason": "must be between 1 and 99"
    }
  ],
  "timestamp": "2026-01-15T12:00:00Z"
}
```

`timestamp` es ISO-8601 UTC. Un body JSON malformado también responde `400 Bad Request`; no publica el comando. Conserva el formato de error común y usa `error: "INVALID_JSON"` para distinguir un fallo de sintaxis de una validación de campos:

```json
{
  "error": "INVALID_JSON",
  "message": "Request body contains malformed JSON",
  "details": [
    {
      "field": "$",
      "reason": "malformed JSON"
    }
  ],
  "timestamp": "2026-01-15T12:00:00Z"
}
```

#### `415 Unsupported Media Type`

Se devuelve si el request no declara JSON (`Content-Type: application/json`). No se publica el comando.

#### `503 Service Unavailable`

Se devuelve si RabbitMQ no está disponible o no confirma la publicación. **No se debe devolver `202` si el broker reportó una falla de publicación.**

```json
{
  "error": "BROKER_UNAVAILABLE",
  "message": "The order could not be accepted because the message broker is unavailable",
  "details": [],
  "timestamp": "2026-01-15T12:00:00Z"
}
```

El formato común de error también se aplica a `503`. No incluir información interna del broker.

#### `500 Internal Server Error`

Error inesperado: responder con un mensaje genérico, sin stack trace, nombres de clases, credenciales ni detalles internos. El envelope conserva el formato común, por ejemplo `{"error":"INTERNAL_ERROR","message":"Internal server error","details":[],"timestamp":"2026-01-15T12:00:00Z"}`.

### Comando publicado en RabbitMQ: `OrderCreatedCommand`

Tras validar, `order-api` genera el UUID v4 `order_id` y el `created_at` UTC, y publica este comando en la cola durable `new_orders_queue`. Se usa el exchange por defecto de RabbitMQ (`""`) y la routing key igual al nombre de la cola (`new_orders_queue`).

Propiedades AMQP obligatorias: `content_type: application/json`, `delivery_mode: 2` (persistente) y `message_id` igual a `order_id`.

```json
{
  "order_id": "550e8400-e29b-41d4-a716-446655440000",
  "customer_id": "customer-456",
  "customer_phone": "+14155552671",
  "address": "123 Market Street, San Francisco",
  "items": [
    {
      "sku": "sku-001",
      "quantity": 2
    }
  ],
  "created_at": "2026-01-15T12:00:00Z"
}
```

`order-processor` consume el comando con acknowledgement manual, o equivalente que confirme el mensaje **solo después de persistir** el pedido. Un mensaje inválido se registra y se rechaza con `requeue=false` (no reencolar); su manejo no debe tumbar el consumidor.

### Idempotencia y reintentos

Por ahora no existe una clave de idempotencia HTTP: cada `POST /order` válido crea un pedido nuevo, y el servidor genera un nuevo `order_id`. Repetir el POST, incluso como reintento del cliente, puede crear otro pedido. Un timeout con resultado de publicación desconocido no habilita a reutilizar un identificador generado por el cliente, porque dicho campo no forma parte del request.

Los reintentos de entrega del broker conservan el mismo comando, `order_id` y `message_id`; el consumidor debe evitar confirmar un mensaje antes de persistir. La deduplicación persistente de entregas duplicadas se debe coordinar con la Persona A al implementar el dominio y repositorio.

### Salud

La API debe exponer `GET http://localhost:8082/actuator/health` (ajustar el puerto si cambia `ORDER_API_PORT`). Actualmente el `pom.xml` de `order-api` no incluye Actuator y Compose usa `kill -0 1` como healthcheck temporal del esqueleto; habrá que habilitar Actuator y alinear el healthcheck cuando Persona A implemente el endpoint.

### Ejemplos `curl`

Solicitud exitosa — PowerShell (usa `curl.exe` para evitar el alias de PowerShell):

```powershell
curl.exe -i -X POST "http://localhost:8082/order" `
  -H "Content-Type: application/json" `
  -d '{"customer_id":"customer-456","customer_phone":"+14155552671","address":"123 Market Street, San Francisco","items":[{"sku":"sku-001","quantity":2}]}'
```

Respuesta esperada (el UUID y la hora los genera el servidor):

```http
HTTP/1.1 202 Accepted
Location: /order/550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json
```

```json
{
  "order_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RECEIVED",
  "received_at": "2026-01-15T12:00:00Z"
}
```

Solicitud exitosa — Bash:

```bash
curl -i -X POST "http://localhost:8082/order" \
  -H "Content-Type: application/json" \
  -d '{"customer_id":"customer-456","customer_phone":"+14155552671","address":"123 Market Street, San Francisco","items":[{"sku":"sku-001","quantity":2}]}'
```

Solicitud inválida — PowerShell:

```powershell
curl.exe -i -X POST "http://localhost:8082/order" `
  -H "Content-Type: application/json" `
  -d '{"customer_id":"customer-456","customer_phone":"4155552671","address":"123 Market Street","items":[{"sku":"sku-001","quantity":0}]}'
```

Solicitud inválida — Bash:

```bash
curl -i -X POST "http://localhost:8082/order" \
  -H "Content-Type: application/json" \
  -d '{"customer_id":"customer-456","customer_phone":"4155552671","address":"123 Market Street","items":[{"sku":"sku-001","quantity":0}]}'
```

Salida esperada en ambos casos: `400 Bad Request`, sin mensaje en RabbitMQ. Los detalles señalan `customer_phone` por no cumplir E.164 y `items[0].quantity` por estar fuera del rango.

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "customer_phone",
      "reason": "must match ^\\+[1-9]\\d{7,14}$"
    },
    {
      "field": "items[0].quantity",
      "reason": "must be between 1 and 99"
    }
  ],
  "timestamp": "2026-01-15T12:00:00Z"
}
```

### Checklist de implementación — Persona A

- [ ] Request, artículo, comando y respuestas implementados con records inmutables y validación Fail-Fast.
- [ ] Puerto `NewOrderCommandPort` (o equivalente) y adaptador RabbitMQ inyectados por constructor (DIP); confirmación del broker antes de responder `202`.
- [ ] Controlador HTTP limitado a adaptación de entrada/salida, sin lógica de negocio.
- [ ] Manejador global de excepciones para validación, JSON malformado, media type, broker e inesperados con los formatos/códigos descritos.
- [ ] Pruebas JUnit 5 de dominio del record: request válido y campos inválidos/límites.
- [ ] Prueba del servicio con Mockito: `verify` de exactamente un comando publicado para el caso válido y `verify(..., never())` para request inválido.
- [ ] Configuración de RabbitMQ mediante variables ya existentes (`SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`); no introducir secretos en el repositorio.
- [ ] Habilitar `GET /actuator/health` y reemplazar el healthcheck temporal de proceso de `order-api`.

### Consumo por `order-processor`

Deserializar `OrderCreatedCommand` a un record, crear el `Pedido` en el estado inicial definido por el dominio, persistirlo y confirmar el mensaje solo después de persistir. Luego emitir `DRIVER_ASSIGNED` en Kafka `order_events`, usando `order_id` como clave, incluyendo el `driver_id` requerido y respetando el contrato de [eventos Kafka](#eventos).

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

El contrato de entrada está definido en [API REST: POST /order](#api-rest-post-order). Este payload es `OrderCreatedCommand`: el servidor genera `order_id` (UUID v4) y `created_at` (ISO-8601 UTC), mientras que `order-api` copia los datos validados del request.

```json
{
  "order_id": "550e8400-e29b-41d4-a716-446655440000",
  "customer_id": "customer-456",
  "customer_phone": "+14155552671",
  "address": "123 Market Street, San Francisco",
  "items": [
    {
      "sku": "sku-001",
      "quantity": 2
    }
  ],
  "created_at": "2026-01-15T12:00:00Z"
}
```

Publicar usando el exchange por defecto y routing key `new_orders_queue`, con `content_type=application/json`, `delivery_mode=2` y `message_id` igual a `order_id`.

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
