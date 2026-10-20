package com.curix.controller;

import com.curix.dto.request.UpdatePatientProfileRequest;
import com.curix.dto.response.ApiResponse;
import com.curix.dto.response.PatientProfileResponse;
import com.curix.security.UserPrincipal;
import com.curix.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patient")
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getMyProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        PatientProfileResponse profile = patientService.getPatientByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Patient profile retrieved", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdatePatientProfileRequest request) {
        PatientProfileResponse profile = patientService.updatePatientProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Patient profile updated successfully", profile));
    }
}