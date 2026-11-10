package com.rappistream.dashboardaggregator.domain;

public enum OrderStatus {
    EN_CAMINO {
        @Override
        OrderStatus after(EventType eventType) {
            if (eventType == EventType.DRIVER_ARRIVING) {
                return LLEGANDO;
            }
            throw new InvalidOrderTransitionException("Expected DRIVER_ARRIVING after EN_CAMINO");
        }
    },
    LLEGANDO {
        @Override
        OrderStatus after(EventType eventType) {
            if (eventType == EventType.ORDER_DELIVERED) {
                return ENTREGADO;
            }
            throw new InvalidOrderTransitionException("Expected ORDER_DELIVERED after LLEGANDO");
        }
    },
    ENTREGADO {
        @Override
        OrderStatus after(EventType eventType) {
            throw new InvalidOrderTransitionException("ENTREGADO is a terminal order status");
        }
    };

    abstract OrderStatus after(EventType eventType);
}
