package org.springframework.samples.petclinic.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private static final long INITIAL_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60000; // 1 minute max delay
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService executor;
    private long currentDelay = INITIAL_DELAY_MS;
    
    public MonitorService() {
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }
    
    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            scheduleNextExecution();
            logger.info("Monitor service started");
        } else {
            logger.warn("Monitor service is already running");
        }
    }
    
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            executor.shutdown();
            logger.info("Monitor service stopped");
        } else {
            logger.warn("Monitor service is already stopped");
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    private void scheduleNextExecution() {
        if (!running.get()) {
            return;
        }
        
        executor.schedule(this::executeMonitoringTask, currentDelay, TimeUnit.MILLISECONDS);
    }
    
    private void executeMonitoringTask() {
        try {
            if (!running.get()) {
                return;
            }
            
            performMonitoring();
            // Reset delay on successful execution
            currentDelay = INITIAL_DELAY_MS;
            
        } catch (Exception e) {
            handleError(e);
        } finally {
            scheduleNextExecution();
        }
    }
    
    private void performMonitoring() {
        // Implement actual monitoring logic here
        logger.debug("Performing monitoring task");
    }
    
    private void handleError(Exception e) {
        logger.error("Error during monitoring execution: {}", e.getMessage(), e);
        
        // Implement exponential backoff
        currentDelay = Math.min(currentDelay * 2, MAX_DELAY_MS);
        logger.info("Retrying in {} ms", currentDelay);
    }
    
    @PreDestroy
    public void shutdown() {
        stop();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}