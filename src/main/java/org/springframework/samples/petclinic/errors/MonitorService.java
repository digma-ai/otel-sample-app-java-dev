package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Componentpublic class MonitorService implements SmartLifecycle {

    private volatile boolean running = false;
    private Thread backgroundThread;
    private volatile boolean healthy = true;
    private int consecutiveFailures = 0;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long MONITORING_INTERVAL_MS = 5000;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");

        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                Span span = null;
                try {
                    span = otelTracer.spanBuilder("monitor").startSpan();
                    executeWithRetry(() -> {
                        monitor();
                        consecutiveFailures = 0;
                        healthy = true;
                        return null;
                    }, MAX_RETRIES);

                    Thread.sleep(MONITORING_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    consecutiveFailures++;
                    if (consecutiveFailures >= MAX_RETRIES) {
                        healthy = false;
                    }
                    if (span != null) {
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR);
                    }
                    logger.error("Monitor service error", e);
                } finally {
                    if (span != null) {
                        span.end();
                    }
                }
            }
        }, "MonitorService-Thread");

        backgroundThread.setDaemon(true);
        backgroundThread.start();
        logger.info("Monitor service started.");
    }

    private <T> T executeWithRetry(Callable<T> task, int maxRetries) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return task.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries - 1) {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                }
            }
        }
        throw lastException;
    }

    public boolean isHealthy() {
        return healthy;
    }private void monitor() {
    int retryCount = 0;
    int maxRetries = 3;
    long retryDelay = 1000; // 1 second

    while (retryCount < maxRetries) {
        try {
            // Perform health checks
            if (!performHealthCheck()) {
                throw new MonitoringException("Health check failed");
            }

            // Actual monitoring logic
            performMonitoring();
            break; // Success, exit retry loop

        } catch (Exception e) {
            retryCount++;
            if (retryCount >= maxRetries) {
                throw new MonitoringException("Monitor failed after " + maxRetries + " retries", e);
            }
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new MonitoringException("Monitoring interrupted", ie);
            }
        }
    }
}

private boolean performHealthCheck() {
    try {
        // Implement actual health check logic here
        return true;
    } catch (Exception e) {
        return false;
    }
}

private void performMonitoring() {
    // Implement actual monitoring logic here
}

@Override
public void stop() {
    running = false;
    if (backgroundThread != null) {
        try {
            backgroundThread.interrupt();
            backgroundThread.join(5000); // Wait up to 5 seconds for thread to finish
            if (backgroundThread.isAlive()) {
                // Log warning if thread doesn't stop
                System.err.println("Warning: Background thread did not stop gracefully");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Stop operation was interrupted");
        }
    }
    System.out.println("Background service stopped.");
}

@Override
public boolean isRunning() {
    return running && (backgroundThread != null && backgroundThread.isAlive());
}
}