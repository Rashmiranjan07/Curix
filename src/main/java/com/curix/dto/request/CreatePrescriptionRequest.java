package com.curix.dto.request;

import com.curix.dto.PrescriptionItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreatePrescriptionRequest {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotBlank(message = "Diagnosis is required")
    @Size(max = 255, message = "Diagnosis cannot exceed 255 characters")
    private String diagnosis;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    @NotEmpty(message = "Prescription must contain at least one medication item")
    @Valid
    private List<PrescriptionItemDto> items;

    public CreatePrescriptionRequest() {}

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<PrescriptionItemDto> getItems() { return items; }
    public void setItems(List<PrescriptionItemDto> items) { this.items = items; }
}