package com.curix.service;

import com.curix.dto.request.UpdatePatientProfileRequest;
import com.curix.dto.response.PatientProfileResponse;

public interface PatientService {
    PatientProfileResponse getPatientByUserId(Long userId);
    PatientProfileResponse updatePatientProfile(Long userId, UpdatePatientProfileRequest request);
}