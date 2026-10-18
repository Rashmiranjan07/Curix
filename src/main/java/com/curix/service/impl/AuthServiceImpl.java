package com.curix.service.impl;

import com.curix.dto.request.LoginRequest;
import com.curix.dto.request.RegisterPatientRequest;
import com.curix.dto.response.AuthResponse;
import com.curix.entity.PatientProfile;
import com.curix.entity.User;
import com.curix.enums.Role;
import com.curix.repository.PatientProfileRepository;
import com.curix.repository.UserRepository;
import com.curix.security.JwtTokenProvider;
import com.curix.service.AuthService;
import com.curix.exception.ConflictException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PatientProfileRepository patientProfileRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public String registerPatient(RegisterPatientRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered in the Curix system");
        }

        // 1. Create Base User entity
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.PATIENT);
        user.setActive(true);
        user.setVerified(false);

        User savedUser = userRepository.save(user);

        // 2. Generate Patient Identifier (e.g., PAT-8B41D9A2)
        String patientNumber = "PAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. Create Patient Demographics Profile
        PatientProfile profile = new PatientProfile();
        profile.setUser(savedUser);
        profile.setPatientNumber(patientNumber);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setPhone(request.getPhone());
        profile.setBloodGroup(request.getBloodGroup());
        profile.setEmergencyContact(request.getEmergencyContact());

        patientProfileRepository.save(profile);

        return "Patient registered successfully. Assigned Identifier: " + patientNumber;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}