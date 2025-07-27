package org.springframework.samples.petclinic.patientrecords;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/patient-records")
@Profile({"postgres", "mysql"})
public class PatientRecordController implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(PatientRecordController.class);

    private final PatientRecordDataService dataService;
    private final PatientRecordRepository repository;
    private final JdbcTemplate mysqlJdbcTemplate;

    @Autowired
    private OpenTelemetry openTelemetry;

    private Tracer otelTracer;

    @Autowired
    public PatientRecordController(PatientRecordDataService dataService,
                                   PatientRecordRepository repository,
                                   @Qualifier("mysqlJdbcTemplate") JdbcTemplate mysqlJdbcTemplate) {
        this.dataService = dataService;
        this.repository = repository;
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.otelTracer = openTelemetry.getTracer("PatientRecordController");
    }

    @PostMapping("/populate-records")
    public ResponseEntity<String> populateData(@RequestParam(name = "count", defaultValue = "6000000") int count) {
        logger.info("Received request to populate {} patient records.", count);
        if (count <= 0) {
            return ResponseEntity.badRequest().body("Count must be a positive integer.");
        }
        if (count > 10_000_000) {
            return ResponseEntity.badRequest().body("Count too high - maximum 10,000,000 patient records.");
        }
        try {
            dataService.populateData(count);
            return ResponseEntity.ok("Successfully initiated population of " + count + " patient records.");
        } catch (Exception e) {
            logger.error("Error during patient records population", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during data population: " + e.getMessage());
        }
    }

    @GetMapping(value = "/query-records", produces = "application/json")
    public List<Map<String, Object>> getRecords(
            @RequestParam(name = "weight", defaultValue = "5000") int patientWeight,
            @RequestParam(name = "repetitions", defaultValue = "1") int repetitions) {
        
        logger.info("Querying patient records by weight: {} (repetitions: {})", patientWeight, repetitions);
        
        String sql = "SELECT id, treatment_type, patient_weight, visit_date, treatment_completed, medical_notes " +
                    "FROM patient_records WHERE patient_weight = ?";
        
        List<Map<String, Object>> lastResults = null;
        for (int i = 0; i < repetitions; i++) {
            lastResults = mysqlJdbcTemplate.queryForList(sql, patientWeight);
        }
        
        logger.info("Query completed. Found {} records for weight: {}", 
                   lastResults != null ? lastResults.size() : 0, patientWeight);
        return lastResults;
    }

    @DeleteMapping("/cleanup-records")
    public ResponseEntity<String> cleanupRecords() {
        logger.info("Received request to cleanup all patient records.");
        try {
            dataService.cleanupPatientRecords();
            return ResponseEntity.ok("Successfully cleaned up all patient records.");
        } catch (Exception e) {
            logger.error("Error during patient records cleanup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during cleanup: " + e.getMessage());
        }
    }

    @PostMapping("/recreate-and-populate-records")
    public ResponseEntity<String> recreateAndPopulateRecords(@RequestParam(name = "count", defaultValue = "6000000") int count) {
        logger.info("Received request to recreate and populate {} patient records.", count);
        if (count <= 0) {
            return ResponseEntity.badRequest().body("Count must be a positive integer.");
        }
        if (count > 10_000_000) {
            return ResponseEntity.badRequest().body("Count too high - maximum 10,000,000 patient records.");
        }
        try {
            // Drop the table
            mysqlJdbcTemplate.execute("DROP TABLE IF EXISTS patient_records");
            logger.info("Table 'patient_records' dropped successfully.");

            // Recreate the table with MySQL syntax
            String createTableSql = 
                "CREATE TABLE patient_records (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "treatment_type VARCHAR(255) NOT NULL," +
                "patient_weight INT NOT NULL," +
                "visit_date TIMESTAMP NOT NULL," +
                "treatment_completed BOOLEAN NOT NULL," +
                "medical_notes TEXT," +
                "INDEX idx_treatment_type (treatment_type)," +
                "INDEX idx_visit_date (visit_date)," +
                "INDEX idx_treatment_completed (treatment_completed)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            
            mysqlJdbcTemplate.execute(createTableSql);
            logger.info("Table 'patient_records' created successfully.");

            // Populate data
            dataService.populateData(count);
            return ResponseEntity.ok("Successfully recreated and initiated population of " + count + " patient records.");
        } catch (Exception e) {
            logger.error("Error during patient records recreation and population", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during data recreation and population: " + e.getMessage());
        }
    }

    @GetMapping("/run-simulated-queries")
    public ResponseEntity<String> runSimulatedQueries(
        @RequestParam(name = "uniqueQueriesCount", defaultValue = "3") int uniqueQueriesCount,
        @RequestParam(name = "repetitions", defaultValue = "100") int repetitions
    ) {
        long startTime = System.currentTimeMillis();
        int totalOperations = 0;

        for (int queryTypeIndex = 0; queryTypeIndex < uniqueQueriesCount; queryTypeIndex++) {
            char queryTypeChar = (char) ('A' + queryTypeIndex);
            String parentSpanName = "PatientBatch_Type" + queryTypeChar;
            Span typeParentSpan = otelTracer.spanBuilder(parentSpanName).startSpan();

            try (Scope scope = typeParentSpan.makeCurrent()) {
                for (int execution = 1; execution <= repetitions; execution++) {
                    String operationName = "SimulatedPatientQuery_Type" + queryTypeChar;
                    performObservablePatientOperation(operationName);
                    totalOperations++;
                }
            } finally {
                typeParentSpan.end();
            }
        }

        long endTime = System.currentTimeMillis();
        String message = String.format("Executed %d simulated patient query operations in %d ms.", totalOperations, (endTime - startTime));
        logger.info(message);
        return ResponseEntity.ok(message);
    }

    private void performObservablePatientOperation(String operationName) {
        Span span = otelTracer.spanBuilder(operationName)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("db.system", "mysql")
            .setAttribute("db.name", "petclinic")
            .setAttribute("db.statement", "SELECT * FROM some_patient_table" + operationName)
            .setAttribute("db.operation", "SELECT")
            .startSpan();
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 6));
            logger.debug("Executing simulated patient operation: {}", operationName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Simulated patient operation {} interrupted", operationName, e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }
} 