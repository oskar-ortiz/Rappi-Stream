package com.rappi.stream.domain;

import java.util.List;
import java.util.UUID;
import java.util.Map;

public class MenorCargaStrategy implements AsignacionRepartidorStrategy {

    private final Map<UUID, Integer> cargaRepartidores; // idRepartidor -> numero de pedidos en curso

    public MenorCargaStrategy(Map<UUID, Integer> cargaRepartidores) {
        this.cargaRepartidores = cargaRepartidores;
    }

    @Override
    public UUID asignarRepartidor(UUID idPedido, List<UUID> repartidoresDisponibles) {
        if (repartidoresDisponibles == null || repartidoresDisponibles.isEmpty()) {
            throw new IllegalArgumentException("No hay repartidores disponibles para asignar");
        }
        UUID repartidorSeleccionado = null;
        int cargaMinima = Integer.MAX_VALUE;
        for (UUID repartidor : repartidoresDisponibles) {
            int carga = cargaRepartidores.getOrDefault(repartidor, 0);
            if (carga < cargaMinima) {
                cargaMinima = carga;
                repartidorSeleccionado = repartidor;
            }
        }
        return repartidorSeleccionado;
    }
}