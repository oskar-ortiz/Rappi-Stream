package com.rappi.stream.domain;

import java.util.List;
import java.util.UUID;
import java.util.Random;

public class AleatoriaStrategy implements AsignacionRepartidorStrategy {

    private final Random random = new Random();

    @Override
    public UUID asignarRepartidor(UUID idPedido, List<UUID> repartidoresDisponibles) {
        if (repartidoresDisponibles == null || repartidoresDisponibles.isEmpty()) {
            throw new IllegalArgumentException("No hay repartidores disponibles para asignar");
        }
        int indice = random.nextInt(repartidoresDisponibles.size());
        return repartidoresDisponibles.get(indice);
    }
}