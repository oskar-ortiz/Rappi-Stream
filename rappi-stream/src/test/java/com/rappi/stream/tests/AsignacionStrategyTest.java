package com.rappi.stream.tests;

import com.rappi.stream.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AsignacionStrategyTest {

    // ===================== AleatoriaStrategy =====================

    @Test
    @DisplayName("AleatoriaStrategy retorna un repartidor de la lista")
    void aleatoriaStrategyRetornaRepartidorDeLaLista() {
        // Arrange
        AleatoriaStrategy strategy = new AleatoriaStrategy();
        UUID idPedido = UUID.randomUUID();
        List<UUID> repartidores = List.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        // Act
        UUID resultado = strategy.asignarRepartidor(idPedido, repartidores);

        // Assert
        assertNotNull(resultado);
        assertTrue(repartidores.contains(resultado),
                "El repartidor asignado debe estar en la lista de disponibles");
    }

    @Test
    @DisplayName("AleatoriaStrategy lanza excepción con lista vacía")
    void aleatoriaStrategyListaVaciaLanzaExcepcion() {
        // Arrange
        AleatoriaStrategy strategy = new AleatoriaStrategy();
        UUID idPedido = UUID.randomUUID();
        List<UUID> listaVacia = Collections.emptyList();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> strategy.asignarRepartidor(idPedido, listaVacia));
    }

    @Test
    @DisplayName("AleatoriaStrategy lanza excepción con lista nula")
    void aleatoriaStrategyListaNulaLanzaExcepcion() {
        // Arrange
        AleatoriaStrategy strategy = new AleatoriaStrategy();
        UUID idPedido = UUID.randomUUID();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> strategy.asignarRepartidor(idPedido, null));
    }

    @Test
    @DisplayName("AleatoriaStrategy con un solo repartidor siempre asigna ese")
    void aleatoriaStrategyUnSoloRepartidor() {
        // Arrange
        AleatoriaStrategy strategy = new AleatoriaStrategy();
        UUID idPedido = UUID.randomUUID();
        UUID unicoRepartidor = UUID.randomUUID();
        List<UUID> lista = List.of(unicoRepartidor);

        // Act
        UUID resultado = strategy.asignarRepartidor(idPedido, lista);

        // Assert
        assertEquals(unicoRepartidor, resultado);
    }

    @Test
    @DisplayName("AleatoriaStrategy distribuye asignaciones entre repartidores (estadístico)")
    void aleatoriaStrategyDistribuyeAsignaciones() {
        // Arrange
        AleatoriaStrategy strategy = new AleatoriaStrategy();
        UUID idPedido = UUID.randomUUID();
        UUID r1 = UUID.randomUUID();
        UUID r2 = UUID.randomUUID();
        UUID r3 = UUID.randomUUID();
        List<UUID> repartidores = List.of(r1, r2, r3);

        Set<UUID> repartidoresAsignados = new HashSet<>();

        // Act
        for (int i = 0; i < 100; i++) {
            UUID asignado = strategy.asignarRepartidor(idPedido, repartidores);
            repartidoresAsignados.add(asignado);
        }

        // Assert - con 100 iteraciones, es muy probable que al menos 2 repartidores distintos sean elegidos
        assertTrue(repartidoresAsignados.size() >= 2,
                "Deberían asignarse al menos 2 repartidores distintos en 100 iteraciones");
    }

    // ===================== MenorCargaStrategy =====================

    @Test
    @DisplayName("MenorCargaStrategy asigna al repartidor con menor carga")
    void menorCargaStrategyAsignaMenorCarga() {
        // Arrange
        UUID r1 = UUID.randomUUID(); // carga 5
        UUID r2 = UUID.randomUUID(); // carga 2 (menor)
        UUID r3 = UUID.randomUUID(); // carga 8

        Map<UUID, Integer> cargas = Map.of(r1, 5, r2, 2, r3, 8);
        MenorCargaStrategy strategy = new MenorCargaStrategy(cargas);

        UUID idPedido = UUID.randomUUID();
        List<UUID> repartidores = List.of(r1, r2, r3);

        // Act
        UUID resultado = strategy.asignarRepartidor(idPedido, repartidores);

        // Assert
        assertEquals(r2, resultado, "Debe asignar al repartidor con carga 2");
    }

    @Test
    @DisplayName("MenorCargaStrategy asigna al repartidor con carga cero")
    void menorCargaStrategyCargaCero() {
        // Arrange
        UUID r1 = UUID.randomUUID(); // carga 3
        UUID r2 = UUID.randomUUID(); // carga 0
        UUID r3 = UUID.randomUUID(); // carga 7

        Map<UUID, Integer> cargas = Map.of(r1, 3, r2, 0, r3, 7);
        MenorCargaStrategy strategy = new MenorCargaStrategy(cargas);

        UUID idPedido = UUID.randomUUID();
        List<UUID> repartidores = List.of(r1, r2, r3);

        // Act
        UUID resultado = strategy.asignarRepartidor(idPedido, repartidores);

        // Assert
        assertEquals(r2, resultado, "Debe asignar al repartidor con carga 0");
    }

    @Test
    @DisplayName("MenorCargaStrategy trata carga no registrada como 0")
    void menorCargaStrategyCargaNoRegistrada() {
        // Arrange
        UUID r1 = UUID.randomUUID(); // carga 3
        UUID r2 = UUID.randomUUID(); // no está en el mapa -> carga 0

        Map<UUID, Integer> cargas = Map.of(r1, 3);
        MenorCargaStrategy strategy = new MenorCargaStrategy(cargas);

        UUID idPedido = UUID.randomUUID();
        List<UUID> repartidores = List.of(r1, r2);

        // Act
        UUID resultado = strategy.asignarRepartidor(idPedido, repartidores);

        // Assert
        assertEquals(r2, resultado, "Repartidor sin carga registrada debe ser tratado como 0");
    }

    @Test
    @DisplayName("MenorCargaStrategy lanza excepción con lista vacía")
    void menorCargaStrategyListaVaciaLanzaExcepcion() {
        // Arrange
        MenorCargaStrategy strategy = new MenorCargaStrategy(Map.of());
        UUID idPedido = UUID.randomUUID();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> strategy.asignarRepartidor(idPedido, Collections.emptyList()));
    }

    @Test
    @DisplayName("MenorCargaStrategy lanza excepción con lista nula")
    void menorCargaStrategyListaNulaLanzaExcepcion() {
        // Arrange
        MenorCargaStrategy strategy = new MenorCargaStrategy(Map.of());
        UUID idPedido = UUID.randomUUID();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> strategy.asignarRepartidor(idPedido, null));
    }

    @Test
    @DisplayName("MenorCargaStrategy con empate retorna uno de los empatados")
    void menorCargaStrategyConEmpate() {
        // Arrange
        UUID r1 = UUID.randomUUID(); // carga 2
        UUID r2 = UUID.randomUUID(); // carga 2 (empate)
        UUID r3 = UUID.randomUUID(); // carga 5

        Map<UUID, Integer> cargas = Map.of(r1, 2, r2, 2, r3, 5);
        MenorCargaStrategy strategy = new MenorCargaStrategy(cargas);

        UUID idPedido = UUID.randomUUID();
        List<UUID> repartidores = List.of(r1, r2, r3);

        // Act
        UUID resultado = strategy.asignarRepartidor(idPedido, repartidores);

        // Assert
        assertTrue(resultado.equals(r1) || resultado.equals(r2),
                "Debe asignar a uno de los repartidores empatados con carga 2");
    }

    // ===================== INTERCAMBIABILIDAD (polimorfismo) =====================

    @Test
    @DisplayName("Ambas estrategias implementan la interfaz y son intercambiables")
    void estrategiasSonIntercambiables() {
        // Arrange
        AsignacionRepartidorStrategy aleatoria = new AleatoriaStrategy();
        AsignacionRepartidorStrategy menorCarga = new MenorCargaStrategy(Map.of());

        UUID idPedido = UUID.randomUUID();
        List<UUID> repartidores = List.of(UUID.randomUUID(), UUID.randomUUID());

        // Act
        UUID resultado1 = aleatoria.asignarRepartidor(idPedido, repartidores);
        UUID resultado2 = menorCarga.asignarRepartidor(idPedido, repartidores);

        // Assert
        assertNotNull(resultado1);
        assertNotNull(resultado2);
        assertTrue(repartidores.contains(resultado1));
        assertTrue(repartidores.contains(resultado2));
    }
}