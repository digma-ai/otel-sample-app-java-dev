package org.springframework.samples.petclinic.system;

import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @PostConstruct
    public void init() {
        running.set(true);
        logger.info("MonitorService initialized");
    }
    
    @PreDestroy
    public void shutdown() {
        running.set(false);
        logger.info("MonitorService shutting down");
    }
    
    public void monitor() {
        try {
            if (!isRunning()) {
                logger.warn("Monitor service is not running");
                return;
            }
            
            // Perform monitoring logic here
            logger.info("Monitoring system status...");
            
        } catch (Exception e) {
            logger.error("Error during monitoring", e);
            throw new AppException("Monitoring failed: " + e.getMessage());
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
}