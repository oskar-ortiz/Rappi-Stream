package com.rappi.stream.domain;

import java.util.List;
import java.util.UUID;

public interface AsignacionRepartidorStrategy {
    UUID asignarRepartidor(UUID idPedido, List<UUID> repartidoresDisponibles);
}