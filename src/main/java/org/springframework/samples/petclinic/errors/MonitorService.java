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

	@Override
	public void start() {
		if (running) {
			return; // Already running
		}

		// Environment check
		String env = System.getProperty("spring.profiles.active", "default");
		if (!isValidEnvironment(env)) {
			throw new IllegalStateException("Invalid environment for monitor service: " + env);
		}

		var otelTracer = openTelemetry.getTracer("MonitorService");

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
					System.out.println("Background service is running...");
					monitor();
				} catch (Exception e) {
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
				} finally {
					span.end();
				}
			}
		});

		// Start the background thread
		backgroundThread.start();
		System.out.println("Background service started.");
	}

	private boolean isValidEnvironment(String env) {
		return env != null && (env.equals("production") || env.equals("staging") || env.equals("default"));
	}private void monitor() throws InvalidPropertiesFormatException {
		// State validation
		if (!running) {
			throw new IllegalStateException("Monitor service is not running");
		}

		// Environment check
		String env = System.getProperty("env", "prod");
		try {
			// Environment specific handling
			switch(env) {
				case "dev":
					// More lenient monitoring for dev
					System.out.println("Monitoring in dev mode");
					break;
				case "prod":
					// Stricter monitoring for prod
					System.out.println("Monitoring in prod mode");
					break;
				default:
					throw new IllegalStateException("Unknown environment: " + env);
			}
		} catch (Exception e) {
			// Recovery mechanism
			try {
				System.out.println("Attempting recovery...");
				running = true; // Reset state
				// Additional recovery logic here
			} catch (Exception recoveryEx) {
				running = false;
				throw new InvalidPropertiesFormatException("Recovery failed: " + recoveryEx.getMessage());
			}
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