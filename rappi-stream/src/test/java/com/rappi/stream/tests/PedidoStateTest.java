package com.rappi.stream.tests;

import com.rappi.stream.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PedidoStateTest {

    // ===================== TRANSICIONES VÁLIDAS =====================

    @Test
    @DisplayName("Nuevo pedido inicia en estado CREADO")
    void nuevoPedidoIniciaEnCreado() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID cliente = UUID.randomUUID();
        String direccion = "Calle Falsa 123";

        // Act
        Pedido pedido = new Pedido(id, cliente, direccion);

        // Assert
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        assertNull(pedido.getIdRepartidorAsignado());
    }

    @Test
    @DisplayName("Transición válida CREADO -> ASIGNADO via asignarRepartidor()")
    void transicionValidaCreadoAsignado() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Av. Principal 1");
        UUID repartidor = UUID.randomUUID();

        // Act
        pedido.asignarRepartidor(repartidor);

        // Assert
        assertEquals(EstadoPedido.ASIGNADO, pedido.getEstado());
        assertEquals(repartidor, pedido.getIdRepartidorAsignado());
    }

    @Test
    @DisplayName("Transición válida ASIGNADO -> EN_CAMINO via registrarLlegada()")
    void transicionValidaAsignadoEnCamino() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Av. Principal 1");
        pedido.asignarRepartidor(UUID.randomUUID());

        // Act
        pedido.registrarLlegada();

        // Assert
        assertEquals(EstadoPedido.EN_CAMINO, pedido.getEstado());
        assertTrue(pedido.isEnCamino());
    }

    @Test
    @DisplayName("Transición válida EN_CAMINO -> ENTREGADO via completarEntrega()")
    void transicionValidaEnCaminoEntregado() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Av. Principal 1");
        pedido.asignarRepartidor(UUID.randomUUID());
        pedido.registrarLlegada();

        // Act
        pedido.completarEntrega();

        // Assert
        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
        assertTrue(pedido.isEntregado());
        assertNull(pedido.getIdRepartidorAsignado());
    }

    @Test
    @DisplayName("Ciclo completo CREADO -> ASIGNADO -> EN_CAMINO -> ENTREGADO")
    void cicloCompletoDeEstados() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");

        // Act
        pedido.asignarRepartidor(UUID.randomUUID());
        pedido.registrarLlegada();
        pedido.completarEntrega();

        // Assert
        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
    }

    // ===================== TRANSICIONES INVÁLIDAS =====================

    @Test
    @DisplayName("No se puede asignar repartidor a un pedido ya ASIGNADO")
    void asignarRepartidorEnPedidoAsignadoLanzaExcepcion() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");
        pedido.asignarRepartidor(UUID.randomUUID());

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> pedido.asignarRepartidor(UUID.randomUUID()));
    }

    @Test
    @DisplayName("No se puede registrar llegada a un pedido en CREADO (sin asignar)")
    void registrarLlegadaEnPedidoCreadoLanzaExcepcion() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> pedido.registrarLlegada());
    }

    @Test
    @DisplayName("No se puede completar entrega a un pedido en CREADO")
    void completarEntregaEnPedidoCreadoLanzaExcepcion() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> pedido.completarEntrega());
    }

    @Test
    @DisplayName("No se puede completar entrega a un pedido ASIGNADO (sin estar EN_CAMINO)")
    void completarEntregaEnPedidoAsignadoLanzaExcepcion() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");
        pedido.asignarRepartidor(UUID.randomUUID());

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> pedido.completarEntrega());
    }

    @Test
    @DisplayName("No se puede registrar llegada a un pedido ENTREGADO")
    void registrarLlegadaEnPedidoEntregadoLanzaExcepcion() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");
        pedido.asignarRepartidor(UUID.randomUUID());
        pedido.registrarLlegada();
        pedido.completarEntrega();

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> pedido.registrarLlegada());
    }

    @Test
    @DisplayName("No se puede reasignar repartidor a un pedido ENTREGADO")
    void asignarRepartidorEnPedidoEntregadoLanzaExcepcion() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");
        pedido.asignarRepartidor(UUID.randomUUID());
        pedido.registrarLlegada();
        pedido.completarEntrega();

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> pedido.asignarRepartidor(UUID.randomUUID()));
    }

    // ===================== MÉTODOS DE CONSULTA (is*) =====================

    @Test
    @DisplayName("isCreado() refleja correctamente el estado")
    void isCreadoReflejaEstado() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");

        // Act
        boolean esCreado = pedido.isCreado();

        // Assert
        assertTrue(esCreado);
        assertFalse(pedido.isAsignado());
        assertFalse(pedido.isEnCamino());
        assertFalse(pedido.isEntregado());
    }

    @Test
    @DisplayName("isAsignado() refleja correctamente el estado")
    void isAsignadoReflejaEstado() {
        // Arrange
        Pedido pedido = new Pedido(UUID.randomUUID(), UUID.randomUUID(), "Calle 5 #10");
        pedido.asignarRepartidor(UUID.randomUUID());

        // Act
        boolean esAsignado = pedido.isAsignado();

        // Assert
        assertTrue(esAsignado);
        assertFalse(pedido.isCreado());
    }

    // ===================== ESTADO_PEDIDO: puedeTransitarA =====================

    @Test
    @DisplayName("EstadoPedido.CREADO solo puede transitar a ASIGNADO")
    void estadoCreadoSoloTransitaAAsignado() {
        // Arrange
        // Act & Assert
        assertTrue(EstadoPedido.CREADO.puedeTransitarA(EstadoPedido.ASIGNADO));
        assertFalse(EstadoPedido.CREADO.puedeTransitarA(EstadoPedido.EN_CAMINO));
        assertFalse(EstadoPedido.CREADO.puedeTransitarA(EstadoPedido.ENTREGADO));
        assertFalse(EstadoPedido.CREADO.puedeTransitarA(EstadoPedido.CREADO));
    }

    @Test
    @DisplayName("EstadoPedido.ASIGNADO solo puede transitar a EN_CAMINO")
    void estadoAsignadoSoloTransitaAEnCamino() {
        // Arrange
        // Act & Assert
        assertTrue(EstadoPedido.ASIGNADO.puedeTransitarA(EstadoPedido.EN_CAMINO));
        assertFalse(EstadoPedido.ASIGNADO.puedeTransitarA(EstadoPedido.CREADO));
        assertFalse(EstadoPedido.ASIGNADO.puedeTransitarA(EstadoPedido.ENTREGADO));
    }

    @Test
    @DisplayName("EstadoPedido.EN_CAMINO solo puede transitar a ENTREGADO")
    void estadoEnCaminoSoloTransitaAEntregado() {
        // Arrange
        // Act & Assert
        assertTrue(EstadoPedido.EN_CAMINO.puedeTransitarA(EstadoPedido.ENTREGADO));
        assertFalse(EstadoPedido.EN_CAMINO.puedeTransitarA(EstadoPedido.CREADO));
        assertFalse(EstadoPedido.EN_CAMINO.puedeTransitarA(EstadoPedido.ASIGNADO));
    }

    @Test
    @DisplayName("EstadoPedido.ENTREGADO es estado terminal (sin transiciones)")
    void estadoEntregadoEsTerminal() {
        // Arrange
        // Act & Assert
        for (EstadoPedido destino : EstadoPedido.values()) {
            assertFalse(EstadoPedido.ENTREGADO.puedeTransitarA(destino),
                    "ENTREGADO no debería poder transitar a " + destino);
        }
    }

    // ===================== FÁBRICA restaurar() =====================

    @Test
    @DisplayName("Pedido.restaurar reconstruye un pedido en estado CREADO")
    void restaurarPedidoEnCreado() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID cliente = UUID.randomUUID();

        // Act
        Pedido pedido = Pedido.restaurar(id, EstadoPedido.CREADO, cliente, "Calle 1", null);

        // Assert
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        assertEquals(id, pedido.getId());
        assertEquals(cliente, pedido.getIdCliente());
        assertNull(pedido.getIdRepartidorAsignado());
    }

    @Test
    @DisplayName("Pedido.restaurar reconstruye un pedido en estado ASIGNADO")
    void restaurarPedidoAsignado() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID cliente = UUID.randomUUID();
        UUID repartidor = UUID.randomUUID();

        // Act
        Pedido pedido = Pedido.restaurar(id, EstadoPedido.ASIGNADO, cliente, "Calle 1", repartidor);

        // Assert
        assertEquals(EstadoPedido.ASIGNADO, pedido.getEstado());
        assertEquals(repartidor, pedido.getIdRepartidorAsignado());
    }

    @Test
    @DisplayName("Pedido.restaurar reconstruye un pedido ENTREGADO sin repartidor")
    void restaurarPedidoEntregado() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID cliente = UUID.randomUUID();

        // Act
        Pedido pedido = Pedido.restaurar(id, EstadoPedido.ENTREGADO, cliente, "Calle 1", null);

        // Assert
        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
        assertTrue(pedido.isEntregado());
        assertNull(pedido.getIdRepartidorAsignado());
    }

    @Test
    @DisplayName("Pedido restaurado conserva invariantes: no permite transiciones inválidas")
    void restauradoProtegeInvariantes() {
        // Arrange
        Pedido pedido = Pedido.restaurar(UUID.randomUUID(), EstadoPedido.ENTREGADO,
                UUID.randomUUID(), "Calle 1", null);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> pedido.asignarRepartidor(UUID.randomUUID()));
        assertThrows(IllegalStateException.class, () -> pedido.registrarLlegada());
        assertThrows(IllegalStateException.class, () -> pedido.completarEntrega());
    }
}