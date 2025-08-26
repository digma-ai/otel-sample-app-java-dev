package org.springframework.samples.petclinic.patientrecords;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.samples.petclinic.model.PatientRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PatientRecordRepository extends JpaRepository<PatientRecord, Integer> {

    @Query("SELECT pr FROM PatientRecord pr WHERE pr.treatmentType = :treatmentType " +
           "AND pr.patientWeight >= :minWeight AND pr.patientWeight <= :maxWeight " +
           "AND pr.visitDate >= :startDate AND pr.visitDate < :endDate " +
           "AND pr.treatmentCompleted = :treatmentCompleted")
    List<PatientRecord> findByComplexCriteria(
        @Param("treatmentType") String treatmentType,
        @Param("minWeight") Integer minWeight,
        @Param("maxWeight") Integer maxWeight,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("treatmentCompleted") Boolean treatmentCompleted
    );

    @Query("SELECT COUNT(1) FROM PatientRecord pr WHERE pr.treatmentType = :treatmentType")
    int countRecordsByTreatmentType(@Param("treatmentType") String treatmentType);

    @Query("SELECT COUNT(1) FROM PatientRecord pr WHERE pr.treatmentType = :treatmentType " +
           "AND pr.treatmentCompleted = true")
    int countCompletedRecordsByTreatmentType(@Param("treatmentType") String treatmentType);
} 