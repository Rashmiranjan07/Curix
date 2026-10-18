package com.curix.controller;

import com.curix.dto.request.LoginRequest;
import com.curix.dto.request.RegisterPatientRequest;
import com.curix.dto.response.ApiResponse;
import com.curix.dto.response.AuthResponse;
import com.curix.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerPatient(@Valid @RequestBody RegisterPatientRequest request) {
        String result = authService.registerPatient(request);
        return new ResponseEntity<>(ApiResponse.success(result, null), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }
}