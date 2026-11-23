package com.rappistream.orderprocessor.application;

import com.rappistream.orderprocessor.domain.DriverAssignmentStrategy;
import com.rappistream.orderprocessor.domain.Order;
import com.rappistream.orderprocessor.domain.OrderItem;
import com.rappistream.orderprocessor.domain.OrderState;
import com.rappistream.orderprocessor.messaging.OrderCreatedCommand;
import com.rappistream.orderprocessor.messaging.OrderEvent;
import com.rappistream.orderprocessor.ports.OrderEventPublisherPort;
import com.rappistream.orderprocessor.ports.OrderRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

public class OrderProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessingService.class);

    // Interfaces: el negocio no depende directamente de las implementaciones JPA y Kafka.
    private final OrderRepositoryPort orderRepository;
    private final OrderEventPublisherPort eventPublisher;
    private final DriverAssignmentStrategy driverAssignmentStrategy;

    public OrderProcessingService(OrderRepositoryPort orderRepository,
                                 OrderEventPublisherPort eventPublisher,
                                 DriverAssignmentStrategy driverAssignmentStrategy) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.driverAssignmentStrategy = Objects.requireNonNull(driverAssignmentStrategy, "driverAssignmentStrategy");
    }

    /**
     * Procesa comandos contemplando entregas repetidas de RabbitMQ.
     * Si el pedido no existe, guarda CREATED, publica y guarda DRIVER_ASSIGNED.
     * Si sigue en CREATED, reintenta; si ya esta en DRIVER_ASSIGNED, evita republicar.
     *
     * No garantiza exactamente una vez: si Kafka recibe el evento pero falla el
     * guardado final, otro intento puede publicarlo nuevamente. Tampoco hay aqui
     * un bloqueo para coordinar dos ejecuciones simultaneas del mismo pedido.
     */
    public void process(OrderCreatedCommand command) {
        // Consulta el estado persistido para reconocer pedidos ya procesados.
        Order existing = orderRepository.findById(command.orderId()).orElse(null);
        if (existing != null && existing.state() == OrderState.DRIVER_ASSIGNED) {
            LOGGER.warn("Duplicate delivery for order {}: already DRIVER_ASSIGNED, skipping re-publication",
                    command.orderId());
            return;
        }
        if (existing != null) {
            LOGGER.info("Republishing DRIVER_ASSIGNED for order {} whose previous publication was not confirmed",
                    command.orderId());
        }
        // Recupera el pedido pendiente o guarda su estado inicial CREATED.
        Order order = existing != null
                ? existing
                : orderRepository.save(Order.create(
                        command.orderId(),
                        command.customerId(),
                        command.customerPhone(),
                        command.address(),
                        command.items().stream()
                                .map(i -> new OrderItem(i.sku(), i.quantity()))
                                .toList(),
                        command.createdAt()));
        // Asigna conductor y modifica el estado del objeto en memoria mediante una estrategia.
        driverAssignmentStrategy.assignDriver(order);
        // El comando produce un hecho: DRIVER_ASSIGNED, consumible por otros servicios.
        OrderEvent event = new OrderEvent(
                "DRIVER_ASSIGNED",
                order.orderId(),
                order.driverId(),
                Instant.now());
        // Publica primero y luego persiste el estado final. Una excepcion llega al listener.
        // Esta secuencia no es una transaccion atomica entre Kafka y la base de datos.
        eventPublisher.publish(event);
        orderRepository.save(order);
    }
}
