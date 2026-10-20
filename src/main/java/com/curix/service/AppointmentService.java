package com.curix.service;

import com.curix.dto.request.BookAppointmentRequest;
import com.curix.dto.request.CancelAppointmentRequest;
import com.curix.dto.response.AppointmentResponse;
import com.curix.dto.response.AvailableSlotDto;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    List<AvailableSlotDto> getAvailableSlots(Long doctorId, LocalDate date);

    AppointmentResponse bookAppointment(Long patientUserId, BookAppointmentRequest request);

    AppointmentResponse cancelAppointment(Long appointmentId, Long requestingUserId, CancelAppointmentRequest request);

    AppointmentResponse completeAppointment(Long appointmentId, Long doctorUserId);

    List<AppointmentResponse> getAppointmentsForPatient(Long patientUserId);

    List<AppointmentResponse> getAppointmentsForDoctor(Long doctorUserId);

    AppointmentResponse getAppointmentById(Long appointmentId, Long requestingUserId);
}