package com.curix.service.impl;

import com.curix.dto.request.BookAppointmentRequest;
import com.curix.dto.request.CancelAppointmentRequest;
import com.curix.dto.response.AppointmentResponse;
import com.curix.dto.response.AvailableSlotDto;
import com.curix.entity.Appointment;
import com.curix.entity.DoctorAvailability;
import com.curix.entity.DoctorProfile;
import com.curix.entity.PatientProfile;
import com.curix.entity.User;
import com.curix.enums.AppointmentStatus;
import com.curix.enums.DayOfWeek;
import com.curix.enums.Role;
import com.curix.exception.ConflictException;
import com.curix.exception.ResourceNotFoundException;
import com.curix.repository.AppointmentRepository;
import com.curix.repository.DoctorAvailabilityRepository;
import com.curix.repository.DoctorProfileRepository;
import com.curix.repository.PatientProfileRepository;
import com.curix.repository.UserRepository;
import com.curix.service.AppointmentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  DoctorProfileRepository doctorProfileRepository,
                                  DoctorAvailabilityRepository doctorAvailabilityRepository,
                                  PatientProfileRepository patientProfileRepository,
                                  UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getAvailableSlots(Long doctorId, LocalDate date) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        DayOfWeek targetDay = DayOfWeek.valueOf(date.getDayOfWeek().name());
        List<DoctorAvailability> availabilities =
                doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctor.getId(), targetDay);

        if (availabilities.isEmpty()) {
            return Collections.emptyList();
        }

        List<Appointment> existingBookings =
                appointmentRepository.findActiveBookingsForDoctorOnDate(doctor.getId(), date);

        Set<LocalTime> bookedStartTimes = existingBookings.stream()
                .map(Appointment::getStartTime)
                .collect(Collectors.toSet());

        List<AvailableSlotDto> generatedSlots = new ArrayList<>();

        for (DoctorAvailability window : availabilities) {
            LocalTime current = window.getStartTime();
            int slotDuration = (window.getSlotMinutes() != null && window.getSlotMinutes() > 0)
                    ? window.getSlotMinutes() : 30;

            while (!current.plusMinutes(slotDuration).isAfter(window.getEndTime())) {
                LocalTime slotEnd = current.plusMinutes(slotDuration);
                boolean isTaken = bookedStartTimes.contains(current);

                generatedSlots.add(new AvailableSlotDto(date, current, slotEnd, !isTaken));
                current = slotEnd;
            }
        }

        return generatedSlots;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse bookAppointment(Long patientUserId, BookAppointmentRequest request) {
        PatientProfile patient = patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + patientUserId));

        DoctorProfile doctor = doctorProfileRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        DayOfWeek bookingDay = DayOfWeek.valueOf(request.getAppointmentDate().getDayOfWeek().name());
        List<DoctorAvailability> availabilities =
                doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctor.getId(), bookingDay);

        if (availabilities.isEmpty()) {
            throw new ConflictException("Doctor has no active consultation schedule on " + bookingDay);
        }

        DoctorAvailability matchedWindow = null;
        for (DoctorAvailability avail : availabilities) {
            if (!request.getStartTime().isBefore(avail.getStartTime()) &&
                !request.getStartTime().plusMinutes(avail.getSlotMinutes()).isAfter(avail.getEndTime())) {
                matchedWindow = avail;
                break;
            }
        }

        if (matchedWindow == null) {
            throw new ConflictException("Selected time slot is outside the doctor's operational schedule");
        }

        LocalTime calculatedEndTime = request.getStartTime().plusMinutes(matchedWindow.getSlotMinutes());

        boolean alreadyBooked = appointmentRepository.existsByDoctorIdAndAppointmentDateAndStartTimeAndStatusNot(
                doctor.getId(), request.getAppointmentDate(), request.getStartTime(), AppointmentStatus.CANCELLED);

        if (alreadyBooked) {
            throw new ConflictException("This appointment slot has already been reserved. Please select another slot.");
        }

        Appointment appointment = new Appointment(
                patient,
                doctor,
                request.getAppointmentDate(),
                request.getStartTime(),
                calculatedEndTime,
                AppointmentStatus.CONFIRMED,
                doctor.getConsultationFee(),
                request.getReasonForVisit()
        );

        try {
            Appointment saved = appointmentRepository.saveAndFlush(appointment);
            return mapToResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Concurrency Conflict: This slot was just booked by another patient. Please choose another time.");
        }
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, Long requestingUserId, CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + requestingUserId));

        boolean isPatientOwner = appointment.getPatient().getUser().getId().equals(requestingUserId);
        boolean isDoctorOwner = appointment.getDoctor().getUser().getId().equals(requestingUserId);
        boolean isAdmin = requestingUser.getRole() == Role.ADMIN;

        if (!isPatientOwner && !isDoctorOwner && !isAdmin) {
            throw new AccessDeniedException("Access Denied: You do not have permission to cancel this appointment");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Cannot cancel an appointment that has already been completed");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.getReason());

        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(Long appointmentId, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new AccessDeniedException("Access Denied: Only the assigned consulting doctor can mark this appointment as completed");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Cannot complete a cancelled appointment");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsForPatient(Long patientUserId) {
        PatientProfile patient = patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + patientUserId));

        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patient.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsForDoctor(Long doctorUserId) {
        DoctorProfile doctor = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user: " + doctorUserId));

        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDescStartTimeDesc(doctor.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long appointmentId, Long requestingUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        User user = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + requestingUserId));

        boolean isPatientOwner = appointment.getPatient().getUser().getId().equals(requestingUserId);
        boolean isDoctorOwner = appointment.getDoctor().getUser().getId().equals(requestingUserId);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isPatientOwner && !isDoctorOwner && !isAdmin) {
            throw new AccessDeniedException("Access Denied: You are not authorized to view this appointment record");
        }

        return mapToResponse(appointment);
    }

    private AppointmentResponse mapToResponse(Appointment appt) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(appt.getId());
        res.setPatientId(appt.getPatient().getId());
        res.setPatientName(appt.getPatient().getFirstName() + " " + appt.getPatient().getLastName());
        res.setPatientNumber(appt.getPatient().getPatientNumber());
        res.setDoctorId(appt.getDoctor().getId());
        res.setDoctorName("Dr. " + appt.getDoctor().getFirstName() + " " + appt.getDoctor().getLastName());
        res.setDepartmentName(appt.getDoctor().getDepartment() != null ? appt.getDoctor().getDepartment().getName() : "General");
        res.setAppointmentDate(appt.getAppointmentDate());
        res.setStartTime(appt.getStartTime());
        res.setEndTime(appt.getEndTime());
        res.setStatus(appt.getStatus());
        res.setConsultationFee(appt.getConsultationFee());
        res.setReasonForVisit(appt.getReasonForVisit());
        res.setCancellationReason(appt.getCancellationReason());
        res.setCreatedAt(appt.getCreatedAt());
        return res;
    }
}