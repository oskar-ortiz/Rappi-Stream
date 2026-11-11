package com.rappi.stream.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEventoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Column(name = "tipo_evento", nullable = false)
    private String tipoEvento;

    @Column(name = "estado_anterior")
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false)
    private String estadoNuevo;

    @Column(name = "repartidor_id")
    private UUID repartidorId;

    @Column(name = "fecha_evento", nullable = false, updatable = false)
    private Instant fechaEvento;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    // Constructor JPA
    public AuditoriaEventoJpaEntity() {}

    // Constructor de dominio
    public AuditoriaEventoJpaEntity(UUID pedidoId, String tipoEvento,
                                    String estadoAnterior, String estadoNuevo,
                                    UUID repartidorId, String payloadJson) {
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.tipoEvento = tipoEvento;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.repartidorId = repartidorId;
        this.fechaEvento = Instant.now();
        this.payloadJson = payloadJson;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public String getTipoEvento() { return tipoEvento; }
    public String getEstadoAnterior() { return estadoAnterior; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public UUID getRepartidorId() { return repartidorId; }
    public Instant getFechaEvento() { return fechaEvento; }
    public String getPayloadJson() { return payloadJson; }
}