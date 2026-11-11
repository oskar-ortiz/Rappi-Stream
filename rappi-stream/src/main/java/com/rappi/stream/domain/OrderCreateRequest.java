package com.rappi.stream.domain;

import lombok.Value;

@Value
public class OrderCreateRequest {
    private String idCliente;
    private String direccionEntrega;

    public OrderCreateRequest {
        validarIdCliente();
        validarDireccionEntrega();
    }

    private void validarIdCliente() {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo o estar en blanco");
        }
    }

    private void validarDireccionEntrega() {
        if (direccionEntrega == null || direccionEntrega.trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección de entrega no puede ser nula o estar en blanco");
        }
    }
}