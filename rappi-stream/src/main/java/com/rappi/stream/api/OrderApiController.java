package com.rappi.stream.api;

import com.rappi.stream.application.OrderService;
import com.rappi.stream.application.OrderCommandPublisherPort;
import com.rappi.stream.domain.OrderCreateRequest;
import com.rappi.stream.domain.OrderCreatedCommand;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/order")
public class OrderApiController {

    private final OrderService orderService;
    private final OrderCommandPublisherPort orderCommandPublisher;

    public OrderApiController(OrderService orderService, OrderCommandPublisherPort orderCommandPublisher) {
        this.orderService = orderService;
        this.orderCommandPublisher = orderCommandPublisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseOrderCreated crearOrder(@RequestBody OrderCreateRequest request) {
        // 1. Validar y convertir a record domain (fail-fast ya ocurre en el record constructor)
        OrderCreatedCommand command = new OrderCreatedCommand(request.getIdCliente(), request.getDireccionEntrega());

        // 2. Generar ID de pedido si no existe (o recibirlo del cliente)
        UUID idPedido = UUID.randomUUID();

        // 3. Publicar el comando en la cola new_orders_queue a través del puerto
        orderCommandPublisher.publicarNuevoPedido(command, idPedido);

        // 4. Devolver respuesta inmediata (pattern async command)
        return new ResponseOrderCreated(idPedido.toString(), command.getIdCliente(), command.getDireccionEntrega(), "PENDING");
    }

    // DTO de respuesta simple
    @lombok.Data
    @lombok.AllArgsConstructor
    static class ResponseOrderCreated {
        private String orderId;
        private String idCliente;
        private String direccionEntrega;
        private String status;
    }
}