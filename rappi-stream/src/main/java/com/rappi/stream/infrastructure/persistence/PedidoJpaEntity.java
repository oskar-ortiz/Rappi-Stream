package com.rappi.stream.infrastructure.persistence;

import com.rappi.stream.domain.EstadoPedido;
import com.rappi.stream.domain.Pedido;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
public class PedidoJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPedido estado;

    @Column(name = "id_cliente", nullable = false)
    private UUID idCliente;

    @Column(name = "direccion_entrega", nullable = false)
    private String direccionEntrega;

    @Column(name = "id_repartidor_asignado")
    private UUID idRepartidorAsignado;

    // Constructores
    public PedidoJpaEntity() {}

    public PedidoJpaEntity(UUID id, UUID idCliente, String direccionEntrega) {
        this.id = id;
        this.idCliente = idCliente;
        this.direccionEntrega = direccionEntrega;
        this.estado = EstadoPedido.CREADO;
        this.idRepartidorAsignado = null;
    }

    // Getters y Setters (Lombok no usado en entidad JPA por compatibilidad)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public UUID getIdCliente() { return idCliente; }
    public void setIdCliente(UUID idCliente) { this.idCliente = idCliente; }

    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }

    public UUID getIdRepartidorAsignado() { return idRepartidorAsignado; }
    public void setIdRepartidorAsignado(UUID idRepartidorAsignado) { this.idRepartidorAsignado = idRepartidorAsignado; }
}