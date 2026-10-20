package com.curix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelAppointmentRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public CancelAppointmentRequest() {}

    public CancelAppointmentRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}