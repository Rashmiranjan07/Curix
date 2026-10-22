package com.curix.controller;

import com.curix.dto.request.CreatePrescriptionRequest;
import com.curix.dto.response.ApiResponse;
import com.curix.dto.response.PrescriptionResponse;
import com.curix.security.UserPrincipal;
import com.curix.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> createPrescription(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreatePrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.createPrescription(currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Prescription generated successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescriptionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PrescriptionResponse response = prescriptionService.getPrescriptionById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Prescription details retrieved", response));
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescriptionByAppointment(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PrescriptionResponse response = prescriptionService.getPrescriptionByAppointmentId(appointmentId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Prescription retrieved for appointment", response));
    }

    @GetMapping({"/my-prescriptions", "/patient"})
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getMyPatientPrescriptions(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<PrescriptionResponse> list = prescriptionService.getMyPrescriptionsAsPatient(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Patient prescriptions retrieved", list));
    }

    @GetMapping("/doctor-history")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getMyDoctorPrescriptions(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<PrescriptionResponse> list = prescriptionService.getMyPrescriptionsAsDoctor(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Doctor clinical prescriptions retrieved", list));
    }
}