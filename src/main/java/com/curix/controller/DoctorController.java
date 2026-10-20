package com.curix.controller;

import com.curix.dto.response.ApiResponse;
import com.curix.dto.response.DoctorResponse;
import com.curix.security.UserPrincipal;
import com.curix.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> searchDoctors(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String specialty) {
        List<DoctorResponse> list = doctorService.searchDoctors(departmentId, specialty);
        return ResponseEntity.ok(ApiResponse.success("Doctors retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long id) {
        DoctorResponse doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor details retrieved", doctor));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorResponse>> getCurrentDoctor(@AuthenticationPrincipal UserPrincipal currentUser) {
        DoctorResponse doctor = doctorService.getDoctorByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Doctor profile retrieved", doctor));
    }
}