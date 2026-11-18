package com.callscheduler;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class TestConfig {

    // «Сейчас» в тестах: понедельник 16.11.2026, 05:00 по Алматы
    static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
