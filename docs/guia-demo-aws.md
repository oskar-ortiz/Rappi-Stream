# Guía de Demostración — Rappi-Stream en AWS

## 1. ¿Qué se desplegó?

Se desplegó el sistema distribuido completo de Rappi-Stream en una instancia EC2 de AWS, corriendo con Docker Compose (perfil `full`), idéntico al entorno local pero accesible públicamente.

### Recursos AWS creados

| Recurso | ID / Nombre | Detalle |
|---|---|---|
| **EC2 Instance** | `i-088b5e6e985ecb666` | `c7i-flex.large` (2 vCPU, 4 GiB RAM), Amazon Linux 2023, Free Tier eligible |
| **Security Group** | `sg-05c835a07b12e25e7` | Puertos abiertos: 22 (SSH), 80, 8080, 8082, 18080 |
| **Key Pair** | `rappi-stream-key` | Par de claves SSH para acceso a la instancia |
| **VPC** | Default VPC (`vpc-0b7f3bbb69f8a8379`) | Subred pública en us-east-1a |
| **EBS** | 20 GiB gp3 | Disco de la instancia |

### Costo estimado
- **~$2-17/mes** (Free Tier + t3/c7i-flex.large)
- Mucho menos que el plan Fargate+ALB+MSK+RDS (~$300/mes)

## 2. Arquitectura desplegada

```
Internet
   │
   ▼
EC2 Instance (34.237.124.62)
   │
   ├── Docker Compose (perfil full)
   │   ├── ZooKeeper (healthcheck: cub zk-ready)
   │   ├── Kafka (healthcheck: cub kafka-ready, sin JVM)
   │   ├── RabbitMQ (healthcheck: check_port_listener 5672)
   │   ├── PostgreSQL (esquema rappi)
   │   ├── order-api (puerto 8082) ← recibe POST /order
   │   ├── order-processor (consume RabbitMQ → PG → Kafka)
   │   ├── driver-simulator (consume Kafka → publica eventos de entrega)
   │   ├── notification-router (consume Kafka → RabbitMQ notifications)
   │   ├── sms-worker ×2 (consume sms_queue)
   │   ├── push-notification-worker ×2 (consume push_queue)
   │   ├── dashboard-aggregator (consume Kafka → PostgreSQL + WebSocket)
   │   └── dashboard-backend (puerto 18080, WebSocket + REST)
   │
   └── Red interna Docker (bridge)
```

### Flujo de un pedido

1. **POST /order** → `order-api` valida el request, publica en `new_orders_queue` (RabbitMQ)
2. **order-processor** consume de RabbitMQ, persiste en PostgreSQL (`rappi.orders`), publica `DRIVER_ASSIGNED` en Kafka (`order_events`)
3. **driver-simulator** consume `DRIVER_ASSIGNED`, programa entrega (5s → `DRIVER_ARRIVING`, 10s → `ORDER_DELIVERED`)
4. **dashboard-aggregator** consume todos los eventos, materializa estado en `rappi.order_history`, publica por WebSocket
5. **dashboard-backend** recibe por WebSocket y actualiza el dashboard en tiempo real
6. **notification-router** consume eventos Kafka, publica notificaciones en RabbitMQ (`sms_queue`, `push_queue`)
7. **sms-worker** y **push-notification-worker** consumen y procesan notificaciones

### Estados del pedido en el dashboard

| Estado | Color | Cuándo |
|---|---|---|
| EN CAMINO | Azul | Pedido creado, driver asignado |
| LLEGANDO | Amarillo | Driver se acerca (~5s) |
| ENTREGADO | Verde | Pedido entregado (~10s) |

## 3. Cómo levantar la instancia (si está parada)

```bash
# 1. Prender la instancia
aws ec2 start-instances --instance-ids i-088b5e6e985ecb666 --region us-east-1

# 2. Esperar ~2 minutos y obtener la nueva IP
aws ec2 describe-instances --region us-east-1 --instance-ids i-088b5e6e985ecb666 --query "Reservations[0].Instances[0].PublicIpAddress" --output text

# 3. Verificar que todo está healthy (esperar ~3 min después de prender)
ssh -i rappi-stream-key.pem ec2-user@<NUEVA_IP> "cd /home/ec2-user/Rappi-Stream && docker compose --profile full ps"
```

Los contenedores se reinician automáticamente (`restart: unless-stopped` + Docker habilitado en boot).

## 4. URLs de acceso

| Servicio | URL |
|---|---|
| **Dashboard** (tiempo real) | `http://<IP>:18080/` |
| **Order API** | `http://<IP>:8082/order` |
| **Health (API)** | `http://<IP>:8082/actuator/health` |
| **Health (Dashboard)** | `http://<IP>:18080/actuator/health` |
| **API Orders** | `http://<IP>:18080/api/orders` |

## 5. Demostración en vivo

### Preparación (antes de la presentación)

1. Verificar que la instancia está corriendo:
   ```bash
   aws ec2 describe-instances --region us-east-1 --instance-ids i-088b5e6e985ecb666 --query "Reservations[0].Instances[0].{State:State.Name,IP:PublicIpAddress}" --output table
   ```
2. Abrir el dashboard en el navegador: `http://<IP>:18080/`
3. Abrir Postman con la colección `Rappi-Stream.postman_collection.json` y el environment `Rappi-Stream.postman_environment.json`
4. Actualizar `base_url` y `dashboard_url` en el environment con la IP actual

### Demo paso a paso

1. **Mostrar el dashboard vacío** — "Esperando pedidos…"
2. **Health checks** — ejecutar "Health - order-api" y "Health - dashboard-backend" en Postman → ambos 200
3. **Crear pedido** — ejecutar "Crear pedido valido" en Postman → 202 con `order_id`
4. **Observar el dashboard** — el pedido aparece en ~2s como EN CAMINO (azul)
5. **Esperar 5s** → cambia a LLEGANDO (amarillo)
6. **Esperar 10s** → cambia a ENTREGADO (verde)
7. **Mostrar notificaciones toast** (esquina superior derecha)
8. **Probar validación** — ejecutar "Telefono invalido" → 400 con error limpio (sin detalles técnicos de Jackson)
9. **Mostrar contadores** — En camino / Llegando / Entregados se actualizan en tiempo real

### Puntos clave para explicar

- **Idempotencia**: si un mensaje se entrega dos veces, el sistema no duplica el evento
- **Validación limpia**: los errores 400 no exponen detalles internos de Jackson
- **Kafka con clave**: cada evento usa `order_id` como clave (verificable con consumidor)
- **Redelivery**: si RabbitMQ cae, el POST devuelve 503 y no persiste; al recuperarse, acepta pedidos
- **Persistencia**: los datos sobreviven a `down` sin `-v` + `up`
- **Healthchecks**: Kafka usa `cub kafka-ready` (sin JVM, evita presión de memoria); RabbitMQ verifica listener AMQP

## 6. Para parar la instancia (ahorrar costos)

```bash
aws ec2 stop-instances --instance-ids i-088b5e6e985ecb666 --region us-east-1
```

Los datos (volúmenes Docker) se preservan. Al prender de nuevo, todo arranca automáticamente.

## 7. Para terminar (liberar todo)

```bash
aws ec2 terminate-instances --instance-ids i-088b5e6e985ecb666 --region us-east-1
```

Esto elimina la instancia y su disco. Los datos se pierden (pero están en el repo y en el PR #7/#8/#9).

## 8. Notas para el equipo

- El código está en `dev` (PRs #7, #8, #9 mergeados)
- La colección Postman está en la raíz: `Rappi-Stream.postman_collection.json` + `Rappi-Stream.postman_environment.json`
- La evidencia completa está en `docs/evidencias-fase2.md`
- Decisiones pendientes: DLQ (activar o no), ventana de recuperación RabbitMQ (~42s), contraseñas en `.env` (cambiar antes de producción)