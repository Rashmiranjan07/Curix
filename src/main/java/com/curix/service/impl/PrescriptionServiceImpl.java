package com.curix.service.impl;

import com.curix.dto.PrescriptionItemDto;
import com.curix.dto.request.CreatePrescriptionRequest;
import com.curix.dto.response.PrescriptionResponse;
import com.curix.entity.Appointment;
import com.curix.entity.PatientProfile;
import com.curix.entity.Prescription;
import com.curix.entity.PrescriptionItem;
import com.curix.entity.User;
import com.curix.enums.AppointmentStatus;
import com.curix.enums.Role;
import com.curix.exception.ConflictException;
import com.curix.exception.ResourceNotFoundException;
import com.curix.repository.AppointmentRepository;
import com.curix.repository.DoctorProfileRepository;
import com.curix.repository.PatientProfileRepository;
import com.curix.repository.PrescriptionRepository;
import com.curix.repository.UserRepository;
import com.curix.service.EmailService;
import com.curix.service.PrescriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionServiceImpl.class);

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   AppointmentRepository appointmentRepository,
                                   PatientProfileRepository patientProfileRepository,
                                   DoctorProfileRepository doctorProfileRepository,
                                   UserRepository userRepository,
                                   EmailService emailService) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(Long doctorUserId, CreatePrescriptionRequest request) {
        // 1. Validate Appointment exists
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));

        // 2. Ownership Guard: Ensure the logged-in doctor is the doctor on this appointment (or an admin)
        User requestingUser = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + doctorUserId));

        boolean isAssignedDoctor = appointment.getDoctor() != null
                && appointment.getDoctor().getUser() != null
                && appointment.getDoctor().getUser().getId().equals(doctorUserId);
        boolean isAdmin = (requestingUser.getRole() == Role.ADMIN);

        if (!isAssignedDoctor && !isAdmin) {
            String assignedDoctorName = (appointment.getDoctor() != null)
                    ? "Dr. " + appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName()
                    : "the assigned physician";
            throw new AccessDeniedException("Access Denied: Only the assigned consulting doctor (" + assignedDoctorName + ") or an administrator can issue this prescription.");
        }

        // 3. Status Guard: Cannot write prescriptions for cancelled visits
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Cannot issue a prescription for a cancelled appointment");
        }

        // 4. One-to-One Guard: An appointment cannot have duplicate prescriptions
        if (prescriptionRepository.existsByAppointmentId(appointment.getId())) {
            throw new ConflictException("A prescription has already been issued for this appointment");
        }

        // 5. Construct Parent Prescription Entity
        Prescription prescription = new Prescription();
        prescription.setAppointment(appointment);
        prescription.setPatient(appointment.getPatient());
        prescription.setDoctor(appointment.getDoctor());
        prescription.setDiagnosis(request.getDiagnosis().trim());
        prescription.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        // 6. Map Child Line Items
        for (PrescriptionItemDto itemDto : request.getItems()) {
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicationName(itemDto.getMedicationName().trim());
            item.setDosage(itemDto.getDosage().trim());
            item.setFrequency(itemDto.getFrequency().trim());
            item.setDuration(itemDto.getDuration().trim());
            item.setInstructions(itemDto.getInstructions() != null ? itemDto.getInstructions().trim() : null);

            prescription.addItem(item); // Bi-directional helper links foreign key
        }

        // 7. Auto-transition appointment to COMPLETED upon prescription creation
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(appointment);
        }

        Prescription saved = prescriptionRepository.saveAndFlush(prescription);
        PrescriptionResponse response = mapToResponse(saved);
        if (response.getCreatedAt() == null) {
            response.setCreatedAt(LocalDateTime.now());
        }

        // 8. Resolve patient registered email (identical pattern to cancelAppointment)
        try {
            String patientEmail = (appointment.getPatient() != null && appointment.getPatient().getUser() != null)
                    ? appointment.getPatient().getUser().getEmail() : null;

            // Direct repository lookup fallback if proxy was uninitialized
            if (patientEmail == null && appointment.getPatient() != null) {
                Long patientProfileId = appointment.getPatient().getId();
                PatientProfile pProfile = patientProfileRepository.findById(patientProfileId).orElse(null);
                if (pProfile != null && pProfile.getUser() != null) {
                    User u = userRepository.findById(pProfile.getUser().getId()).orElse(null);
                    if (u != null && u.getEmail() != null) {
                        patientEmail = u.getEmail().trim();
                    }
                }
            }

            if (patientEmail != null && !patientEmail.trim().isEmpty()) {
                log.info("[Curix Prescription] Dispatching 'Your Prescription is Ready' email to patient registered email: {} for Rx #{}", patientEmail.trim(), saved.getId());
                emailService.sendPrescriptionEmail(response, patientEmail.trim());
            } else {
                log.warn("[Curix Prescription] WARNING: Could not determine patient email for appointment #{}. Prescription saved, email skipped.", appointment.getId());
            }
        } catch (Exception emailEx) {
            log.error("[Curix Prescription] Error delivering prescription email: {}", emailEx.getMessage(), emailEx);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long prescriptionId, Long requestingUserId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + prescriptionId));

        validateAccess(prescription, requestingUserId);
        return mapToResponse(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionByAppointmentId(Long appointmentId, Long requestingUserId) {
        Prescription prescription = prescriptionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("No prescription found for appointment ID: " + appointmentId));

        validateAccess(prescription, requestingUserId);
        return mapToResponse(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getMyPrescriptionsAsPatient(Long patientUserId) {
        PatientProfile patient = patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + patientUserId));

        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getAllPrescriptions() {
        return prescriptionRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getMyPrescriptionsAsDoctor(Long doctorUserId) {
        User user = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + doctorUserId));

        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access Denied: Only clinical personnel can access this resource");
        }

        if (user.getRole() == Role.ADMIN) {
            return getAllPrescriptions();
        }

        return prescriptionRepository.findAll().stream()
                .filter(p -> p.getDoctor().getUser().getId().equals(doctorUserId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateAccess(Prescription prescription, Long requestingUserId) {
        User user = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + requestingUserId));

        boolean isPatientOwner = prescription.getPatient().getUser().getId().equals(requestingUserId);
        boolean isDoctorOwner = prescription.getDoctor().getUser().getId().equals(requestingUserId);
        boolean isAdmin = (user.getRole() == Role.ADMIN);

        if (!isPatientOwner && !isDoctorOwner && !isAdmin) {
            throw new AccessDeniedException("Access Denied: You are not authorized to view this prescription");
        }
    }

    private PrescriptionResponse mapToResponse(Prescription p) {
        PrescriptionResponse res = new PrescriptionResponse();
        res.setId(p.getId());
        res.setAppointmentId(p.getAppointment().getId());
        res.setAppointmentDate(p.getAppointment().getAppointmentDate());
        res.setPatientId(p.getPatient().getId());
        res.setPatientName(p.getPatient().getFirstName() + " " + p.getPatient().getLastName());
        res.setPatientNumber(p.getPatient().getPatientNumber());
        res.setDoctorId(p.getDoctor().getId());
        res.setDoctorName("Dr. " + p.getDoctor().getFirstName() + " " + p.getDoctor().getLastName());
        res.setDepartmentName(p.getDoctor().getDepartment() != null ? p.getDoctor().getDepartment().getName() : "General");
        res.setDiagnosis(p.getDiagnosis());
        res.setNotes(p.getNotes());
        res.setCreatedAt(p.getCreatedAt());

        if (p.getItems() != null) {
            List<PrescriptionItemDto> itemDtos = p.getItems().stream()
                    .map(i -> new PrescriptionItemDto(
                            i.getId(),
                            i.getMedicationName(),
                            i.getDosage(),
                            i.getFrequency(),
                            i.getDuration(),
                            i.getInstructions()
                    ))
                    .collect(Collectors.toList());
            res.setItems(itemDtos);
        }
        return res;
    }
}
