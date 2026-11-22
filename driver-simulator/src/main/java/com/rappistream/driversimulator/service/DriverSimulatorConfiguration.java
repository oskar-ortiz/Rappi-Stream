package com.rappistream.driversimulator.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class DriverSimulatorConfiguration {
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService driverTripScheduler() {
        return Executors.newScheduledThreadPool(2);
    }
}
