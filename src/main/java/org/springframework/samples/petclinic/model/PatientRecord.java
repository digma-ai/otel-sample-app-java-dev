package org.springframework.samples.petclinic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Patient Record entity for MySQL database
 * Represents veterinary patient treatment records
 */
@Entity
@Table(name = "patient_records")
public class PatientRecord extends BaseEntity {

    @Column(name = "treatment_type", nullable = false)
    private String treatmentType;

    @Column(name = "patient_weight", nullable = false)
    private Integer patientWeight;

    @Column(name = "visit_date", nullable = false)
    private LocalDateTime visitDate;

    @Column(name = "treatment_completed", nullable = false)
    private Boolean treatmentCompleted;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    // Default constructor
    public PatientRecord() {
    }

    // Constructor with all fields
    public PatientRecord(String treatmentType, Integer patientWeight, LocalDateTime visitDate, 
                        Boolean treatmentCompleted, String medicalNotes) {
        this.treatmentType = treatmentType;
        this.patientWeight = patientWeight;
        this.visitDate = visitDate;
        this.treatmentCompleted = treatmentCompleted;
        this.medicalNotes = medicalNotes;
    }

    // Getters and Setters
    public String getTreatmentType() {
        return treatmentType;
    }

    public void setTreatmentType(String treatmentType) {
        this.treatmentType = treatmentType;
    }

    public Integer getPatientWeight() {
        return patientWeight;
    }

    public void setPatientWeight(Integer patientWeight) {
        this.patientWeight = patientWeight;
    }

    public LocalDateTime getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDateTime visitDate) {
        this.visitDate = visitDate;
    }

    public Boolean getTreatmentCompleted() {
        return treatmentCompleted;
    }

    public void setTreatmentCompleted(Boolean treatmentCompleted) {
        this.treatmentCompleted = treatmentCompleted;
    }

    public String getMedicalNotes() {
        return medicalNotes;
    }

    public void setMedicalNotes(String medicalNotes) {
        this.medicalNotes = medicalNotes;
    }

    @Override
    public String toString() {
        return "PatientRecord{" +
                "id=" + getId() +
                ", treatmentType='" + treatmentType + '\'' +
                ", patientWeight=" + patientWeight +
                ", visitDate=" + visitDate +
                ", treatmentCompleted=" + treatmentCompleted +
                ", medicalNotes='" + medicalNotes + '\'' +
                '}';
    }
} 