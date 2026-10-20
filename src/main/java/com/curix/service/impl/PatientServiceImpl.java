package com.curix.service.impl;

import com.curix.dto.request.UpdatePatientProfileRequest;
import com.curix.dto.response.PatientProfileResponse;
import com.curix.entity.PatientProfile;
import com.curix.exception.ResourceNotFoundException;
import com.curix.repository.PatientProfileRepository;
import com.curix.service.PatientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientProfileRepository patientProfileRepository;

    public PatientServiceImpl(PatientProfileRepository patientProfileRepository) {
        this.patientProfileRepository = patientProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getPatientByUserId(Long userId) {
        PatientProfile profile = patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for User ID: " + userId));
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public PatientProfileResponse updatePatientProfile(Long userId, UpdatePatientProfileRequest request) {
        PatientProfile profile = patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for User ID: " + userId));

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        if (request.getBloodGroup() != null) {
            profile.setBloodGroup(request.getBloodGroup());
        }
        if (request.getEmergencyContact() != null) {
            profile.setEmergencyContact(request.getEmergencyContact());
        }

        PatientProfile updated = patientProfileRepository.save(profile);
        return mapToResponse(updated);
    }

    private PatientProfileResponse mapToResponse(PatientProfile profile) {
        PatientProfileResponse res = new PatientProfileResponse();
        res.setId(profile.getId());
        res.setUserId(profile.getUser().getId());
        res.setEmail(profile.getUser().getEmail());
        res.setPatientNumber(profile.getPatientNumber());
        res.setFirstName(profile.getFirstName());
        res.setLastName(profile.getLastName());
        res.setDateOfBirth(profile.getDateOfBirth());
        res.setGender(profile.getGender());
        res.setPhone(profile.getPhone());
        res.setBloodGroup(profile.getBloodGroup());
        res.setEmergencyContact(profile.getEmergencyContact());
        return res;
    }
}