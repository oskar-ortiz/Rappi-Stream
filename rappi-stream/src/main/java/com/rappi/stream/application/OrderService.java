package com.rappi.stream.application;

import com.rappi.stream.domain.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.*;

@Service
public class OrderService {

    private final OrderRepositoryPort orderRepository;
    private final AsignacionRepartidorStrategy asignacionStrategy;
    private final OrderEventPublisherPort orderEventPublisher;

    public OrderService(
            OrderRepositoryPort orderRepository,
            AsignacionRepartidorStrategy asignacionStrategy,
            OrderEventPublisherPort orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.asignacionStrategy = asignacionStrategy;
        this.orderEventPublisher = orderEventPublisher;
    }

    public Pedido procesarNuevoPedido(OrderCreateRequest request, UUID idPedido) {
        // 1. Crear el Pedido con estado inicial
        Pedido pedido = new Pedido(idPedido, UUID.fromString(request.getIdCliente()), request.getDireccionEntrega());

        // 2. Aplicar estrategia de asignación de repartidor
        // Simulación: lista de repartidores disponibles (en un caso real vendría del dominio de conductores)
        List<UUID> repartidoresDisponibles = obtenerRepartidoresDisponibles();
        UUID idRepartidor = asignacionStrategy.asignarRepartidor(idPedido, repartidoresDisponibles);

        // 3. Asignar repartidor al pedido (cambia estado a ASIGNADO)
        pedido.asignarRepartidor(idRepartidor);

        // 4. Persistir en la base de datos (a través del puerto)
        Pedido pedidoGuardado = orderRepository.save(pedido);

        // 5. Publicar evento DRIVER_ASSIGNED en el topic order_events
        DriverAssignedEvent evento = new DriverAssignedEvent(
                pedidoGuardado.getId().toString(),
                idRepartidor.toString()
        );
        orderEventPublisher.publicarDriverAssigned(evento);

        return pedidoGuardado;
    }

    // En un caso real esto vendría de un catálogo de conductores/BD
    private List<UUID> obtenerRepartidoresDisponibles() {
        // Retorna una lista fija para demostración; en producción vendría de un repositorio
        return List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003")
        );
    }
}