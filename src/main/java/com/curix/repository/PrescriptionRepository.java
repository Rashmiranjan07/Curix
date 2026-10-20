package com.curix.repository;

import com.curix.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Prescription> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
}