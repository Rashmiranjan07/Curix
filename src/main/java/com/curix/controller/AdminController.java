package com.curix.controller;

import com.curix.dto.request.CreateDoctorRequest;
import com.curix.dto.response.ApiResponse;
import com.curix.dto.response.DoctorResponse;
import com.curix.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DoctorService doctorService;

    public AdminController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping("/doctors")
    public ResponseEntity<ApiResponse<DoctorResponse>> provisionDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        DoctorResponse response = doctorService.provisionDoctor(request);
        return new ResponseEntity<>(ApiResponse.success("Doctor account provisioned successfully", response), HttpStatus.CREATED);
    }
}