package com.curix.repository;

import com.curix.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByUserId(Long userId);
    Optional<PatientProfile> findByPatientNumber(String patientNumber);

    @Query("SELECT u.email FROM PatientProfile p JOIN p.user u WHERE p.id = :patientId")
    Optional<String> findUserEmailByPatientId(@Param("patientId") Long patientId);
}
