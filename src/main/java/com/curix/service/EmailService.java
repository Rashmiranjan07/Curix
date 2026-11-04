package com.curix.service;

import com.curix.dto.response.AppointmentResponse;

public interface EmailService {

    /**
     * Asynchronously sends an automated HTML appointment confirmation email to the patient
     * with the doctor's name, department, date, time slot, fee, and hospital guidance.
     *
     * @param appointment the confirmed appointment response DTO
     * @param patientEmail the recipient patient's registered email address
     */
    void sendAppointmentConfirmationEmail(AppointmentResponse appointment, String patientEmail);

    /**
     * Asynchronously sends an appointment cancellation notice to the patient.
     *
     * @param appointment the cancelled appointment response DTO
     * @param patientEmail the recipient patient's registered email address
     * @param reason the reason for cancellation
     */
    void sendAppointmentCancellationEmail(AppointmentResponse appointment, String patientEmail, String reason);
}
