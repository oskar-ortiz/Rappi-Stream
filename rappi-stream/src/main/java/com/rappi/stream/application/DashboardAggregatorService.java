package com.rappi.stream.application;

import com.rappi.stream.domain.Pedido;
import com.rappi.stream.domain.EstadoPedido;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DashboardAggregatorService {

    private final OrderRepositoryPort orderRepository;
    private final AuditoriaEventoPort auditoriaEventoPort;

    public DashboardAggregatorService(OrderRepositoryPort orderRepository,
                                      AuditoriaEventoPort auditoriaEventoPort) {
        this.orderRepository = orderRepository;
        this.auditoriaEventoPort = auditoriaEventoPort;
    }

    /**
     * Aplica un evento de dominio al Pedido, persiste el nuevo estado
     * y registra la transición en la tabla de auditoría.
     *
     * Este método es invocado por el consumidor Kafka (fuera de esta capa).
     */
    public void aplicarEventoADominio(UUID pedidoId, String tipoEvento, Map<String, Object> datosEvento) {
        Optional<Pedido> optional = orderRepository.findById(pedidoId);
        if (optional.isEmpty()) {
            throw new IllegalStateException("Pedido no encontrado: " + pedidoId);
        }

        Pedido pedido = optional.get();
        EstadoPedido estadoAnterior = pedido.getEstado();

        // Lógica de transición de estado basada en el tipo de evento
        switch (tipoEvento) {
            case "DRIVER_ARRIVING":
                // Llamar a registrarLlegada() — transición ASIGNADO -> EN_CAMINO
                try {
                    pedido.registrarLlegada();
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(
                            "No se puede registrar llegada para pedido " + pedidoId + ": " + e.getMessage());
                }
                break;

            case "ORDER_DELIVERED":
                // Llamar a completarEntrega() — transición EN_CAMINO -> ENTREGADO
                try {
                    pedido.completarEntrega();
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(
                            "No se puede entregar pedido " + pedidoId + ": " + e.getMessage());
                }
                break;

            default:
                throw new IllegalArgumentException("Tipo de evento desconocido: " + tipoEvento);
        }

        // 1. Persistir el nuevo estado del agregado
        orderRepository.save(pedido);

        // 2. Registrar la transición en la auditoría
        auditoriaEventoPort.registrarEvento(
                pedidoId,
                tipoEvento,
                estadoAnterior,
                pedido.getEstado(),
                pedido.getIdRepartidorAsignado(),
                String.valueOf(datosEvento)
        );
    }

    /**
     * Obtiene el historial de eventos/estados de un pedido.
     */
    public AuditoriaEventoPort.AuditoriaEvento[] obtenerHistorial(UUID pedidoId) {
        return auditoriaEventoPort.historialPorPedido(pedidoId)
                .toArray(new AuditoriaEventoPort.AuditoriaEvento[0]);
    }

    // ===================== INTERFACES PENDIENTES (sin implementar en esta capa) =====================

    /**
     * Contrato que debe implementar el consumidor Kafka de dashboard_aggregator.
     * Recibirá los eventos del topic order_events y llamará a aplicarEventoADominio().
     */
    @FunctionalInterface
    public interface KafkaEventHandler {
        void handle(UUID pedidoId, String tipoEvento, Map<String, Object> datosEvento);
    }

    /**
     * Contrato que debe implementar la salida WebSocket del dashboard.
     * Será notificada tras cada transición de estado exitosa.
     */
    @FunctionalInterface
    public interface WebSocketPublisher {
        void publicarEstadoActualizado(UUID pedidoId, EstadoPedido estadoNuevo);
    }
}