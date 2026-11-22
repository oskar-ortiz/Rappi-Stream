# Guía Completa — Rappi-Stream: Arquitectura, Desarrollo y Despliegue

## 1. ¿Qué es Rappi-Stream?

Rappi-Stream es una plataforma distribuida de seguimiento de pedidos en tiempo real. Simula el ciclo de vida de un pedido de delivery (creación → asignación de driver → en camino → llegando → entregado) usando microservicios, mensajería asíncrona (RabbitMQ), event streaming (Kafka) y un dashboard web en tiempo real (WebSocket).

**Stack tecnológico:**
- Java 17, Spring Boot 3.2.12, Maven (reactor multi-módulo)
- Kafka 3.6.2 (Confluent 7.5.5), RabbitMQ 3.13, PostgreSQL 16
- Docker Compose, AWS EC2

---

## 2. Arquitectura del sistema

### 2.1 Componentes y responsabilidades

| Componente | Puerto | Responsabilidad |
|---|---|---|
| `order-api` | 8082 (host) | Recibe `POST /order`, valida el request, publica `OrderCreatedCommand` en RabbitMQ (`new_orders_queue`), responde `202` |
| `order-processor` | 8080 (interno) | Consume comandos de RabbitMQ, persiste en PostgreSQL (`rappi.orders`), publica `DRIVER_ASSIGNED` en Kafka (`order_events`) |
| `driver-simulator` | 8080 (interno) | Consume `DRIVER_ASSIGNED` de Kafka, programa entrega: +5s → `DRIVER_ARRIVING`, +10s → `ORDER_DELIVERED` |
| `notification-router` | 8080 (interno) | Consume eventos Kafka, publica notificaciones en RabbitMQ (exchange `notifications`, routing `notify.client.*` / `notify.driver.*`) |
| `sms-worker` (×2) | 8080 (interno) | Consume `sms_queue` de RabbitMQ |
| `push-notification-worker` (×2) | 8080 (interno) | Consume `push_queue` de RabbitMQ |
| `dashboard-aggregator` | 8080 (interno) | Consume `order_events` de Kafka, reconstruye vista en memoria (`OrderStateStore`), valida transiciones, persiste en `rappi.order_history`, publica snapshots por WebSocket a `dashboard-backend` |
| `dashboard-backend` | 8080 (contenedor) / 18080 (host) | Mantiene último snapshot en memoria, sirve UI REST (`GET /api/orders`) y WebSocket (`/ws/dashboard`) a navegadores |

### 2.2 Infraestructura

| Servicio | Imagen | Puerto host | Rol |
|---|---|---|---|
| ZooKeeper | `confluentinc/cp-zookeeper:7.5.5` | — (interno) | Coordinación de Kafka |
| Kafka | `confluentinc/cp-kafka:7.5.5` | 9092 (interno) | Broker de eventos, topic `order_events` (3 particiones) |
| RabbitMQ | `rabbitmq:3.13-management` | 5672 (AMQP), 15672 (UI) | Broker de mensajería, colas `new_orders_queue`, `sms_queue`, `push_queue` |
| PostgreSQL | `postgres:16-alpine` | 5432 (interno) | Persistencia, esquema `rappi` |

### 2.3 Camino de datos completo

```
POST /order (JSON)
  → order-api (valida, genera order_id UUID)
    → RabbitMQ new_orders_queue (OrderCreatedCommand)
      → order-processor (consume, persiste CREATED → DRIVER_ASSIGNED, publica Kafka)
        → Kafka order_events (DRIVER_ASSIGNED, clave = order_id)
          → driver-simulator (programa: +5s DRIVER_ARRIVING, +10s ORDER_DELIVERED)
          → dashboard-aggregator (reconstruye vista, persiste order_history, WebSocket)
            → dashboard-backend (actualiza snapshot, WebSocket /ws/dashboard)
              → Navegador (actualización en tiempo real)
          → notification-router (publica en RabbitMQ notifications exchange)
            → sms-worker (sms_queue)
            → push-notification-worker (push_queue)
```

### 2.4 Redes Docker

- **rappi-stream-network** (bridge interno): todos los servicios se comunican aquí
- **rappi-stream-edge** (bridge): solo expone `dashboard-backend` (18080) y `order-api` (8082) al host

---

## 3. Rol: Persona C (Infraestructura e Integración)

### Responsabilidades

- **`dashboard-backend`**: UI/REST/WebSocket, sirve el dashboard en tiempo real
- **`dashboard-aggregator`**: integración Kafka/WebSocket, reconstrucción de vista en memoria, validación de transiciones
- **Docker/Compose**: orquestación de todos los servicios, healthchecks, redes, volúmenes
- **Repositorio**: mantenimiento, CI/CD, documentación, evidencias
- **Integración**: coordinación entre Personas A (dominio/persistencia) y B (mensajería)

### Trabajo realizado (commits)

| Commit | Descripción |
|---|---|
| `0ba9e47` | Idempotencia en `OrderProcessingService` (redelivery seguro) |
| `a3d434d` | Errores de validación 400 limpios (sin texto técnico de Jackson) |
| `ba3ff74` | Publicación sincrónica en driver-simulator (orden de eventos) |
| `ed2d6f0` | Healthchecks: Kafka sin JVM, RabbitMQ AMQP-aware, heap tuning |
| `3573db1` | Evidencias de cierre pre-AWS |
| `55787ec` | Dashboard premium (dark theme, animaciones, búsqueda, toasts) |
| `ec94d0b` | Colección Postman con URLs corregidas |
| `ddff334` | Environment Postman para AWS |
| `d979746` | Guía de demo AWS |

### Archivos clave modificados

| Archivo | Cambio |
|---|---|
| `docker-compose.yml` | Healthchecks HTTP reales, Kafka `cub kafka-ready`, RabbitMQ `check_port_listener`, heap tuning |
| `OrderProcessingService.java` | Consumo idempotente (CREATED → publish → DRIVER_ASSIGNED) |
| `GlobalExceptionHandler.java` | Sin filtración de Jackson; `InvalidOrderRequestException` con campo+motivo |
| `DriverSimulatorService.java` | `kafkaTemplate.send(...).get(10s)` sincrónico |
| `dashboard.html` | UI premium: Inter font, glassmorphism, animaciones, búsqueda, toasts, dark mode |

---

## 4. Docker Compose — cómo funciona

### Estructura del compose

```yaml
# Infraestructura (perfil default)
zookeeper, kafka, kafka-init, rabbitmq, rabbitmq-init, postgres-db,
dashboard-backend, dashboard-aggregator

# Servicios de negocio (perfil full)
order-api, order-processor, driver-simulator, notification-router,
sms-worker (×2), push-notification-worker (×2)
```

### Comandos esenciales

```bash
# Levantar todo (infra + negocio)
docker compose --profile full up -d --wait --wait-timeout 300

# Ver estado
docker compose --profile full ps

# Ver logs de un servicio
docker compose --profile full logs -f order-processor

# Parar (preserva volúmenes/datos)
docker compose --profile full down

# Parar y borrar todo (CUIDADO: borra datos)
docker compose --profile full down -v
```

### Volúmenes persistentes

| Volumen | Servicio | Qué persiste |
|---|---|---|
| `postgres-data` | PostgreSQL | Pedidos, esquema `rappi` |
| `kafka-data` | Kafka | Eventos, offsets |
| `rabbitmq-data` | RabbitMQ | Colas, mensajes |
| `zookeeper-data` | ZooKeeper | Metadata de Kafka |

### Healthchecks

| Servicio | Healthcheck | Timeout |
|---|---|---|
| ZooKeeper | `cub zk-ready localhost:2181 10` | 20s |
| Kafka | `cub kafka-ready 1 10 -b localhost:29092` | 15s |
| RabbitMQ | `rabbitmq-diagnostics -q check_port_listener 5672` | 10s |
| PostgreSQL | `pg_isready` | 5s |
| order-api | `wget /actuator/health` | 5s |
| order-processor | `wget /actuator/health/readiness` | 5s |
| dashboard-backend | `wget /actuator/health` | 5s |
| dashboard-aggregator | `wget /actuator/health` (8081) | 5s |

### Redes

- **rappi-stream-network** (bridge interno): todos los servicios
- **rappi-stream-edge** (bridge): expone dashboard (18080) y order-api (8082) al host

---

## 5. Despliegue en AWS (EC2 + Docker Compose)

### Recursos creados

| Recurso | ID | Detalle |
|---|---|---|
| EC2 Instance | `i-088b5e6e985ecb666` | `c7i-flex.large` (2 vCPU, 4 GiB), Amazon Linux 2023, Free Tier |
| Security Group | `sg-05c835a07b12e25e7` | Puertos: 22 (SSH), 80, 8080, 8082, 18080 |
| Key Pair | `rappi-stream-key` | Guardado en `$env:TEMP\opencode\rappi-stream-key.pem` |
| VPC | Default VPC (`vpc-0b7f3bbb69f8a8379`) | Subred pública en us-east-1a |
| EBS | 20 GiB gp3 | Disco de la instancia |

### URLs de acceso

| Servicio | URL |
|---|---|
| Dashboard (premium) | `http://34.237.124.62:18080/` |
| Order API | `http://34.237.124.62:8082/order` |
| Health (API) | `http://34.237.124.62:8082/actuator/health` |
| Health (Dashboard) | `http://34.237.124.62:18080/actuator/health` |
| API Orders | `http://34.237.124.62:18080/api/orders` |

### Comandos de gestión

```bash
# Ver estado
ssh -i rappi-stream-key.pem ec2-user@34.237.124.62 "cd /home/ec2-user/Rappi-Stream && docker compose --profile full ps"

# Ver logs
ssh -i rappi-stream-key.pem ec2-user@34.237.124.62 "cd /home/ec2-user/Rappi-Stream && docker compose --profile full logs -f order-processor"

# Parar instancia (ahorra costos, preserva datos)
aws ec2 stop-instances --instance-ids i-088b5e6e985ecb666 --region us-east-1

# Prender instancia (IP cambia)
aws ec2 start-instances --instance-ids i-088b5e6e985ecb666 --region us-east-1

# Terminar instancia (borra todo)
aws ec2 terminate-instances --instance-ids i-088b5e6e985ecb666 --region us-east-1
```

### Proceso de despliegue ejecutado

1. **Login AWS**: `aws login` (credenciales de corto plazo)
2. **Key pair**: creado `rappi-stream-key`, clave privada guardada localmente
3. **Security Group**: creado con puertos 22, 80, 8080, 8082, 18080
4. **EC2**: lanzada instancia `c7i-flex.large` con Amazon Linux 2023
5. **User data**: instaló Docker + Docker Compose v2 (buildx 0.19.3), clonó el repo (rama `feat/pre-aws-cierre-validaciones`)
6. **Deploy**: `docker compose --profile full up -d --build --wait` (14 contenedores healthy)
7. **Verificación**: POST válido → 202, dashboard → ENTREGADO, health → 200

---

## 6. Cómo funciona cada cosa

### POST /order (crear pedido)

```
Cliente → POST /order (JSON) → order-api
  → Valida request (Bean Validation + compact constructor)
  → Genera order_id (UUID) y created_at
  → Publica OrderCreatedCommand en RabbitMQ (new_orders_queue, persistente)
  → Responde 202 + Location: /order/{order_id}
```

### Procesamiento (order-processor)

```
RabbitMQ new_orders_queue → order-processor (listener manual ack)
  → findById(order_id)
    → No existe: save CREATED → assignDriver → publish DRIVER_ASSIGNED (Kafka) → save DRIVER_ASSIGNED
    → Existe como CREATED: re-publica (recuperación de caída)
    → Existe como DRIVER_ASSIGNED: skip (idempotente, ack sin re-publicar)
  → basicAck (mensaje consumido)
```

### Entrega (driver-simulator)

```
Kafka order_events (DRIVER_ASSIGNED) → driver-simulator
  → Programa: +5s → publish DRIVER_ARRIVING
  → Programa: +10s → publish ORDER_DELIVERED
  → Publicación sincrónica (send().get()) para garantizar orden
```

### Dashboard (dashboard-aggregator + dashboard-backend)

```
Kafka order_events → dashboard-aggregator
  → Valida transición (DRIVER_ASSIGNED → EN_CAMINO → LLEGANDO → ENTREGADO)
  → Persiste en rappi.order_history
  → Publica snapshot por WebSocket a dashboard-backend (/ws/ingest)
    → dashboard-backend actualiza OrderSnapshotStore
    → Difunde a navegadores por /ws/dashboard
    → Navegador actualiza tabla + contadores + toasts
```

### Notificaciones (notification-router)

```
Kafka order_events → notification-router
  → Publica en RabbitMQ exchange "notifications"
    → sms_queue (routing: notify.client.*)
    → push_queue (routing: notify.driver.*)
      → sms-worker (×2) consume sms_queue
      → push-notification-worker (×2) consume push_queue
```

---

## 7. Pruebas y validación

### Pruebas unitarias (57 total)

| Módulo | Pruebas | Detalle |
|---|---|---|
| order-api | 19 | CreateOrderService (2), CreateOrderRequest (4), OrderItem (3), OrderApiApplication (1), OrderControllerWeb (9) |
| order-processor | 14 | OrderProcessingService (4), OrderState (2), Order (3), Strategy (1), OrderCommandRabbitListener (3), OrderProcessorApplication (1) |
| driver-simulator | 3 | DriverSimulatorApplication (1), DriverSimulatorService (2) |
| notification-router | 3 | NotificationRouterApplication (1), NotificationRouterService (2) |
| sms-worker | 2 | SmsWorkerApplication (1), SmsWorkerService (1) |
| push-notification-worker | 2 | PushNotificationWorkerApplication (1), PushNotificationWorkerService (1) |
| dashboard-aggregator | 8 | ApplyOrderEventUseCase (2), OrderStateStore (1), DashboardAggregatorApplication (1), OrderEvent (1), OrderView (2), JpaOrderHistoryAdapter (1) |
| dashboard-backend | 6 | ApplyOrderStatusUpdateUseCase (2), DashboardBackendApplication (1), OrderStatusUpdate (1), OrderIngestWebSocketHandler (2) |

### Validación end-to-end (AWS)

| Prueba | Resultado |
|---|---|
| POST válido → 202 | ✅ Pedido persistido como DRIVER_ASSIGNED |
| Dashboard → ENTREGADO | ✅ Cadena completa en ~10s |
| POST teléfono inválido → 400 | ✅ `field: "customerPhone"`, sin Jackson |
| POST JSON malformado → 400 | ✅ `error: "INVALID_JSON"` |
| POST text/plain → 415 | ✅ |
| Clave Kafka (55 eventos) | ✅ 55/55 con clave = order_id |
| Redelivery duplicado | ✅ Skip re-publication, 0 filas nuevas |
| RabbitMQ caído → 503 | ✅ 0 filas persistidas |
| Recuperación RabbitMQ | ✅ ~42s, POST 202 + persistido |
| Persistencia (down sin -v + up) | ✅ Datos intactos |
| Estabilidad (5 min) | ✅ RestartCount=0, todos healthy |

---

## 8. Archivos de evidencia y documentación

| Archivo | Contenido |
|---|---|
| `docs/evidencias-fase2.md` | Evidencia completa de la fase 2 (histórica + revalidación + cierre pre-AWS) |
| `docs/guia-demo-aws.md` | Guía de demostración en AWS |
| `docs/arquitectura.md` | Arquitectura inicial del sistema |
| `docs/contratos.md` | Contratos de API y mensajes |
| `Rappi-Stream.postman_collection.json` | Colección Postman (raíz del repo) |
| `Rappi-Stream.postman_environment.json` | Environment Postman con URLs de AWS |

---

## 9. Equipo y roles

- **Persona A** (`feature/dominio`): `order-api`, `order-processor`, persistencia JPA
- **Persona B** (`feature/mensajeria`): `driver-simulator`, `notification-router`, `sms-worker`, `push-notification-worker`
- **Persona C** (`feature/infra`): `dashboard-backend`, integración Kafka/WebSocket, Docker/Compose, repositorio, CI/CD, documentación, despliegue AWS

---

## 10. Decisiones pendientes para el equipo

1. **DLQ**: `new_orders_queue` sin argumentos DLX. Mensajes inválidos se rechazan con `requeue=false` (se registra motivo, no payload). Activar DLQ requiere migración de cola durable.
2. **Ventana de recuperación RabbitMQ (~42s)**: aceptada para esta fase; revisar contra objetivo de disponibilidad antes de producción.
3. **Contraseñas**: cambiar `change-me-in-production` en `.env` antes de uso real.
4. **HTTPS/dominio**: actualmente HTTP. Para producción, configurar dominio + ACM.