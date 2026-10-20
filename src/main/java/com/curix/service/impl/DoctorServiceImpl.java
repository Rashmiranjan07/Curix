package com.curix.service.impl;

import com.curix.dto.DoctorAvailabilityDto;
import com.curix.dto.request.CreateDoctorRequest;
import com.curix.dto.response.DoctorResponse;
import com.curix.entity.Department;
import com.curix.entity.DoctorAvailability;
import com.curix.entity.DoctorProfile;
import com.curix.entity.User;
import com.curix.enums.Role;
import com.curix.exception.ConflictException;
import com.curix.exception.ResourceNotFoundException;
import com.curix.repository.DepartmentRepository;
import com.curix.repository.DoctorProfileRepository;
import com.curix.repository.UserRepository;
import com.curix.service.DoctorService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorServiceImpl(UserRepository userRepository,
                             DoctorProfileRepository doctorProfileRepository,
                             DepartmentRepository departmentRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public DoctorResponse provisionDoctor(CreateDoctorRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ConflictException("Email is already registered in the Curix system");
        }
        if (doctorProfileRepository.existsByLicenseNumber(request.getLicenseNumber().trim())) {
            throw new ConflictException("License number is already assigned to an existing doctor");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        // 1. Create Base User with Role DOCTOR
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.DOCTOR)
                .active(true)
                .verified(true)
                .build();
        User savedUser = userRepository.save(user);

        // 2. Build Doctor Profile
        DoctorProfile doctor = DoctorProfile.builder()
                .user(savedUser)
                .department(department)
                .licenseNumber(request.getLicenseNumber().trim())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .specialty(request.getSpecialty())
                .qualification(request.getQualification())
                .experienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0)
                .consultationFee(request.getConsultationFee())
                .availabilities(new ArrayList<>())
                .build();

        // 3. Attach Availabilities
        if (request.getAvailabilities() != null && !request.getAvailabilities().isEmpty()) {
            for (DoctorAvailabilityDto availDto : request.getAvailabilities()) {
                DoctorAvailability availability = DoctorAvailability.builder()
                        .doctor(doctor)
                        .dayOfWeek(availDto.getDayOfWeek())
                        .startTime(availDto.getStartTime())
                        .endTime(availDto.getEndTime())
                        .slotMinutes(availDto.getSlotMinutes() != null ? availDto.getSlotMinutes() : 30)
                        .active(true)
                        .build();
                doctor.getAvailabilities().add(availability);
            }
        }

        DoctorProfile savedDoctor = doctorProfileRepository.save(doctor);
        return mapToResponse(savedDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> searchDoctors(Long departmentId, String specialty) {
        return doctorProfileRepository.searchDoctors(departmentId, specialty)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));
        return mapToResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByUserId(Long userId) {
        DoctorProfile doctor = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for User ID: " + userId));
        return mapToResponse(doctor);
    }

    private DoctorResponse mapToResponse(DoctorProfile doctor) {
        DoctorResponse res = new DoctorResponse();
        res.setId(doctor.getId());
        res.setUserId(doctor.getUser().getId());
        res.setEmail(doctor.getUser().getEmail());
        res.setFirstName(doctor.getFirstName());
        res.setLastName(doctor.getLastName());
        res.setLicenseNumber(doctor.getLicenseNumber());
        res.setDepartmentId(doctor.getDepartment() != null ? doctor.getDepartment().getId() : null);
        res.setDepartmentName(doctor.getDepartment() != null ? doctor.getDepartment().getName() : null);
        res.setSpecialty(doctor.getSpecialty());
        res.setQualification(doctor.getQualification());
        res.setExperienceYears(doctor.getExperienceYears());
        res.setConsultationFee(doctor.getConsultationFee());
        res.setActive(doctor.getUser().isActive());

        if (doctor.getAvailabilities() != null) {
            List<DoctorAvailabilityDto> availDtos = doctor.getAvailabilities().stream()
                    .map(a -> new DoctorAvailabilityDto(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), a.getSlotMinutes(), a.isActive()))
                    .collect(Collectors.toList());
            res.setAvailabilities(availDtos);
        }
        return res;
    }
}