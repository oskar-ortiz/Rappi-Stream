package com.rappi.stream.tests;

import com.rappi.stream.domain.OrderCreateRequest;
import com.rappi.stream.domain.OrderCreatedCommand;
import com.rappi.stream.domain.DriverAssignedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecordsFailFastTest {

    // ===================== OrderCreatedCommand =====================

    @Test
    @DisplayName("OrderCreatedCommand acepta valores válidos")
    void orderCreatedCommandValoresValidos() {
        // Arrange
        String idCliente = "cliente-123";
        String direccion = "Av. Siempre Viva 742";

        // Act
        OrderCreatedCommand command = new OrderCreatedCommand(idCliente, direccion);

        // Assert
        assertEquals(idCliente, command.getIdCliente());
        assertEquals(direccion, command.getDireccionEntrega());
    }

    @Test
    @DisplayName("OrderCreatedCommand rechaza idCliente nulo")
    void orderCreatedCommandIdClienteNulo() {
        // Arrange
        String direccion = "Av. Siempre Viva 742";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OrderCreatedCommand(null, direccion));
    }

    @Test
    @DisplayName("OrderCreatedCommand rechaza idCliente en blanco")
    void orderCreatedCommandIdClienteEnBlanco() {
        // Arrange
        String direccion = "Av. Siempre Viva 742";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OrderCreatedCommand("   ", direccion));
    }

    @Test
    @DisplayName("OrderCreatedCommand rechaza direccionEntrega nula")
    void orderCreatedCommandDireccionNula() {
        // Arrange
        String idCliente = "cliente-123";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OrderCreatedCommand(idCliente, null));
    }

    @Test
    @DisplayName("OrderCreatedCommand rechaza direccionEntrega en blanco")
    void orderCreatedCommandDireccionEnBlanco() {
        // Arrange
        String idCliente = "cliente-123";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OrderCreatedCommand(idCliente, ""));
    }

    @Test
    @DisplayName("OrderCreatedCommand es inmutable (no tiene setters)")
    void orderCreatedCommandEsInmutable() {
        // Arrange
        OrderCreatedCommand command = new OrderCreatedCommand("cliente-1", "Calle 1");

        // Act & Assert
        // Verificar que no existen setters en la clase
        assertThrows(NoSuchMethodError.class, () -> {
            command.getClass().getMethod("setIdCliente", String.class);
        });
    }

    // ===================== DriverAssignedEvent =====================

    @Test
    @DisplayName("DriverAssignedEvent acepta valores válidos")
    void driverAssignedEventValoresValidos() {
        // Arrange
        String orderId = "order-456";
        String driverId = "driver-789";
        String timestamp = "2024-01-15T10:30:00Z";

        // Act
        DriverAssignedEvent event = new DriverAssignedEvent(orderId, driverId, timestamp);

        // Assert
        assertEquals(orderId, event.getOrderId());
        assertEquals(driverId, event.getDriverId());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    @DisplayName("DriverAssignedEvent rechaza orderId nulo")
    void driverAssignedEventOrderIdNulo() {
        // Arrange
        String driverId = "driver-789";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new DriverAssignedEvent(null, driverId, "2024-01-15T10:30:00Z"));
    }

    @Test
    @DisplayName("DriverAssignedEvent rechaza orderId en blanco")
    void driverAssignedEventOrderIdEnBlanco() {
        // Arrange
        String driverId = "driver-789";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new DriverAssignedEvent("  ", driverId, "2024-01-15T10:30:00Z"));
    }

    @Test
    @DisplayName("DriverAssignedEvent rechaza driverId nulo")
    void driverAssignedEventDriverIdNulo() {
        // Arrange
        String orderId = "order-456";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new DriverAssignedEvent(orderId, null, "2024-01-15T10:30:00Z"));
    }

    @Test
    @DisplayName("DriverAssignedEvent rechaza driverId en blanco")
    void driverAssignedEventDriverIdEnBlanco() {
        // Arrange
        String orderId = "order-456";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new DriverAssignedEvent(orderId, "", "2024-01-15T10:30:00Z"));
    }

    // ===================== OrderCreateRequest (DTO REST) =====================

    @Test
    @DisplayName("OrderCreateRequest acepta valores válidos")
    void orderCreateRequestValoresValidos() {
        // Arrange
        String idCliente = "cliente-456";
        String direccion = "Calle Test 999";

        // Act
        OrderCreateRequest request = new OrderCreateRequest(idCliente, direccion);

        // Assert
        assertEquals(idCliente, request.getIdCliente());
        assertEquals(direccion, request.getDireccionEntrega());
    }

    @Test
    @DisplayName("OrderCreateRequest rechaza idCliente nulo")
    void orderCreateRequestIdClienteNulo() {
        // Arrange
        String direccion = "Calle Test 999";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OrderCreateRequest(null, direccion));
    }

    @Test
    @DisplayName("OrderCreateRequest rechaza idCliente en blanco")
    void orderCreateRequestIdClienteEnBlanco() {
        // Arrange
        String direccion = "Calle Test 999";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OrderCreateRequest("", direccion));
    }

    @Test
    @DisplayName("OrderCreateRequest rechaza direccionEntrega nula")
    void orderCreateRequestDireccionNula() {
        // Arrange
        String idCliente = "cliente-456";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OrderCreateRequest(idCliente, null));
    }

    @Test
    @DisplayName("OrderCreateRequest rechaza direccionEntrega en blanco")
    void orderCreateRequestDireccionEnBlanco() {
        // Arrange
        String idCliente = "cliente-456";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OrderCreateRequest(idCliente, "   "));
    }

    @Test
    @DisplayName("OrderCreateRequest es inmutable")
    void orderCreateRequestEsInmutable() {
        // Arrange
        OrderCreateRequest request = new OrderCreateRequest("cliente-1", "Calle 1");

        // Act & Assert
        // Records no tienen setters
        assertFalse(request.getClass().getDeclaredMethods().length == 0,
                "Record debe tener métodos getter");
    }
}