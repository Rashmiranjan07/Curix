package com.curix.controller;

import com.curix.dto.request.CreateDoctorRequest;
import com.curix.dto.response.ApiResponse;
import com.curix.dto.response.AppointmentResponse;
import com.curix.dto.response.AuditLogResponse;
import com.curix.dto.response.DashboardMetricsResponse;
import com.curix.dto.response.DoctorInsightDto;
import com.curix.dto.response.DoctorResponse;
import com.curix.dto.response.PrescriptionResponse;
import com.curix.service.AnalyticsService;
import com.curix.service.AppointmentService;
import com.curix.service.AuditService;
import com.curix.service.DoctorService;
import com.curix.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DoctorService doctorService;
    private final AnalyticsService analyticsService;
    private final AuditService auditService;
    private final AppointmentService appointmentService;
    private final PrescriptionService prescriptionService;

    public AdminController(DoctorService doctorService,
                           AnalyticsService analyticsService,
                           AuditService auditService,
                           AppointmentService appointmentService,
                           PrescriptionService prescriptionService) {
        this.doctorService = doctorService;
        this.analyticsService = analyticsService;
        this.auditService = auditService;
        this.appointmentService = appointmentService;
        this.prescriptionService = prescriptionService;
    }

    @PostMapping("/doctors")
    public ResponseEntity<ApiResponse<DoctorResponse>> provisionDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        DoctorResponse response = doctorService.provisionDoctor(request);
        auditService.logAction(null, "ADMIN", "DOCTOR_PROVISIONED", "DoctorProfile", response.getId(), "Provisioned doctor " + response.getEmail(), null);
        return new ResponseEntity<>(ApiResponse.success("Doctor account provisioned successfully", response), HttpStatus.CREATED);
    }
    
    @PutMapping("/doctors/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable Long id,
            @RequestBody CreateDoctorRequest request) {
        DoctorResponse updated = doctorService.updateDoctor(id, request);
        auditService.logAction(null, "ADMIN", "DOCTOR_UPDATED", "DoctorProfile", id, "Updated doctor profile for " + updated.getEmail(), null);
        return ResponseEntity.ok(ApiResponse.success("Doctor profile updated successfully", updated));
    }

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardMetrics() {
        DashboardMetricsResponse metrics = analyticsService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success("Analytics dashboard metrics retrieved successfully", metrics));
    }

    @GetMapping("/doctor-insights")
    public ResponseEntity<ApiResponse<List<DoctorInsightDto>>> getDoctorInsights() {
        List<DoctorInsightDto> list = analyticsService.getDoctorClinicalInsights();
        return ResponseEntity.ok(ApiResponse.success("Doctor clinical insights and prescription metrics retrieved successfully", list));
    }

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAllAppointments() {
        List<AppointmentResponse> list = appointmentService.getAllAppointments();
        return ResponseEntity.ok(ApiResponse.success("All hospital appointments retrieved successfully", list));
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getAllPrescriptions() {
        List<PrescriptionResponse> list = prescriptionService.getAllPrescriptions();
        return ResponseEntity.ok(ApiResponse.success("All clinical prescriptions retrieved successfully", list));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(@PageableDefault(size = 20) Pageable pageable) {
        Page<AuditLogResponse> logs = auditService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
    }
}
