package com.curix.service;

import com.curix.dto.request.CreateDoctorRequest;
import com.curix.dto.response.DoctorResponse;

import java.util.List;

public interface DoctorService {
    DoctorResponse provisionDoctor(CreateDoctorRequest request);
    List<DoctorResponse> searchDoctors(Long departmentId, String specialty);
    DoctorResponse getDoctorById(Long doctorId);
    DoctorResponse getDoctorByUserId(Long userId);
}