package com.rappistream.orderprocessor.domain;

public enum OrderState {
    CREATED {
        @Override
        public OrderState assignDriver() {
            return DRIVER_ASSIGNED;
        }
    },
    DRIVER_ASSIGNED {
        @Override
        public OrderState assignDriver() {
            throw new InvalidStateTransitionException("Driver already assigned");
        }
    };

    public abstract OrderState assignDriver();
}
