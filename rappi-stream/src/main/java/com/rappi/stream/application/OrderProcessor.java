package com.rappi.stream.application;

import com.rappi.stream.domain.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class OrderProcessor {

    private final OrderRepositoryPort orderRepository;
    private final AsignacionRepartidorStrategy asignacionStrategy;
    private final OrderEventPublisherPort orderEventPublisher;
    private final AuditoriaEventoPort auditoriaEventoPort;

    public OrderProcessor(
            OrderRepositoryPort orderRepository,
            AsignacionRepartidorStrategy asignacionStrategy,
            OrderEventPublisherPort orderEventPublisher,
            AuditoriaEventoPort auditoriaEventoPort) {
        this.orderRepository = orderRepository;
        this.asignacionStrategy = asignacionStrategy;
        this.orderEventPublisher = orderEventPublisher;
        this.auditoriaEventoPort = auditoriaEventoPort;
    }

    /**
     * Consume el comando OrderCreatedCommand de la cola new_orders_queue (RabbitMQ),
     * crea el Pedido, aplica la Strategy de asignación, persiste en PostgreSQL
     * y publica DRIVER_ASSIGNED en el topic order_events.
     */
    @RabbitListener(queues = "new_orders_queue")
    public void consumirNuevoPedido(OrderCreatedCommand command) {
        // 1. Generar ID del pedido
        UUID idPedido = UUID.randomUUID();

        // 2. Crear el Pedido con estado inicial CREADO
        Pedido pedido = new Pedido(idPedido, UUID.randomUUID(), command.getDireccionEntrega());

        // Registrar creación en auditoría
        auditoriaEventoPort.registrarEvento(
                idPedido, "ORDER_CREATED",
                null, EstadoPedido.CREADO, null, null);

        // 3. Obtener repartidores disponibles (en producción: repositorio de conductores)
        List<UUID> repartidoresDisponibles = obtenerRepartidoresDisponibles();

        // 4. Aplicar la Strategy de asignación (inyección polimórfica)
        UUID idRepartidor = asignacionStrategy.asignarRepartidor(idPedido, repartidoresDisponibles);

        // 5. Asignar repartidor (transición CREADO -> ASIGNADO con validación de invariantes)
        pedido.asignarRepartidor(idRepartidor);

        // 6. Persistir el estado inicial ANTES de emitir el evento (consistencia)
        Pedido pedidoGuardado = orderRepository.save(pedido);

        // 7. Registrar transición en auditoría
        auditoriaEventoPort.registrarEvento(
                idPedido, "DRIVER_ASSIGNED",
                EstadoPedido.CREADO, EstadoPedido.ASIGNADO,
                idRepartidor, null);

        // 8. Publicar DRIVER_ASSIGNED en el topic order_events con order_id como key
        DriverAssignedEvent evento = new DriverAssignedEvent(
                pedidoGuardado.getId().toString(),
                idRepartidor.toString()
        );
        orderEventPublisher.publicarDriverAssigned(evento);
    }

    // En producción vendría de un repositorio de conductores activos
    private List<UUID> obtenerRepartidoresDisponibles() {
        return List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003")
        );
    }
}