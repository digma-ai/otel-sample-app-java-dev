-- MySQL schema for patient records table

DROP TABLE IF EXISTS patient_records;

CREATE TABLE patient_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    treatment_type VARCHAR(255) NOT NULL,
    patient_weight INT NOT NULL,
    visit_date TIMESTAMP NOT NULL,
    treatment_completed BOOLEAN NOT NULL,
    medical_notes TEXT,
    
    INDEX idx_treatment_type (treatment_type),
    INDEX idx_visit_date (visit_date),
    INDEX idx_treatment_completed (treatment_completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; 