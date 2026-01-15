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
	@Autowired
	private OpenTelemetry openTelemetry;

	@Override
	public void start() {
		if (isRunning()) {
			return;
		}

		var otelTracer = openTelemetry.getTracer("MonitorService");

		running = true;
		backgroundThread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted() && running) {
				Span span = null;
				try {
					Thread.sleep(5000);
					span = otelTracer.spanBuilder("monitor").startSpan();
					System.out.println("Background service is running...");
					monitor();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					running = false;
					break;
				} catch (Exception e) {
					if (span != null) {
						span.recordException(e);
						span.setStatus(StatusCode.ERROR);
					}
					System.err.println("Error in monitor service: " + e.getMessage());
				} finally {
					if (span != null) {
						span.end();
					}
				}
			}
		});

		backgroundThread.setName("MonitorService-Thread");
		backgroundThread.start();
		System.out.println("Background service started.");
	}private void monitor() {
		if (!running) {
			throw new IllegalStateException("Monitor service is not running");
		}
		try {
			// Add monitoring logic here
		} catch (Exception e) {
			throw new MonitoringException("Failed to execute monitoring", e);
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