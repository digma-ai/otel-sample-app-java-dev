package org.springframework.samples.petclinic.patientrecords;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.samples.petclinic.model.PatientRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Profile({"postgres", "mysql"})
public class PatientRecordDataService {

    private static final Logger logger = LoggerFactory.getLogger(PatientRecordDataService.class);
    private static final int BATCH_SIZE = 1000;

    private final PatientRecordRepository repository;
    private final JdbcTemplate mysqlJdbcTemplate;
    private final PlatformTransactionManager mysqlTransactionManager;

    // List of veterinary treatment types
    private static final List<String> TREATMENT_TYPES = List.of(
            "Annual Wellness Exam", "Vaccination (Rabies)", "Vaccination (DHPP)", "Vaccination (FVRCP)",
            "Dental Cleaning", "Dental Extraction", "Spay Surgery", "Neuter Surgery",
            "Wound Care", "Emergency Treatment", "X-Ray Examination", "Blood Work",
            "Flea Treatment", "Tick Treatment", "Heartworm Prevention", "Microchip Implant",
            "Grooming Service", "Nail Trimming", "Ear Cleaning", "Eye Examination",
            "Skin Condition Treatment", "Allergy Treatment", "Pain Management", "Post-Surgery Follow-up"
    );

    private final Random random = new Random();

    @Autowired
    public PatientRecordDataService(PatientRecordRepository repository,
                                   @Qualifier("mysqlJdbcTemplate") JdbcTemplate mysqlJdbcTemplate,
                                   @Qualifier("mysqlTransactionManager") PlatformTransactionManager mysqlTransactionManager) {
        this.repository = repository;
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
        this.mysqlTransactionManager = mysqlTransactionManager;
    }

    @Transactional("mysqlTransactionManager")
    public void cleanupPatientRecords() {
        logger.info("Received request to clean up all patient records.");
        long startTime = System.currentTimeMillis();
        try {
            repository.deleteAllInBatch(); // Efficiently delete all entries
            long endTime = System.currentTimeMillis();
            logger.info("Successfully cleaned up all patient records in {} ms.", (endTime - startTime));
        } catch (Exception e) {
            logger.error("Error during patient records cleanup", e);
            throw new RuntimeException("Error cleaning up patient records: " + e.getMessage(), e);
        }
    }

    @Transactional("mysqlTransactionManager")
    public void populateData(int totalEntries) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting MySQL batch insert data population of {} patient records.", totalEntries);
        
        try {
            populateDataWithMySqlBatchInNewTransaction(totalEntries);
        } catch (Exception e) {
            logger.error("Error during patient records population", e);
            throw new RuntimeException("Error during patient records population: " + e.getMessage(), e);
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("Finished patient records population for {} records in {} ms.", totalEntries, (endTime - startTime));
    }

    private void populateDataWithMySqlBatchInNewTransaction(int totalEntries) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = mysqlTransactionManager.getTransaction(def);
        
        try {
            Faker faker = new Faker(new Locale("en-US"));
            
            // MySQL-specific batch insert using multiple VALUES
            String sql = "INSERT INTO patient_records (treatment_type, patient_weight, visit_date, treatment_completed, medical_notes) VALUES ";
            
            for (int i = 0; i < totalEntries; ) {
                StringBuilder batchSql = new StringBuilder(sql);
                List<Object> batchParams = new ArrayList<>();
                
                int batchCount = 0;
                for (int j = 0; j < BATCH_SIZE && i < totalEntries; j++, i++, batchCount++) {
                    if (j > 0) {
                        batchSql.append(", ");
                    }
                    batchSql.append("(?, ?, ?, ?, ?)");
                    
                    String treatmentType = TREATMENT_TYPES.get(random.nextInt(TREATMENT_TYPES.size()));
                    int patientWeight = faker.number().numberBetween(500, 50_000); // 0.5kg to 50kg in grams
                    LocalDateTime visitDate = LocalDateTime.ofInstant(
                        faker.date().past(2 * 365, TimeUnit.DAYS).toInstant(), ZoneId.systemDefault());
                    boolean treatmentCompleted = faker.bool().bool();
                    String medicalNotes = generateVeterinaryNotes(faker, treatmentType);
                    
                    batchParams.add(treatmentType);
                    batchParams.add(patientWeight);
                    batchParams.add(visitDate);
                    batchParams.add(treatmentCompleted);
                    batchParams.add(medicalNotes);
                }
                
                if (batchCount > 0) {
                    mysqlJdbcTemplate.update(batchSql.toString(), batchParams.toArray());
                    
                    if (logger.isInfoEnabled()) {
                        logger.info("MySQL batch inserted {} / {} patient records...", i, totalEntries);
                    }
                }
            }
            
            mysqlTransactionManager.commit(status);
            
        } catch (Exception e) {
            if (!status.isCompleted()) {
                mysqlTransactionManager.rollback(status);
            }
            logger.error("Error during MySQL batch population with new transaction", e);
            throw new RuntimeException("Error during MySQL batch population: " + e.getMessage(), e);
        }
    }

    private String generateVeterinaryNotes(Faker faker, String treatmentType) {
        StringBuilder notes = new StringBuilder();
        
        // Treatment-specific notes
        switch (treatmentType) {
            case "Annual Wellness Exam":
                notes.append("Patient appears healthy. Weight within normal range. ");
                notes.append("Heart rate: ").append(faker.number().numberBetween(60, 120)).append(" bpm. ");
                notes.append("Temperature: ").append(faker.number().numberBetween(100, 103)).append("°F. ");
                break;
            case "Dental Cleaning":
                notes.append("Dental tartar buildup noted. Cleaning performed under anesthesia. ");
                notes.append("Extracted ").append(faker.number().numberBetween(0, 3)).append(" damaged teeth. ");
                break;
            case "Vaccination (Rabies)":
            case "Vaccination (DHPP)":
            case "Vaccination (FVRCP)":
                notes.append("Vaccination administered successfully. ");
                notes.append("Next vaccination due in 1 year. ");
                notes.append("No adverse reactions observed. ");
                break;
            case "Emergency Treatment":
                notes.append("Emergency case: ").append(faker.medical().symptoms()).append(". ");
                notes.append("Immediate treatment provided. ");
                break;
            default:
                notes.append("Treatment performed as scheduled. ");
                notes.append("Patient responded well to procedure. ");
        }
        
        // Add general veterinary observations
        notes.append("Owner compliance: ").append(faker.options().option("Excellent", "Good", "Fair"));
        notes.append(". Follow-up: ").append(faker.options().option("Not needed", "1 week", "2 weeks", "1 month"));
        notes.append(". Additional notes: ").append(faker.lorem().sentence());
        
        return notes.toString();
    }
} 