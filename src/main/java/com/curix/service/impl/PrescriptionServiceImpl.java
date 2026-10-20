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
import com.curix.service.PrescriptionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   AppointmentRepository appointmentRepository,
                                   PatientProfileRepository patientProfileRepository,
                                   DoctorProfileRepository doctorProfileRepository,
                                   UserRepository userRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(Long doctorUserId, CreatePrescriptionRequest request) {
        // 1. Validate Appointment exists
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));

        // 2. Ownership Guard: Ensure the logged-in doctor is the doctor on this appointment
        if (!appointment.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new AccessDeniedException("Access Denied: Only the assigned consulting doctor can issue this prescription");
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

        Prescription saved = prescriptionRepository.save(prescription);
        return mapToResponse(saved);
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
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for User ID: " + patientUserId));

        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getMyPrescriptionsAsDoctor(Long doctorUserId) {
        User user = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + doctorUserId));

        if (user.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Access Denied: Only doctors can access this resource");
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