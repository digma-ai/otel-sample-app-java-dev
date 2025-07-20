package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.InvalidPropertiesFormatException;

@Componentpublic class MonitorService implements SmartLifecycle {

    private boolean running = false;
    private Thread backgroundThread;
    @Autowired
    private OpenTelemetry openTelemetry;
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private CircuitBreaker circuitBreaker;

    @Override
    public void start() {
        var otelTracer = openTelemetry.getTracer("MonitorService");
        
        // Initialize circuit breaker
        circuitBreaker = CircuitBreaker.ofDefaults("monitorService");
        
        running = true;
        backgroundThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error("Monitor service interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                }
                
                Span span = otelTracer.spanBuilder("monitor").startSpan();
                try {
                    logger.debug("Executing monitoring cycle");
                    circuitBreaker.executeRunnable(() -> monitor());
                } catch (Exception e) {
                    logger.error("Error during monitoring cycle", e);
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                } finally {
                    span.end();
                }
            }
        });

        backgroundThread.setName("MonitorService-Thread");
        backgroundThread.start();
        logger.info("Background service started successfully");
    }private void monitor() throws InvalidPropertiesFormatException {
		Logger logger = LoggerFactory.getLogger(this.getClass());
		try {
			if (!isRunning()) {
				logger.warn("Cannot monitor - service is not running");
				return;
			}

			if (circuitBreaker.isOpen()) {
				logger.warn("Circuit breaker is open, skipping monitoring");
				return;
			}

			try {
				// Perform monitoring
				logger.info("Performing monitoring check");
				// Reset circuit breaker on success
				circuitBreaker.reset();
			} catch (Exception e) {
				circuitBreaker.recordFailure();
				logger.error("Monitor operation failed", e);
				throw new InvalidPropertiesFormatException("Monitor operation failed: " + e.getMessage());
			}
		} catch (Exception e) {
			logger.error("Unexpected error during monitoring", e);
			throw e;
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
		return running;
	}
}