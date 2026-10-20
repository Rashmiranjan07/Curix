package com.curix.controller;

import com.curix.dto.request.CreateDoctorRequest;
import com.curix.dto.response.ApiResponse;
import com.curix.dto.response.AuditLogResponse;
import com.curix.dto.response.DashboardMetricsResponse;
import com.curix.dto.response.DoctorResponse;
import com.curix.service.AnalyticsService;
import com.curix.service.AuditService;
import com.curix.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DoctorService doctorService;
    private final AnalyticsService analyticsService;
    private final AuditService auditService;

    public AdminController(DoctorService doctorService,
                           AnalyticsService analyticsService,
                           AuditService auditService) {
        this.doctorService = doctorService;
        this.analyticsService = analyticsService;
        this.auditService = auditService;
    }

    @PostMapping("/doctors")
    public ResponseEntity<ApiResponse<DoctorResponse>> provisionDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        DoctorResponse response = doctorService.provisionDoctor(request);
        auditService.logAction(null, "ADMIN", "DOCTOR_PROVISIONED", "DoctorProfile", response.getId(), "Provisioned doctor " + response.getEmail(), null);
        return new ResponseEntity<>(ApiResponse.success("Doctor account provisioned successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardMetrics() {
        DashboardMetricsResponse metrics = analyticsService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success("Analytics dashboard metrics retrieved successfully", metrics));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(@PageableDefault(size = 20) Pageable pageable) {
        Page<AuditLogResponse> logs = auditService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
    }
}