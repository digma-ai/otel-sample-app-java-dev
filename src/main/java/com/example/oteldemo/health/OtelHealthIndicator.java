package com.example.oteldemo.health;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OtelHealthIndicator implements HealthIndicator {
    private final OpenTelemetry openTelemetry;

    public OtelHealthIndicator(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public Health health() {
        try {
            // Check if OpenTelemetry is properly initialized
            if (openTelemetry != null && openTelemetry.getTracerProvider() != null) {
                return Health.up()
                    .withDetail("status", "Operational")
                    .withDetail("implementation", openTelemetry.getClass().getSimpleName())
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "Not initialized")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("status", "Error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}