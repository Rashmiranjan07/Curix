package com.curix.repository;

import com.curix.entity.Appointment;
import com.curix.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDescStartTimeDesc(Long doctorId);

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusNot(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    boolean existsByDoctorIdAndAppointmentDateAndStartTimeAndStatusNot(
            Long doctorId, LocalDate appointmentDate, LocalTime startTime, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a " +
           "WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status != 'CANCELLED'")
    List<Appointment> findActiveBookingsForDoctorOnDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);
}