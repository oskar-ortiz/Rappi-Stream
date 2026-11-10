# Evidencias de la Fase 2 — Persona C

Verificaciones ejecutadas en `feature/infra`. Los servicios Java de las Personas A/B permanecen en el perfil `full`; no se modificó su código.

| Comando / prueba | Resultado |
|---|---|
| `mvn -B clean verify` | Correcto: reactor Maven completo, 19 pruebas, cero fallos/errores. |
| `docker compose config -q` y `docker compose --profile full config -q` | Correcto: configuración Compose válida para ambos perfiles. |
| `docker compose --profile full build` | Correcto: se construyeron las ocho imágenes Java. |
| `docker compose up -d --build` | Correcto tras ajustar el timeout del healthcheck de ZooKeeper a 15 s. Quedaron saludables ZooKeeper, Kafka, RabbitMQ, PostgreSQL, `dashboard-backend` y `dashboard-aggregator`. |
| `docker compose --profile full up -d` | Correcto: Compose creó los ocho servicios Java y las dos réplicas configuradas de cada worker. Los seis módulos A/B son esqueletos: algunos cierran su contexto Spring sin consumidores/listeners y entran en reinicio; se dejaron en `full` y sus contenedores se retiraron tras la prueba. |
| `docker image inspect rappi-stream-<servicio>:latest --format '{{.Config.User}}'` | Correcto para las ocho imágenes: `appuser`. Los contenedores activos del dashboard también se verificaron con `appuser`, límites de CPU/memoria aplicados y healthcheck healthy. |
| `docker exec rappi-stream-dashboard-backend-1 java -XshowSettings:vm -version` y equivalente para el aggregator | Correcto: JVM con heap máximo estimado de aproximadamente 122 MiB bajo límites de 384 MiB (backend) y 512 MiB (aggregator). |
| `docker compose up -d --build`; `GET /`, `GET /api/orders`, `GET /actuator/health` en `localhost:18080` | Correcto: los endpoints del dashboard respondieron HTTP 200. |
| `infra/scripts/send-dashboard-smoke-events.ps1` con cliente `infra/scripts/watch-dashboard.mjs` conectado a `ws://localhost:18080/ws/dashboard` | Correcto: tres pedidos recibieron en vivo la secuencia `EN_CAMINO` → `LLEGANDO` → `ENTREGADO`; `GET /api/orders` reflejó los tres en `ENTREGADO`. |
| `docker compose restart dashboard-aggregator` | Correcto: el aggregator volvió a healthy, el log mostró `Seeking to earliest offset` en `order_events` y REST volvió a mostrar los tres pedidos entregados. |
| Publicación de dos eventos `DRIVER_ASSIGNED` repetidos para `phase2-invalid` con clave Kafka `phase2-invalid` | Correcto: se registró `Ignoring invalid order transition ... cannot be repeated`; el aggregator y `/actuator/health` continuaron healthy. |
| `kafka-topics --bootstrap-server localhost:29092 --describe --topic order_events` | Correcto: topic existente con tres particiones. El script de humo publica cada evento con su `order_id` como clave y el aggregator materializa el resultado. |
| Publicación/consumo con `rabbitmqadmin` en `notifications` usando `notify.client.sms` y `notify.driver.push` | Correcto: los mensajes llegaron a `sms_queue` y `push_queue`; ambas colas quedaron en cero tras consumirlos. |
| `docker compose exec -T postgres-db psql -U rappi -d rappistream -Atc "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'rappi';"` | Correcto: devolvió `rappi`, tanto antes como después de `docker compose down` (sin `-v`) y `docker compose up -d --build`. |
| `docker compose --profile full up -d --no-deps sms-worker`; `docker compose --profile full rm -sf sms-worker` | Correcto: Compose creó dos réplicas con usuario `appuser`; el escalado se verificó y las réplicas se eliminaron después. El esqueleto puede cerrar su proceso por falta de listeners funcionales. |
| `git diff --check` | Correcto: sin errores de whitespace. Los scripts Bash verificados conservan finales de línea LF. |

## Decisiones y dependencias

- Los seis módulos aún sin comportamiento funcional se activan con `--profile full`; la infraestructura y los dos servicios del dashboard quedan en el perfil predeterminado. Para no publicar servicios internos, únicamente se exponen `dashboard-backend` (`18080`) y `order-api` (`8082`).
- El healthcheck de ZooKeeper invoca `cub zk-ready ... 10`; el timeout de Docker Compose se elevó de 5 a 15 segundos para que no mate una verificación que puede esperar hasta 10 segundos.
- Los servicios esqueleto no se dejaron ejecutándose: se observó salida normal de la JVM y reinicios con `restart: unless-stopped`. Cada persona responsable debe agregar el consumer/listener de larga duración y validar readiness antes de quitar el perfil.
- `OrderHistoryPort` tiene un adaptador provisional de log. La implementación JPA y la alineación de los estados con la entidad `Pedido` dependen de la Persona A.
- El PR a `dev` y su CI se documentan al finalizar la publicación.
