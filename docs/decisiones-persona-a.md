# Decisiones de diseño — Persona A (ADR cortos)

## ADR-1: Generar `order_id` en `order-api`
- **Contexto:** el contrato no permite que el cliente envíe `order_id`.
- **Decisión:** generar UUID v4 en `order-api` y conservarlo en comando, evento y persistencia.
- **Consecuencias:** cada POST crea un pedido nuevo; reintentos del cliente pueden duplicar pedidos (aceptado por contrato).

## ADR-2: Ack manual en `order-processor`
- **Contexto:** se requiere persistir antes de confirmar.
- **Decisión:** `AcknowledgeMode.MANUAL`; ack solo tras persistir y publicar en Kafka.
- **Consecuencias:** mayor control de entrega; requiere manejo explícito de reject/nack.

## ADR-3: DLX para mensajes inválidos
- **Contexto:** evitar pérdida y bucles de reentrega.
- **Decisión:** cola con `x-dead-letter-exchange=new_orders_dlx` y DLQ `new_orders_queue_dlq`; inválidos → `basicReject(requeue=false)`.
- **Consecuencias:** mensajes defectuosos quedan auditables; requiere que RabbitMQ declare la DLX/DLQ (o que `rabbitmq-init` lo haga).

## ADR-4: Idempotencia por `findById(order_id)`
- **Contexto:** reentregas del broker.
- **Decisión:** buscar por `order_id` antes de persistir; si existe, no duplicar.
- **Consecuencias:** re-publicación del evento puede duplicarlo, pero el dashboard ignora transiciones inválidas.

## ADR-5: Puertos e inyección por constructor (DIP)
- **Contexto:** desacoplar dominio de infraestructura.
- **Decisión:** interfaces `NewOrderCommandPort`, `OrderRepositoryPort`, `OrderEventPublisherPort`, `OrderHistoryPort`; adaptadores inyectados por constructor.
- **Consecuencias:** pruebas unitarias con mocks; cambios de broker/BD no afectan dominio.

## ADR-6: Fail-fast en records
- **Contexto:** validar antes de publicar.
- **Decisión:** constructores compactos validan nulos, blancos, rangos y E.164.
- **Consecuencias:** errores claros; la deserialización fallida se traduce a 400.

Ver también: [contratos.md](contratos.md).
