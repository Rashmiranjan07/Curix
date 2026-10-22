package com.curix.controller;

import com.curix.dto.request.BookAppointmentRequest;
import com.curix.dto.request.CancelAppointmentRequest;
import com.curix.dto.response.ApiResponse;
import com.curix.dto.response.AppointmentResponse;
import com.curix.dto.response.AvailableSlotDto;
import com.curix.security.UserPrincipal;
import com.curix.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // 1. Available slots calculation
    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<List<AvailableSlotDto>>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AvailableSlotDto> slots = appointmentService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(ApiResponse.success("Available slots calculated", slots));
    }

    // 2. Book an appointment
    @PostMapping("/book")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody BookAppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Appointment booked successfully", response), HttpStatus.CREATED);
    }

    // 3. Patient appointments list (Supports both /my-appointments and /my-bookings)
    @GetMapping({"/my-appointments", "/my-bookings"})
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyPatientBookings(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AppointmentResponse> list = appointmentService.getAppointmentsForPatient(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Patient appointments retrieved", list));
    }

    // 4. Doctor consultation schedule (Supports both /doctor/my-schedule and /doctor/schedule)
    @GetMapping({"/doctor/my-schedule", "/doctor/schedule"})
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getDoctorSchedule(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AppointmentResponse> list = appointmentService.getAppointmentsForDoctor(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Doctor appointments retrieved", list));
    }

    // 5. Cancel appointment
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody(required = false) CancelAppointmentRequest request) {
        CancelAppointmentRequest cancelRequest = (request != null) ? request : new CancelAppointmentRequest();
        AppointmentResponse response = appointmentService.cancelAppointment(id, currentUser.getId(), cancelRequest);
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully", response));
    }

    // 6. Complete appointment
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> completeAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AppointmentResponse response = appointmentService.completeAppointment(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Appointment marked as completed", response));
    }

    // 7. Get single appointment by ID (Placed at the end)
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AppointmentResponse response = appointmentService.getAppointmentById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Appointment details retrieved", response));
    }
}