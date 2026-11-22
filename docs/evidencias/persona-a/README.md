# Evidencias Persona A: order-api y order-processor

Base: `origin/dev` (`10e52e0`). Rama: `fix/order-api-bean-config`.

## Causa y correcciones

- `CreateOrderService` no era un componente ni tenía un bean declarado. El contexto de `order-api` fallaba al crear `OrderController`.
- `OrderProcessingService` tenía `@Component` en application y `SimpleDriverAssignmentStrategy` no tenía bean; el contexto de `order-processor` fallaba al resolver `DriverAssignmentStrategy`.
- El test de arranque anterior solo comprobaba que existía la clase principal. Se reemplazó por pruebas `@SpringBootTest` que cargan los contextos y sustituyen los puertos externos.
- El arranque del processor también tenía una inyección ambigua entre dos beans `Queue`; `RabbitConfig` ahora identifica `deadLetterQueue` explícitamente.
- El listener no podía consumir la cola inicializada por Compose porque la declaración del processor agregaba argumentos DLX que no existen en la definición compartida de `new_orders_queue`. La declaración ahora coincide con la cola ya creada.
- La conversión de elementos JPA accedía a una colección lazy después de terminar la transacción del repositorio. `save` y `findById` ahora mantienen una transacción durante la conversión; el flujo E2E confirma el evento posterior.
- Se habilitó el modo servlet y se añadió `spring-boot-starter-web` al processor para servir Actuator. Los detalles se habilitaron solo en `readiness`.

No se modificó `docker-compose.yml` ni sus healthchecks.

## Evidencias de comandos

`mvn` no está instalado en el host; los comandos Maven se ejecutaron con Maven 3.9.6 / Java 17 dentro de Docker, montando este repositorio y una caché Maven persistente.

| Evidencia | Resultado |
| --- | --- |
| [`context-before-fix.txt`](./context-before-fix.txt) | Antes del fix, cada prueba de contexto falla (exit 1): falta `CreateOrderService` en API y `DriverAssignmentStrategy` en processor. |
| [`order-api-maven-clean-package.log`](./order-api-maven-clean-package.log) | `mvn -B clean package`: 16 tests, 0 fallos/errores; `BUILD SUCCESS`. Incluye el test de contexto (1 test, 0 fallos). |
| [`order-processor-maven-clean-package.log`](./order-processor-maven-clean-package.log) | `mvn -B clean package`: 10 tests en la evidencia original, 11 tras añadir la prueba de redelivery; `BUILD SUCCESS`. Incluye el test de contexto (1 test, 0 fallos). |
| [`compose-up-build-final.log`](./compose-up-build-final.log) | `docker compose --profile full up -d --build order-api order-processor`: código 0; ambas imágenes y servicios iniciados. |
| [`docker-runtime-status.log`](./docker-runtime-status.log) | `docker compose ps`: ambos `Up (healthy)`; `RestartCount=0` en los dos. API `/actuator/health` responde HTTP 200 / `UP`; processor `/actuator/health` responde `UP` y readiness muestra `db`, `kafka` y `rabbitListener` en `UP`. |
| [`http-order-requests.log`](./http-order-requests.log) | POST válido: 202; inválido: 400; `Content-Type: text/plain`: 415. |
| [`postgres-order.log`](./postgres-order.log) | Consulta de `orders`: el pedido de la evidencia está persistido con `DRIVER_ASSIGNED` y un `driver_id`. |
| [`kafka-order-event.log`](./kafka-order-event.log) | `kafka-console-consumer --property print.key=true`: `DRIVER_ASSIGNED`; la key y `order_id` coinciden. |
| [`order-id.txt`](./order-id.txt) | ID usado para correlacionar HTTP, PostgreSQL y Kafka. |
| [`rabbitmq-stopped-test.log`](./rabbitmq-stopped-test.log) | RabbitMQ detenido: POST valido devuelve 503 en 12 s, sin 202 ni publicacion; tras reiniciar, readiness UP sin reiniciar contenedor y POST devuelve 202. |
| [`rabbitmq-invalid-message-test.log`](./rabbitmq-invalid-message-test.log) | Mensaje invalido rechazado, error registrado, consumer vivo, RestartCount=0, mensaje descartado. |
| [`processor-redelivery-check.log`](./processor-redelivery-check.log) | Busqueda de redeliveries/duplicados: no hay `redelivered=true` en logs, pero existe un `order_id` duplicado en `order_events`. |
| [`order-processor-verify.log`](./order-processor-verify.log) | `mvn -B clean verify` en order-processor: BUILD SUCCESS, 11 tests; incluye OrderProcessorApplicationTest (contexto). |
| [`order-api-maven-clean-package.log`](./order-api-maven-clean-package.log) | `mvn -B clean package` en order-api: BUILD SUCCESS, 16 tests; incluye OrderApiApplicationTest (contexto). |

### Comandos Maven reproducibles

Desde PowerShell en la raíz del repositorio:

```powershell
docker run --rm --mount "type=volume,source=rappi-stream-maven-cache,target=/root/.m2" --mount "type=bind,source=$($PWD.Path),target=/workspace" -w /workspace/order-api maven:3.9.6-eclipse-temurin-17-alpine mvn -B clean package
docker run --rm --mount "type=volume,source=rappi-stream-maven-cache,target=/root/.m2" --mount "type=bind,source=$($PWD.Path),target=/workspace" -w /workspace/order-processor maven:3.9.6-eclipse-temurin-17-alpine mvn -B clean package
```

### Salidas funcionales clave

```text
API: HTTP 200 {"status":"UP"}
Processor: HTTP 200 {"status":"UP"}
Readiness: {"status":"UP","components":{"db":{"status":"UP"},"kafka":{"status":"UP"},"rabbitListener":{"status":"UP"}}}
order-api / order-processor: RestartCount=0 / RestartCount=0
POST válido: HTTP 202
POST inválido: HTTP 400
POST text/plain: HTTP 415
PostgreSQL: <order_id>|DRIVER_ASSIGNED|<driver_id>
Kafka: <order_id>::{"event_type":"DRIVER_ASSIGNED","order_id":"<order_id>",...}
```

## Configuración RabbitMQ y transacciones JPA

### RabbitMQ

- `order-api/.../infrastructure/config/RabbitConfig.java`:
  - Declara `newOrdersQueue` como cola durable con `QueueBuilder.durable(queueName).build()` y **sin argumentos DLX** (`arguments: {}`).
  - Configura `RabbitTemplate` con `setMandatory(true)` y publisher confirms; `RabbitOrderCommandAdapter.publish(...)` espera `CorrelationData.getFuture()` hasta `app.broker.confirm-timeout-ms` (5000 ms). Si no hay ACK del broker, lanza `BrokerUnavailableException`, que `GlobalExceptionHandler` traduce a HTTP 503.
- `order-processor/.../infrastructure/config/RabbitConfig.java`:
  - `newOrdersQueue` también se declara durable **sin** `x-dead-letter-exchange` ni `x-dead-letter-routing-key`, para que coincida con la definición compartida de `infra/rabbitmq/definitions.json` (`new_orders_queue`, `arguments: {}`). Antes agregaba argumentos DLX y RabbitMQ rechazaba la declaración porque la cola ya existía con otros argumentos.
  - Se mantiene `deadLetterExchange`, `deadLetterQueue` y su binding como beans declarativos, pero **no están conectados** a `new_orders_queue` porque la cola compartida no tiene DLX configurado.
  - `SimpleRabbitListenerContainerFactory` usa ACK manual (`AcknowledgeMode.MANUAL`) y `setDefaultRequeueRejected(false)`: un mensaje inválido se rechaza con `basicReject(deliveryTag, false)` y se descarta; un fallo transitorio se reencola con `basicNack(deliveryTag, false, true)`.
  - El listener (`OrderCommandRabbitListener`) hace ACK (`basicAck`) únicamente después de que `OrderProcessingService.process(...)` termina, por lo que la publicación del evento Kafka ocurre antes del ACK.
- `infra/rabbitmq/definitions.json` no se modificó: `new_orders_queue` sigue con `arguments: {}`.

### Transacciones JPA

- `JpaOrderRepositoryAdapter`:
  - `save(...)` lleva `@Transactional` y `findById(...)` lleva `@Transactional(readOnly = true)`.
  - La conversión `OrderJpaEntity -> Order` (`toDomain`) se ejecuta dentro de la transacción, evitando `LazyInitializationException` al leer `entity.getItems()` después de cerrar la sesión.
  - `OrderProcessingService.process(...)` no abre transacción propia; delega en el repositorio y publica el evento Kafka fuera de la transacción JPA. Esto mantiene la publicación antes del ACK sin extender la transacción de base de datos durante la llamada a Kafka.

### DLX inactiva (coordinación con Oskar)

La DLX **no está activa** en este PR. La definición compartida de `new_orders_queue` en `infra/rabbitmq/definitions.json` tiene `arguments: {}`, por lo que no existe `x-dead-letter-exchange`. Activarla requiere cambiar esa definición compartida y migrar la cola existente de forma coordinada (Oskar). Mientras tanto, los mensajes inválidos se rechazan con `requeue=false` y se descartan.

## Borrador de descripción del PR hacia `dev`

### Causa raíz

El contexto de Spring no registraba `CreateOrderService`, y `order-processor` tampoco tenía un bean para `DriverAssignmentStrategy`. Los tests previos no cargaban Spring y no detectaban faltantes de wiring.

### Cambios

- Declarar el caso de uso de API, el servicio de procesamiento y la estrategia como beans en `infrastructure/config`, manteniendo application/domain sin dependencia de Spring.
- Sustituir los tests de existencia de clase por tests `@SpringBootTest` de contexto con puertos externos mockeados y sin conexiones a brokers o PostgreSQL.
- Corregir la ambigüedad de beans `Queue` cualificando `deadLetterQueue` en `deadLetterBinding`.
- Alinear la declaración AMQP de `new_orders_queue` con la definición compartida (`arguments: {}`), eliminando los argumentos DLX que RabbitMQ rechazaba.
- Mantener las conversiones JPA dentro de transacciones (`save` transaccional, `findById` de solo lectura) para evitar acceso lazy fuera de sesión.
- Activar el endpoint Actuator del processor en modo servlet (`web-application-type: servlet`) y exponer detalles solo en readiness.
- Para pedidos ya existentes (redelivery), `OrderProcessingService` igualmente publica el evento Kafka antes de que el listener haga ACK; se añadió prueba unitaria.

### Validación

- Los tests de contexto fallan sin el wiring: `CreateOrderService` y `DriverAssignmentStrategy` no tienen beans.
- Ambos `mvn clean package` terminan en `BUILD SUCCESS` (16 tests en API; 10 en processor).
- `docker compose --profile full up -d --build order-api order-processor` termina correctamente. Los dos contenedores quedan `Up (healthy)` y con `RestartCount=0`.
- Health API y processor: `UP`; readiness del processor confirma `rabbitListener`, `db` y `kafka` en `UP`.
- POST válido/inválido/tipo incorrecto: 202/400/415.
- El pedido queda en PostgreSQL como `DRIVER_ASSIGNED` y se emite el evento Kafka `DRIVER_ASSIGNED` con `key = order_id`.

### Riesgos / seguimiento

- El processor ahora sirve HTTP internamente para exponer Actuator; Compose no publica su puerto. Los detalles se exponen únicamente en el grupo `readiness`.
- **DLX no activa:** la definición compartida de `new_orders_queue` (`infra/rabbitmq/definitions.json`) tiene `arguments: {}`, por lo que no existe `x-dead-letter-exchange`. Activarla requiere coordinación con Oskar (cambiar la definición compartida y migrar la cola existente). En este PR los mensajes inválidos se rechazan con `requeue=false` y se descartan.
- Se detectó un `order_id` (`62faeae9-8260-419a-81c8-fbd9a4f9228b`) con dos eventos en `order_events` por redelivery previa. El servicio publica el evento incluso cuando el pedido ya existe, antes del ACK; la idempotencia/outbox completa sigue fuera de este cambio.
- CI: el workflow `.github/workflows/ci.yml` ejecuta `mvn -B clean verify` en la raíz, que incluye `OrderApiApplicationTest` y `OrderProcessorApplicationTest` (pruebas de contexto). Verificado con `mvn -B -pl order-api,order-processor -am clean verify` (BUILD SUCCESS, 16 + 11 tests).
