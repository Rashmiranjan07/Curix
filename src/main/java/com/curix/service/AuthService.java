package com.curix.service;

import com.curix.dto.request.LoginRequest;
import com.curix.dto.request.RegisterPatientRequest;
import com.curix.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    String registerPatient(RegisterPatientRequest request);
}