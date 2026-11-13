# Evidencias de la Fase 2 ÔÇö Persona C

Verificaciones ejecutadas en `feature/infra`. Los servicios Java de las Personas A/B permanecen en el perfil `full`; no se modific├│ su c├│digo.

| Comando / prueba | Resultado |
|---|---|
| `mvn -B clean verify` | Correcto: reactor Maven completo, 19 pruebas, cero fallos/errores. |
| `docker compose config -q` y `docker compose --profile full config -q` | Correcto: configuraci├│n Compose v├ílida para ambos perfiles. |
| `docker compose --profile full build` | Correcto: se construyeron las ocho im├ígenes Java. |
| `docker compose up -d --build` | Correcto tras ajustar el timeout del healthcheck de ZooKeeper a 15 s. Quedaron saludables ZooKeeper, Kafka, RabbitMQ, PostgreSQL, `dashboard-backend` y `dashboard-aggregator`. |
| `docker compose --profile full up -d` | Correcto: Compose cre├│ los ocho servicios Java y las dos r├®plicas configuradas de cada worker. Los seis m├│dulos A/B son esqueletos: algunos cierran su contexto Spring sin consumidores/listeners y entran en reinicio; se dejaron en `full` y sus contenedores se retiraron tras la prueba. |
| `docker image inspect rappi-stream-<servicio>:latest --format '{{.Config.User}}'` | Correcto para las ocho im├ígenes: `appuser`. Los contenedores activos del dashboard tambi├®n se verificaron con `appuser`, l├¡mites de CPU/memoria aplicados y healthcheck healthy. |
| `docker exec rappi-stream-dashboard-backend-1 java -XshowSettings:vm -version` y equivalente para el aggregator | Correcto: JVM con heap m├íximo estimado de aproximadamente 122 MiB bajo l├¡mites de 384 MiB (backend) y 512 MiB (aggregator). |
| `docker compose up -d --build`; `GET /`, `GET /api/orders`, `GET /actuator/health` en `localhost:18080` | Correcto: los endpoints del dashboard respondieron HTTP 200. |
| `infra/scripts/send-dashboard-smoke-events.ps1` con cliente `infra/scripts/watch-dashboard.mjs` conectado a `ws://localhost:18080/ws/dashboard` | Correcto: tres pedidos recibieron en vivo la secuencia `EN_CAMINO` ÔåÆ `LLEGANDO` ÔåÆ `ENTREGADO`; `GET /api/orders` reflej├│ los tres en `ENTREGADO`. |
| `docker compose restart dashboard-aggregator` | Correcto: el aggregator volvi├│ a healthy, el log mostr├│ `Seeking to earliest offset` en `order_events` y REST volvi├│ a mostrar los tres pedidos entregados. |
| Publicaci├│n de dos eventos `DRIVER_ASSIGNED` repetidos para `phase2-invalid` con clave Kafka `phase2-invalid` | Correcto: se registr├│ `Ignoring invalid order transition ... cannot be repeated`; el aggregator y `/actuator/health` continuaron healthy. |
| `kafka-topics --bootstrap-server localhost:29092 --describe --topic order_events` | Correcto: topic existente con tres particiones. El script de humo publica cada evento con su `order_id` como clave y el aggregator materializa el resultado. |
| Publicaci├│n/consumo con `rabbitmqadmin` en `notifications` usando `notify.client.sms` y `notify.driver.push` | Correcto: los mensajes llegaron a `sms_queue` y `push_queue`; ambas colas quedaron en cero tras consumirlos. |
| `docker compose exec -T postgres-db psql -U rappi -d rappistream -Atc "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'rappi';"` | Correcto: devolvi├│ `rappi`, tanto antes como despu├®s de `docker compose down` (sin `-v`) y `docker compose up -d --build`. |
| `docker compose --profile full up -d --no-deps sms-worker`; `docker compose --profile full rm -sf sms-worker` | Correcto: Compose cre├│ dos r├®plicas con usuario `appuser`; el escalado se verific├│ y las r├®plicas se eliminaron despu├®s. El esqueleto puede cerrar su proceso por falta de listeners funcionales. |
| `git diff --check` | Correcto: sin errores de whitespace. Los scripts Bash verificados conservan finales de l├¡nea LF. |

## Decisiones y dependencias

- Los seis m├│dulos a├║n sin comportamiento funcional se activan con `--profile full`; la infraestructura y los dos servicios del dashboard quedan en el perfil predeterminado. Para no publicar servicios internos, ├║nicamente se exponen `dashboard-backend` (`18080`) y `order-api` (`8082`).
- El healthcheck de ZooKeeper invoca `cub zk-ready ... 10`; el timeout de Docker Compose se elev├│ de 5 a 15 segundos para que no mate una verificaci├│n que puede esperar hasta 10 segundos.
- Los servicios esqueleto no se dejaron ejecut├índose: se observ├│ salida normal de la JVM y reinicios con `restart: unless-stopped`. Cada persona responsable debe agregar el consumer/listener de larga duraci├│n y validar readiness antes de quitar el perfil.
- `OrderHistoryPort` tiene un adaptador provisional de log. La implementaci├│n JPA y la alineaci├│n de los estados con la entidad `Pedido` dependen de la Persona A.
- El PR a `dev` y su CI se documentan al finalizar la publicaci├│n.

## Revalidación integrada pre-AWS

Revalidación sobre `dev` en `2b2493e4fd930d55a84bc8a72c66c250a0bbfa7d`. El CI de ese commit terminó en `success` ([Actions run 43318526451](https://github.com/oskar-ortiz/Rappi-Stream/actions/runs/43318526451)).

| Comando / prueba | Resultado |
|---|---|
| `mvn -B '-DargLine=-Xmx512m -XX:MaxMetaspaceSize=384m' clean verify` | `BUILD SUCCESS`: 51 pruebas, 0 fallos, 0 errores. Desglose: order-api 16, order-processor 11, driver-simulator 3, notification-router 3, sms-worker 2, push-notification-worker 2, dashboard-aggregator 8 y dashboard-backend 6. |
| `docker compose --profile full config -q` | Correcto; configuración completa válida. |
| Build de las ocho imágenes Java | Correcto al compilar los servicios secuencialmente. El primer build conjunto perdió la conexión con BuildKit (`failed to receive status ... EOF`); no se consideró exitoso hasta terminar los ocho builds individuales. |
| `docker compose --profile full up -d --wait --wait-timeout 300` | Todos los servicios del perfil llegaron a `healthy`, incluidas las dos réplicas de `sms-worker` y `push-notification-worker`. |
| `docker exec <contenedor-app> id -u` para los contenedores de aplicación | Correcto: los 10 contenedores (ocho imágenes y dos réplicas adicionales) ejecutan con UID `100` (`appuser`). Sin reinicios automáticos observados. |
| `java $JAVA_OPTS -XX:+PrintFlagsFinal -version` dentro de `order-api` y `order-processor` | Correcto: límite cgroup `402653184` bytes (384 MiB), `UseContainerSupport=true`, `MaxRAMPercentage=75.0` y `MaxHeapSize=301989888` bytes (288 MiB). |
| POST válido a `http://localhost:8082/order` | HTTP 202 y cabecera `Location`. El pedido `4955619b-0889-4e3b-b860-2f7c9a5aa30a` quedó en PostgreSQL como `DRIVER_ASSIGNED`; `GET http://localhost:18080/api/orders` lo reflejó como `ENTREGADO`. El publisher de order-processor usa `order_id` como clave Kafka. |
| POST con teléfono inválido, `Content-Type: text/plain` y JSON malformado | HTTP 400, 415 y 400, respectivamente. El error de validación conserva el formato estándar, pero en esta prueba reportó `field: "$"` y texto técnico de Jackson en `reason`; conviene mejorar el detalle antes de exponerlo a clientes. |
| RabbitMQ detenido durante un POST válido | HTTP 503 `BROKER_UNAVAILABLE`; el pedido de prueba tuvo 0 filas en PostgreSQL. |
| Recuperación tras iniciar RabbitMQ | La primera solicitud inmediata aún devolvió 503. Aproximadamente 20 segundos después `/actuator/health` de order-api respondió 200 y un nuevo POST devolvió 202; se persistió como `DRIVER_ASSIGNED`. No hubo reinicio del proceso de API. |
| Readiness de order-processor y colas RabbitMQ | Readiness `UP` para `db`, `kafka` y `rabbitListener`; `new_orders_queue` mostró un consumidor, y `sms_queue`/`push_queue` mostraron dos cada una. |
| `docker compose --profile full down` (sin `-v`) y luego `up -d --wait --wait-timeout 300` | Correcto: los volúmenes `rappi-stream-postgres-data`, `rappi-stream-kafka-data`, `rappi-stream-rabbitmq-data` y `rappi-stream-zookeeper-data` permanecieron. Tras levantar de nuevo, el esquema `rappi` y los dos pedidos de prueba seguían en PostgreSQL como `DRIVER_ASSIGNED`; `GET /api/orders` conservó el estado `ENTREGADO`; todos los servicios quedaron healthy. |
| `git diff --check` y `docker compose --profile full config -q` | Correctos en esta revalidación. |

### Observaciones pendientes antes de AWS

- Durante una inspección con `kafka-console-consumer`, Kafka pasó a `unhealthy` y registró expiraciones de sesión con ZooKeeper; el contenedor requirió un reinicio manual para recuperar `healthy`. Luego el perfil completo volvió a quedar saludable, pero hay que repetir una prueba de estabilidad sin depender de esa intervención y verificar la clave Kafka con una herramienta de bajo impacto.
- `OrderProcessingService` vuelve a publicar `DRIVER_ASSIGNED` cuando encuentra un `order_id` ya persistido. La prueba `publishesEventWhenOrderAlreadyExists` fija actualmente ese comportamiento y la evidencia previa registró un evento duplicado tras redelivery. Acordar y verificar deduplicación del evento (o aceptar explícitamente semántica at-least-once) antes de producción.
- La recuperación RabbitMQ no fue inmediata: una petición enviada justo al terminar el healthcheck del broker aún recibió 503; la API recuperó salud y aceptó pedidos después. Validar/reducir esa ventana de reconexión antes de considerar cerrado el escenario de recuperación.
- La definición compartida de `new_orders_queue` sigue sin argumentos DLX. Actualmente se registra el motivo del rechazo (log `Invalid order command rejected: ...`) y los mensajes inválidos se descartan con `requeue=false` (no se registra el payload completo). Activar una DLQ requiere coordinar la definición y la migración de la cola existente.
'@
Set-Content -Path "$env:TEMP\opencode\useraddition.md" -Value $useraddition -Encoding UTF8 -NoNewline
"USER ADDITION WRITTEN"; (Get-Content "$env:TEMP\opencode\useraddition.md").Count

## Cierre pre-AWS — validaciones y correcciones (2026-11-13)

Base: `dev` en `2b2493e4fd930d55a84bc8a72c66c250a0bbfa7d` (CI `success`, run 43318526451). Cambios locales sin commit sobre `docker-compose.yml` y este documento.

### Correcciones de código implementadas

- `OrderProcessingService`: consumo idempotente. Un `order_id` ya persistido como `DRIVER_ASSIGNED` no se vuelve a publicar (redelivery es no-op y se hace ack). Un `order_id` en estado `CREATED` (publicación no confirmada, p.ej. caída de Kafka) se re-procesa y publica. El primer `save` registra la intención (`CREATED`) antes de publicar; el segundo `save` solo ocurre tras la publicación (`DRIVER_ASSIGNED`).
- `order-api`: `InvalidOrderRequestException` lleva campo + motivo público; `GlobalExceptionHandler.handleNotReadable` deja de filtrar texto técnico de Jackson. Respuesta 400 estándar con `field`/`reason` limpios; los errores de tipo se reportan con la ruta del campo (`items[0].quantity`).
- `docker-compose.yml`: healthcheck de Kafka sustituido por `cub kafka-ready` (sin JVM dentro del contenedor, evita la presión de memoria que causaba expiraciones de sesión de ZooKeeper); `KAFKA_HEAP_OPTS` por defecto `384M`; healthcheck de RabbitMQ verifica el listener AMQP (`check_port_listener 5672`) con timeout 10 s; ZooKeeper usa `KAFKA_HEAP_OPTS` (256 MiB) porque la imagen ignora `ZOOKEEPER_HEAP_OPTS` y por defecto arrancaba con 512 MiB dentro de un límite de 384 MiB. Se conservan los healthchecks HTTP reales añadidos previamente para `order-api` y `order-processor`.
- `driver-simulator`: publicación sincrónica (`kafkaTemplate.send(...).get(10s)`) para garantizar el orden de eventos por `order_id` en el topic (evita la inversión `DRIVER_ARRIVING`/`ORDER_DELIVERED` observada con broker saturado).

### Pruebas focalizadas añadidas

- `OrderProcessingServiceTest`: `newOrderIsPersistedAsCreatedThenPublishedAndMarkedAssigned`, `newOrderPersistsCreatedStateBeforePublishingAndAssignedStateAfterwards`, `redeliveryAfterAssignmentDoesNotRepublish`, `redeliveryAfterUnconfirmedPublicationRepublishesAndMarksAssigned`.
- `OrderCommandRabbitListenerTest`: `successfulProcessingIsAcknowledgedSoDuplicateRedeliveriesAreNotRequeued`.
- `OrderControllerWebTest`: `invalidPhoneReturnsCleanPublicValidationError`, `malformedJsonReturnsCleanPublicInvalidJsonError`, `typeMismatchReturnsCleanFieldPath`.

### Resultados de validación

| Comando / prueba | Resultado |
|---|---|
| `mvn -B '-DargLine=-Xmx512m -XX:MaxMetaspaceSize=384m' clean verify` | `BUILD SUCCESS`: 57 pruebas (order-api 19, order-processor 14, driver-simulator 3, notification-router 3, sms-worker 2, push-notification-worker 2, dashboard-aggregator 8, dashboard-backend 6), 0 fallos. |
| `docker compose --profile full config -q` | Correcto. |
| Build secuencial de `order-api`, `order-processor` y `driver-simulator` | Correcto (evita el fallo previo de BuildKit por builds conjuntos). |
| `docker compose --profile full up -d --wait --wait-timeout 300` | Todos los servicios `healthy`, incluidas las dos réplicas de `sms-worker` y `push-notification-worker`. |
| `docker exec <app> id -u` (10 contenedores) | UID `100` (`appuser`) en los 10. |
| `java $JAVA_OPTS -XX:+PrintFlagsFinal -version` en `order-api` y `order-processor` | `UseContainerSupport=true`, `MaxRAMPercentage=75.0`, `MaxHeapSize=301989888` bytes (288 MiB) bajo el límite cgroup de 384 MiB. |
| Kafka: `KAFKA_HEAP_OPTS` y memoria | `-Xms384M -Xmx384M`; uso observado ~303 MiB / 768 MiB; `RestartCount=0`. |
| POST válido a `http://localhost:8082/order` | HTTP 202 + `Location`. Pedido `d5045f1e-...` en PostgreSQL como `DRIVER_ASSIGNED`; dashboard como `ENTREGADO` (cadena completa con el fix de ordenación sincrónica del simulator). |
| POST con teléfono inválido | HTTP 400, cuerpo público limpio: `field: "customerPhone"`, `reason: "must match ^\\+[1-9]\\d{7,14}$"` (sin detalles de Jackson). |
| POST con JSON malformado | HTTP 400, `error: "INVALID_JSON"`, `field: "$"`, motivo genérico. |
| POST con `Content-Type: text/plain` | HTTP 415. |
| Verificación de clave Kafka (consumidor de bajo impacto en contenedor aparte, `--from-beginning`, `print.key=true`) | 55 eventos, 55/55 con clave igual a `order_id`, 0 desajustes. `order_events` recibe `DRIVER_ASSIGNED` con `order_id` como clave. |
| Redelivery: publicación duplicada a `new_orders_queue` con `order_id` existente | El procesador registró `Duplicate delivery for order ...: already DRIVER_ASSIGNED, skipping re-publication`; 0 filas nuevas en `rappi.order_history` (sigue en 3); fila en `rappi.orders` sin cambios; mensaje consumido y ackeado. |
| RabbitMQ detenido durante un POST válido | HTTP 503 `BROKER_UNAVAILABLE`; 0 filas persistidas para ese pedido. |
| Recuperación tras iniciar RabbitMQ | Salud 503 durante ~37 s; `/actuator/health` = 200 a los ~42 s; nuevo POST = 202 y pedido persistido como `DRIVER_ASSIGNED`; readiness de `order-processor` = UP (`db`, `kafka`, `rabbitListener`). |
| `docker compose --profile full down` (sin `-v`) + `up -d --wait` | Volúmenes intactos; `rappi.orders` conservó las 2 filas (`DRIVER_ASSIGNED` con sus `driver_id`); dashboard conservó los 15 pedidos como `ENTREGADO`; todos los servicios healthy. |
| `git diff --check` | Sin errores de whitespace. |

### Estabilidad sostenida de Kafka (prueba de bajo impacto)

Se ejecutaron dos monitores sin intervención manual:
- Monitor 1 (20 iteraciones, 30 s): durante los primeros 4 min todos los contenedores clave estuvieron `healthy` y Kafka mantuvo `RestartCount=0` con memoria 289-376 MiB y CPU 1.96-7.30 %. A partir de la iteración 5 se inició un ciclo `down`/`up` de validación, lo que dejó a RabbitMQ `unhealthy` (healthcheck combinado `ping && check_port_listener` tardaba ~12.6 s con timeout de 5 s) y a ZooKeeper `unhealthy` (heap de 512 MiB dentro de un límite de 384 MiB). Ambos defectos se corrigieron (healthcheck único de RabbitMQ con timeout 10 s; `KAFKA_HEAP_OPTS` en ZooKeeper).
- Monitor 2 (10 iteraciones, 30 s, tras las correcciones): Kafka `RestartCount=0`, memoria 340-426 MiB / 768 MiB (pico 425.6), CPU 0.56-94.36 %, todos los contenedores clave `healthy` en las 10 iteraciones.

### Decisiones y limitaciones que requieren acuerdo del equipo

- **DLQ**: `new_orders_queue` sigue declarada sin argumentos DLX (`x-dead-letter-exchange`). Existen `new_orders_dlx` y `new_orders_queue_dlq`, pero la cola principal no los enruta; actualmente se registra el motivo del rechazo (log `Invalid order command rejected: ...`) y los mensajes inválidos se descartan con `requeue=false` (no se registra el payload completo). Activar la DLQ exige migrar la cola existente (RabbitMQ rechaza redeclarar una cola durable con argumentos distintos) y actualizar `infra/rabbitmq/definitions.json`. Decisión pendiente de coordinación entre personas.
- **Semántica de entrega**: se adoptó deduplicación por existencia en `OrderProcessingService` (at-least-once en RabbitMQ, at-most-once en la publicación del evento una vez persistido como `DRIVER_ASSIGNED`). Ventana residual documentada: si el proceso cae entre el primer `save` (CREATED) y la publicación, la redelivery re-publica el evento (semántica at-least-once en Kafka, tolerada por el aggregator que ignora transiciones inválidas). No se implementó outbox completo; queda como trabajo futuro si se requiere exactly-once.
- **Ventana de recuperación de RabbitMQ**: ~42 s desde el arranque del broker hasta `/actuator/health = 200` (≈35 s de arranque del broker + ≈7 s de reconexión del cliente Spring). Aceptada para esta fase como limitación documentada (no se considera automáticamente aceptable para producción/AWS sin revisar el objetivo de disponibilidad); el cliente reintenta automáticamente, no hay pérdida de datos ni reinicio de la API. Se mejoró la precisión del healthcheck del broker para que `healthy` implique listener AMQP listo. Si el objetivo de disponibilidad exige menos tiempo, ajustar en una fase posterior (arranque del broker o backoff del cliente).
- **Orden de eventos del simulator**: se corrigió la publicación asincrónica por sincrónica (`kafkaTemplate.send(...).get()`) para evitar inversión de `DRIVER_ARRIVING`/`ORDER_DELIVERED` observada durante arranques con broker saturado. El aggregator mantiene una máquina de estados estricta y rechaza eventos fuera de orden; la publicación sincrónica cierra la ventana principal. Residual: si en el futuro se publican eventos para un mismo `order_id` desde productores distintos, el orden no está garantizado; habría que añadir reordenamiento o un buffer en el aggregator.
- **Healthcheck de Kafka**: se reemplazó el comando basado en JVM (`kafka-topics --list`) por `cub kafka-ready` (Python, sin JVM) para eliminar la presión de memoria que provocaba expiraciones de sesión de ZooKeeper. `KAFKA_HEAP_OPTS` reducido a 384 MiB para dejar margen.

### Estado de Git

- Rama: `dev`; HEAD en `2b2493e4fd930d55a84bc8a72c66c250a0bbfa7d`.
- Cambios locales sin commit: `docker-compose.yml`, `docs/evidencias-fase2.md` y los cambios de código anteriores (order-api, order-processor, driver-simulator). Archivos sin seguimiento (`replay_pid*.log`, `hs_err_pid*.log`, `.atl/`) no se incluyeron.
- CI del commit base: `success` (run 43318526451). Los cambios nuevos requieren push (pendiente de autorización) para re-ejecutar CI.
