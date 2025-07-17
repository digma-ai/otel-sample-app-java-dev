package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Componentpublic class MonitorService implements SmartLifecycle {

	private boolean running = false;
	private Thread backgroundThread;
	@Autowired
	private OpenTelemetry openTelemetry;
	
	@Value("${monitor.error.injection.enabled:false}")
	private boolean errorInjectionEnabled;
	
	private CircuitBreaker circuitBreaker;
	private int failureCount = 0;
	private static final int FAILURE_THRESHOLD = 3;
	private static final long RESET_TIMEOUT_MS = 30000;
	private long lastFailureTime;

	@Override
	public void start() {
		var otelTracer = openTelemetry.getTracer("MonitorService");
		circuitBreaker = CircuitBreaker.CLOSED;
		running = true;
		backgroundThread = new Thread(() -> {
			while (running) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				Span span = otelTracer.spanBuilder("monitor").startSpan();
				try {
					if (circuitBreaker == CircuitBreaker.OPEN) {
						if (System.currentTimeMillis() - lastFailureTime > RESET_TIMEOUT_MS) {
							circuitBreaker = CircuitBreaker.HALF_OPEN;
							span.setAttribute("circuit_breaker.state", "HALF_OPEN");
						} else {
							span.setAttribute("circuit_breaker.state", "OPEN");
							continue;
						}
					}
					System.out.println("Background service is running...");
					monitor();
					if (circuitBreaker == CircuitBreaker.HALF_OPEN) {
						circuitBreaker = CircuitBreaker.CLOSED;
						failureCount = 0;
					}
				} catch (Exception e) {
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
					handleFailure(span);
				} finally {
					span.end();
				}
			}
		});

		// Start the background thread
		backgroundThread.start();
		System.out.println("Background service started.");
	}private CircuitBreakerState circuitState = CircuitBreakerState.CLOSED;
private int failureCount = 0;
private long lastFailureTime = 0;
private static final int FAILURE_THRESHOLD = 3;
private static final long RESET_TIMEOUT_MS = 5000;

@Value("${error.injection.enabled:false}")
private boolean errorInjectionEnabled;

private void monitor() {
    try {
        if (circuitState == CircuitBreakerState.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > RESET_TIMEOUT_MS) {
                circuitState = CircuitBreakerState.HALF_OPEN;
            } else {
                throw new CircuitBreakerOpenException("Circuit breaker is open");
            }
        }

        if (errorInjectionEnabled) {
            throw new RuntimeException("Injected error for testing");
        }

        // Attempt the operation
        try (Scope scope = GlobalOpenTelemetry.get().getTracer("monitor")
                .spanBuilder("monitoring.check")
                .startSpan()
                .makeCurrent()) {
            // Normal monitoring logic here
            if (circuitState == CircuitBreakerState.HALF_OPEN) {
                circuitState = CircuitBreakerState.CLOSED;
                failureCount = 0;
            }
        }
    } catch (Exception e) {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        
        if (failureCount >= FAILURE_THRESHOLD) {
            circuitState = CircuitBreakerState.OPEN;
        }
        
        // Log and report the error
        GlobalOpenTelemetry.get().getTracer("monitor")
            .spanBuilder("monitoring.error")
            .setAttribute("error", true)
            .setAttribute("error.message", e.getMessage())
            .startSpan()
            .end();
            
        throw new MonitoringException("Monitoring failed", e);
    }
}

@Override
public void stop() {
    // Stop the background task
    running = false;
    if (backgroundThread != null) {
        try {
            backgroundThread.join(); // Wait for the thread to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    System.out.println("Background service stopped.");
}

@Override
public boolean isRunning() {
    return false;
}

private enum CircuitBreakerState {
    CLOSED, OPEN, HALF_OPEN
}
}