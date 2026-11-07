package com.curix.service.impl;

import com.curix.dto.PrescriptionItemDto;
import com.curix.dto.response.AppointmentResponse;
import com.curix.dto.response.PrescriptionResponse;
import com.curix.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${curix.mail.enabled:true}")
    private boolean mailEnabled;

    @Async
    @Override
    public void sendAppointmentConfirmationEmail(AppointmentResponse appointment, String patientEmail) {
        if (!mailEnabled) {
            log.info("[Curix Email] Automated email notifications are currently disabled via curix.mail.enabled.");
            return;
        }

        if (patientEmail == null || patientEmail.trim().isEmpty()) {
            log.warn("[Curix Email] Cannot send confirmation email: Patient email is missing for appointment #{}", 
                    appointment != null ? appointment.getId() : "N/A");
            return;
        }

        if (appointment == null) {
            log.warn("[Curix Email] Appointment payload is null. Email aborted.");
            return;
        }

        // Check whether real SMTP credentials have been entered by user or if using dummy defaults
        if (mailSender == null || isPlaceholderCredential(mailUsername, mailPassword)) {
            logSimulatedEmail(appointment, patientEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String senderAddress = (mailUsername != null && !mailUsername.trim().isEmpty()) 
                    ? mailUsername 
                    : "noreply@curixhealthcare.com";

            helper.setFrom(senderAddress, "Curix Healthcare");
            helper.setTo(patientEmail.trim());
            helper.setSubject("Appointment Confirmed: " + appointment.getDoctorName() + " on " + appointment.getAppointmentDate());

            String htmlBody = buildConfirmationHtmlBody(appointment);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[Curix Email] SUCCESS: Appointment confirmation email sent to {} for booking #{}", 
                    patientEmail, appointment.getId());
        } catch (Exception ex) {
            // CRITICAL ARCHITECTURAL SAFETY:
            // Under NO circumstance must an email transport failure crash or roll back the patient's booking!
            log.error("[Curix Email] WARNING: Could not deliver email to {}: {}. Note: The appointment remains safely booked in database.", 
                    patientEmail, ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendAppointmentCancellationEmail(AppointmentResponse appointment, String patientEmail, String reason) {
        if (!mailEnabled || patientEmail == null || patientEmail.trim().isEmpty() || appointment == null) {
            return;
        }

        if (mailSender == null || isPlaceholderCredential(mailUsername, mailPassword)) {
            log.info("================================================================================");
            log.info("[CURIX APPOINTMENT CANCELLATION NOTICE - EMAIL PREVIEW]");
            log.info("To Patient: {}", patientEmail);
            log.info("Subject: Appointment Cancelled - Consultation #{}", appointment.getId());
            log.info("Doctor: {}", appointment.getDoctorName());
            log.info("Date & Time: {} from {} to {}", appointment.getAppointmentDate(), appointment.getStartTime(), appointment.getEndTime());
            log.info("Cancellation Reason: {}", reason != null ? reason : "Requested by patient/clinic");
            log.info("================================================================================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String senderAddress = (mailUsername != null && !mailUsername.trim().isEmpty()) 
                    ? mailUsername 
                    : "noreply@curixhealthcare.com";

            helper.setFrom(senderAddress, "Curix Healthcare");
            helper.setTo(patientEmail.trim());
            helper.setSubject("Appointment Cancelled: Consultation #" + appointment.getId() + " - Curix Healthcare");

            String htmlBody = buildCancellationHtmlBody(appointment, reason);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[Curix Email] SUCCESS: Cancellation email sent to {} for appointment #{}", patientEmail, appointment.getId());
        } catch (Exception ex) {
            log.error("[Curix Email] Failed to send cancellation email to {}: {}", patientEmail, ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendPrescriptionEmail(PrescriptionResponse prescription, String patientEmail) {
        if (!mailEnabled) {
            log.info("[Curix Email] Automated email notifications are currently disabled via curix.mail.enabled.");
            return;
        }

        if (patientEmail == null || patientEmail.trim().isEmpty()) {
            log.warn("[Curix Email] Cannot send prescription email: Patient email is missing for prescription #{}", 
                    prescription != null ? prescription.getId() : "N/A");
            return;
        }

        if (prescription == null) {
            log.warn("[Curix Email] Prescription payload is null. Email aborted.");
            return;
        }

        if (mailSender == null || isPlaceholderCredential(mailUsername, mailPassword)) {
            logSimulatedPrescriptionEmail(prescription, patientEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String senderAddress = (mailUsername != null && !mailUsername.trim().isEmpty()) 
                    ? mailUsername 
                    : "noreply@curixhealthcare.com";

            helper.setFrom(senderAddress, "Curix Healthcare");
            helper.setTo(patientEmail.trim());
            helper.setSubject("Your Prescription is Ready - Curix Healthcare (Rx #" + prescription.getId() + ")");

            String htmlBody;
            try {
                htmlBody = buildPrescriptionHtmlBody(prescription);
            } catch (Exception htmlEx) {
                log.warn("[Curix Email] Stylized HTML body failed ({}), using fallback template", htmlEx.getMessage());
                htmlBody = buildFallbackPrescriptionHtmlBody(prescription);
            }

            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[Curix Email] SUCCESS: Prescription ready email sent to {} for Rx #{}", 
                    patientEmail, prescription.getId());
        } catch (Exception ex) {
            log.error("[Curix Email] WARNING: Could not deliver prescription email to {}: {}. Note: Prescription remains safely saved.", 
                    patientEmail, ex.getMessage(), ex);
        }
    }

    private boolean isPlaceholderCredential(String user, String pass) {
        if (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {
            return true;
        }
        String u = user.toLowerCase();
        String p = pass.toLowerCase();
        return u.contains("your-") || u.contains("example.com") || p.contains("your-") || p.contains("password");
    }

    private void logSimulatedEmail(AppointmentResponse appointment, String patientEmail) {
        log.info("================================================================================");
        log.info("[CURIX APPOINTMENT CONFIRMATION EMAIL - AUTOMATIC RECEIPT]");
        log.info("Recipient (Patient): {}", patientEmail);
        log.info("Subject: Appointment Confirmed with {} on {}", appointment.getDoctorName(), appointment.getAppointmentDate());
        log.info("Doctor: {} | Department: {}", appointment.getDoctorName(), appointment.getDepartmentName());
        log.info("Appointment Date: {}", appointment.getAppointmentDate());
        log.info("Appointment Time: {} - {}", appointment.getStartTime(), appointment.getEndTime());
        log.info("Consultation Fee: ${}", appointment.getConsultationFee());
        log.info("Reason for Visit: {}", appointment.getReasonForVisit());
        log.info("Booking Reference ID: #{}", appointment.getId());
        log.info("--------------------------------------------------------------------------------");
        log.info("NOTE: Real SMTP is ready! To deliver emails straight to patient inboxes,");
        log.info("simply set your Gmail and 16-char App Password in application.properties:");
        log.info("spring.mail.username=your-email@gmail.com");
        log.info("spring.mail.password=your-16-char-app-password");
        log.info("================================================================================");
    }

    private void logSimulatedPrescriptionEmail(PrescriptionResponse prescription, String patientEmail) {
        log.info("================================================================================");
        log.info("[CURIX DIGITAL PRESCRIPTION - EMAIL PREVIEW]");
        log.info("Recipient (Patient): {}", patientEmail);
        log.info("Subject: Official Digital Prescription - Rx #{}", prescription.getId());
        log.info("Doctor: {} | Department: {}", prescription.getDoctorName(), prescription.getDepartmentName());
        log.info("Diagnosis: {}", prescription.getDiagnosis());
        log.info("Prescribed Medications ({} items):", prescription.getItems() != null ? prescription.getItems().size() : 0);
        if (prescription.getItems() != null) {
            for (PrescriptionItemDto item : prescription.getItems()) {
                log.info("  - {} ({}) - {} for {} | {}", 
                        item.getMedicationName(), item.getDosage(), item.getFrequency(), item.getDuration(), item.getInstructions());
            }
        }
        log.info("Doctor Notes: {}", prescription.getNotes());
        log.info("================================================================================");
    }

    private String buildConfirmationHtmlBody(AppointmentResponse appt) {
        String patientName = (appt.getPatientName() != null && !appt.getPatientName().trim().isEmpty()) 
                ? appt.getPatientName() 
                : "Valued Patient";

        String dateFormatted = appt.getAppointmentDate() != null 
                ? appt.getAppointmentDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH))
                : "Scheduled Date";

        String startTime = appt.getStartTime() != null 
                ? appt.getStartTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)) 
                : "Confirmed Time";

        String endTime = appt.getEndTime() != null 
                ? appt.getEndTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)) 
                : "";

        String timeRange = !endTime.isEmpty() ? (startTime + " - " + endTime) : startTime;

        String fee = (appt.getConsultationFee() != null) 
                ? "$" + appt.getConsultationFee().toString() 
                : "Included";

        String reason = (appt.getReasonForVisit() != null && !appt.getReasonForVisit().trim().isEmpty()) 
                ? appt.getReasonForVisit() 
                : "General Outpatient Consultation";

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Appointment Confirmed - Curix Healthcare</title>
              <style>
                body { margin: 0; padding: 0; background-color: #F1F5F9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1E293B; }
                .email-container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #E2E8F0; }
                .email-header { background: linear-gradient(135deg, #0A1C35 0%%, #0f2c52 100%%); padding: 36px 30px; text-align: center; color: #ffffff; }
                .brand-badge { display: inline-block; background: rgba(0, 168, 150, 0.2); border: 1px solid #00A896; color: #0BC2A8; font-size: 11px; font-weight: 800; letter-spacing: 1.5px; text-transform: uppercase; padding: 5px 14px; border-radius: 9999px; margin-bottom: 12px; }
                .header-title { font-size: 24px; font-weight: 800; margin: 0 0 6px 0; color: #ffffff; letter-spacing: -0.5px; }
                .header-sub { font-size: 13px; color: #94A3B8; margin: 0; }
                .email-body { padding: 32px 30px; }
                .greeting { font-size: 16px; font-weight: 700; color: #0A1C35; margin-bottom: 8px; }
                .intro-text { font-size: 14px; color: #64748B; line-height: 1.6; margin-bottom: 24px; }
                .card { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 16px; padding: 22px; margin-bottom: 24px; }
                .card-title { font-size: 11px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; color: #00A896; margin-bottom: 14px; }
                .detail-row { display: flex; justify-content: space-between; align-items: flex-start; padding: 9px 0; border-bottom: 1px dashed #E2E8F0; }
                .detail-row:last-child { border-bottom: none; padding-bottom: 0; }
                .detail-label { font-size: 12px; font-weight: 600; color: #64748B; width: 38%%; }
                .detail-value { font-size: 13px; font-weight: 700; color: #0A1C35; width: 62%%; text-align: right; }
                .doctor-highlight { color: #00A896; font-size: 14px; }
                .guidelines { background: #E6F8F5; border-left: 4px solid #00A896; border-radius: 0 12px 12px 0; padding: 16px; margin-bottom: 24px; }
                .guidelines-title { font-size: 12px; font-weight: 800; color: #00A896; text-transform: uppercase; margin-bottom: 6px; letter-spacing: 0.5px; }
                .guidelines-text { font-size: 12px; color: #334155; line-height: 1.6; margin: 0; }
                .action-btn-wrap { text-align: center; margin: 28px 0 10px 0; }
                .action-btn { display: inline-block; background-color: #00A896; color: #ffffff !important; text-decoration: none; font-size: 13px; font-weight: 800; padding: 13px 32px; border-radius: 9999px; box-shadow: 0 4px 12px rgba(0, 168, 150, 0.3); }
                .email-footer { background: #071524; padding: 24px 30px; text-align: center; color: #64748B; font-size: 11px; line-height: 1.6; }
                .footer-brand { color: #ffffff; font-weight: 800; font-size: 13px; letter-spacing: 1px; margin-bottom: 4px; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <div class="brand-badge">CURIX HEALTHCARE - CONFIRMATION</div>
                  <h1 class="header-title">Appointment Confirmed!</h1>
                  <p class="header-sub">Your hospital outpatient consultation is officially scheduled</p>
                </div>

                <div class="email-body">
                  <div class="greeting">Hello %s,</div>
                  <p class="intro-text">
                    Thank you for booking with Curix Healthcare. Your consultation slot has been reserved in our system. Please review the appointment details below:
                  </p>

                  <div class="card">
                    <div class="card-title">Consultation Summary</div>
                    <table style="width: 100%%; border-collapse: collapse;">
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 9px 0; font-size: 12px; font-weight: 600; color: #64748B;">Consulting Doctor</td>
                        <td style="padding: 9px 0; font-size: 14px; font-weight: 800; color: #00A896; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 9px 0; font-size: 12px; font-weight: 600; color: #64748B;">Department / Clinical Unit</td>
                        <td style="padding: 9px 0; font-size: 13px; font-weight: 700; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 9px 0; font-size: 12px; font-weight: 600; color: #64748B;">Appointment Date</td>
                        <td style="padding: 9px 0; font-size: 13px; font-weight: 800; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 9px 0; font-size: 12px; font-weight: 600; color: #64748B;">Scheduled Time Slot</td>
                        <td style="padding: 9px 0; font-size: 13px; font-weight: 800; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 9px 0; font-size: 12px; font-weight: 600; color: #64748B;">Reason for Visit</td>
                        <td style="padding: 9px 0; font-size: 13px; font-weight: 600; color: #334155; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 9px 0; font-size: 12px; font-weight: 600; color: #64748B;">Consultation Fee</td>
                        <td style="padding: 9px 0; font-size: 13px; font-weight: 800; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 9px 0; font-size: 12px; font-weight: 600; color: #64748B;">Booking Reference</td>
                        <td style="padding: 9px 0; font-size: 12px; font-weight: 700; color: #64748B; text-align: right;">#%s</td>
                      </tr>
                    </table>
                  </div>

                  <div class="guidelines">
                    <div class="guidelines-title">Important Visit Guidelines</div>
                    <p class="guidelines-text">
                      &bull; Please arrive <strong>10 minutes prior</strong> to your consultation time for initial vitals screening.<br>
                      &bull; Carry any past medical records, prescription slips, or laboratory reports.<br>
                      &bull; You can review or reschedule this booking anytime directly from your Curix Patient Dashboard.
                    </p>
                  </div>

                  <p style="font-size: 12px; color: #94A3B8; text-align: center; margin: 0;">
                    Need help or need to reschedule? Visit your portal or contact our 24/7 helpline at <strong>+1 (800) 287-4900</strong>.
                  </p>
                </div>

                <div class="email-footer">
                  <div class="footer-brand">CURIX HEALTHCARE SYSTEMS</div>
                  <p style="margin: 4px 0 0 0;">HIPAA-Aligned & Encrypted Patient Health Network</p>
                  <p style="margin: 4px 0 0 0; color: #475569;">This is an automated clinical notification. Please do not reply directly to this email.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                patientName,
                appt.getDoctorName() != null ? appt.getDoctorName() : "Attending Physician",
                appt.getDepartmentName() != null ? appt.getDepartmentName() : "Specialist Clinic",
                dateFormatted,
                timeRange,
                reason,
                fee,
                appt.getId() != null ? appt.getId().toString() : "N/A"
            );
    }

    private String buildCancellationHtmlBody(AppointmentResponse appt, String reason) {
        String patientName = (appt.getPatientName() != null && !appt.getPatientName().trim().isEmpty()) 
                ? appt.getPatientName() 
                : "Valued Patient";

        String reasonDisplay = (reason != null && !reason.trim().isEmpty()) ? reason : "Requested by patient/clinic";

        String dateFormatted = appt.getAppointmentDate() != null 
                ? appt.getAppointmentDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH))
                : "Scheduled Date";

        String startTime = appt.getStartTime() != null 
                ? appt.getStartTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)) 
                : "Scheduled Time";

        String endTime = appt.getEndTime() != null 
                ? appt.getEndTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)) 
                : "";

        String timeRange = !endTime.isEmpty() ? (startTime + " - " + endTime) : startTime;

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Appointment Cancelled - Curix Healthcare</title>
              <style>
                body { margin: 0; padding: 0; background-color: #F1F5F9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1E293B; }
                .email-container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #E2E8F0; }
                .email-header { background: linear-gradient(135deg, #0A1C35 0%%, #1E293B 100%%); padding: 36px 30px; text-align: center; color: #ffffff; }
                .brand-badge { display: inline-block; background: rgba(239, 68, 68, 0.2); border: 1px solid #EF4444; color: #FCA5A5; font-size: 11px; font-weight: 800; letter-spacing: 1.5px; text-transform: uppercase; padding: 5px 14px; border-radius: 9999px; margin-bottom: 12px; }
                .header-title { font-size: 24px; font-weight: 800; margin: 0 0 6px 0; color: #ffffff; letter-spacing: -0.5px; }
                .header-sub { font-size: 13px; color: #94A3B8; margin: 0; }
                .email-body { padding: 32px 30px; }
                .greeting { font-size: 16px; font-weight: 700; color: #0A1C35; margin-bottom: 8px; }
                .intro-text { font-size: 14px; color: #64748B; line-height: 1.6; margin-bottom: 24px; }
                .card { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 16px; padding: 22px; margin-bottom: 20px; }
                .card-title { font-size: 11px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; color: #64748B; margin-bottom: 14px; }
                .reason-card { background: #FEF2F2; border: 1px solid #FECACA; border-left: 4px solid #EF4444; border-radius: 12px; padding: 16px; margin-bottom: 24px; }
                .reason-title { font-size: 11px; font-weight: 800; color: #DC2626; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
                .reason-text { font-size: 13px; font-weight: 700; color: #991B1B; margin: 0; }
                .email-footer { background: #071524; padding: 24px 30px; text-align: center; color: #64748B; font-size: 11px; line-height: 1.6; }
                .footer-brand { color: #ffffff; font-weight: 800; font-size: 13px; letter-spacing: 1px; margin-bottom: 4px; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <div class="brand-badge">CURIX HEALTHCARE &bull; CANCELLATION NOTICE</div>
                  <h1 class="header-title">Appointment Cancelled</h1>
                  <p class="header-sub">Your hospital outpatient consultation has been cancelled</p>
                </div>

                <div class="email-body">
                  <div class="greeting">Hello %s,</div>
                  <p class="intro-text">
                    This is an official notification to confirm that your scheduled hospital consultation has been cancelled. Below are the details of the cancelled booking:
                  </p>

                  <div class="card">
                    <div class="card-title">Cancelled Visit Details</div>
                    <table style="width: 100%%; border-collapse: collapse;">
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Consulting Doctor</td>
                        <td style="padding: 8px 0; font-size: 13px; font-weight: 800; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Department</td>
                        <td style="padding: 8px 0; font-size: 13px; font-weight: 700; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Cancelled Date</td>
                        <td style="padding: 8px 0; font-size: 13px; font-weight: 700; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Time Window</td>
                        <td style="padding: 8px 0; font-size: 13px; font-weight: 700; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Booking Reference</td>
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 700; color: #64748B; text-align: right;">#%s</td>
                      </tr>
                    </table>
                  </div>

                  <div class="reason-card">
                    <div class="reason-title">Cancellation Reason</div>
                    <p class="reason-text">%s</p>
                  </div>

                  <p style="font-size: 13px; color: #64748B; line-height: 1.6; text-align: center; margin: 20px 0 0 0;">
                    Need to reschedule? You can book an appointment with any available physician at your convenience through your <strong>Curix Patient Dashboard</strong>, or call our 24/7 care desk at <strong>+1 (800) 287-4900</strong>.
                  </p>
                </div>

                <div class="email-footer">
                  <div class="footer-brand">CURIX HEALTHCARE SYSTEMS</div>
                  <p style="margin: 4px 0 0 0;">HIPAA-Aligned & Encrypted Patient Health Network</p>
                  <p style="margin: 4px 0 0 0; color: #475569;">This is an automated clinical notification. Please do not reply directly to this email.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                patientName,
                appt.getDoctorName() != null ? appt.getDoctorName() : "Attending Physician",
                appt.getDepartmentName() != null ? appt.getDepartmentName() : "General Consultation",
                dateFormatted,
                timeRange,
                appt.getId() != null ? appt.getId().toString() : "N/A",
                reasonDisplay
            );
    }

    private String buildPrescriptionHtmlBody(PrescriptionResponse rx) {
        String patientName = (rx.getPatientName() != null && !rx.getPatientName().trim().isEmpty()) 
                ? rx.getPatientName() 
                : "Valued Patient";

        String doctorName = rx.getDoctorName() != null ? rx.getDoctorName() : "Attending Physician";
        String department = rx.getDepartmentName() != null ? rx.getDepartmentName() : "General Clinical OPD";
        String diagnosis = (rx.getDiagnosis() != null && !rx.getDiagnosis().trim().isEmpty()) 
                ? rx.getDiagnosis() 
                : "Routine Clinical Consultation";

        String dateFormatted = rx.getAppointmentDate() != null 
                ? rx.getAppointmentDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH))
                : (rx.getCreatedAt() != null ? rx.getCreatedAt().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH)) : "Official Date");

        String rxNumber = rx.getId() != null ? rx.getId().toString() : "SLIP";
        String apptNumber = rx.getAppointmentId() != null ? "#" + rx.getAppointmentId() : "N/A";
        String patNumber = rx.getPatientNumber() != null ? rx.getPatientNumber() : "Registered Patient";

        StringBuilder rows = new StringBuilder();
        if (rx.getItems() != null && !rx.getItems().isEmpty()) {
            int idx = 1;
            for (PrescriptionItemDto item : rx.getItems()) {
                String med = item.getMedicationName() != null ? item.getMedicationName() : "Medication";
                String dosage = item.getDosage() != null ? item.getDosage() : "As directed";
                String freq = item.getFrequency() != null ? item.getFrequency() : "Daily";
                String dur = item.getDuration() != null ? item.getDuration() : "Course duration";
                String inst = (item.getInstructions() != null && !item.getInstructions().trim().isEmpty()) 
                        ? item.getInstructions().trim() 
                        : "Follow standard medical advice";

                rows.append("""
                    <tr style="border-bottom: 1px solid #E2E8F0;">
                      <td style="padding: 12px 8px; font-weight: bold; color: #64748B; font-size: 12px; text-align: center;">%d</td>
                      <td style="padding: 12px 10px; font-weight: 800; color: #0A1C35; font-size: 13px;">%s</td>
                      <td style="padding: 12px 10px; font-weight: 800; color: #00A896; font-size: 12px;">%s</td>
                      <td style="padding: 12px 10px; font-weight: 600; color: #334155; font-size: 12px;">%s</td>
                      <td style="padding: 12px 10px; font-weight: 600; color: #334155; font-size: 12px;">%s</td>
                      <td style="padding: 12px 10px; color: #475569; font-style: italic; font-size: 11px;">%s</td>
                    </tr>
                    """.formatted(idx++, med, dosage, freq, dur, inst));
            }
        } else {
            rows.append("""
                <tr>
                  <td colspan="6" style="padding: 20px; text-align: center; color: #94A3B8; font-size: 12px;">
                    No specific pharmaceutical items listed. Follow verbal and clinical advice provided.
                  </td>
                </tr>
                """);
        }

        String notesSection = "";
        if (rx.getNotes() != null && !rx.getNotes().trim().isEmpty()) {
            notesSection = "<div style=\"background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 16px; margin-bottom: 24px;\">"
                    + "<span style=\"font-size: 11px; font-weight: 800; color: #00A896; text-transform: uppercase; letter-spacing: 0.5px; display: block; margin-bottom: 6px;\">Doctor's Notes & Advice:</span>"
                    + "<p style=\"margin: 0; font-size: 13px; color: #334155; line-height: 1.6;\">" + rx.getNotes().trim() + "</p></div>";
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Digital Prescription - Curix Healthcare</title>
              <style>
                body { margin: 0; padding: 0; background-color: #F1F5F9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1E293B; }
                .email-container { max-width: 650px; margin: 30px auto; background: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #E2E8F0; }
                .email-header { background: linear-gradient(135deg, #0A1C35 0%%, #0f2c52 100%%); padding: 36px 30px; text-align: center; color: #ffffff; }
                .brand-badge { display: inline-block; background: rgba(0, 168, 150, 0.2); border: 1px solid #00A896; color: #0BC2A8; font-size: 11px; font-weight: 800; letter-spacing: 1.5px; text-transform: uppercase; padding: 5px 14px; border-radius: 9999px; margin-bottom: 12px; }
                .header-title { font-size: 24px; font-weight: 800; margin: 0 0 6px 0; color: #ffffff; letter-spacing: -0.5px; }
                .header-sub { font-size: 13px; color: #94A3B8; margin: 0; }
                .email-body { padding: 32px 30px; }
                .card { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 16px; padding: 20px; margin-bottom: 22px; }
                .card-title { font-size: 11px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; color: #00A896; margin-bottom: 12px; }
                .diagnosis-box { background: #FFFFFF; border: 1px solid #CBD5E1; border-left: 4px solid #00A896; border-radius: 8px; padding: 14px 16px; font-weight: 700; color: #0A1C35; font-size: 14px; margin-top: 6px; }
                .table-container { margin: 20px 0 24px 0; border: 1px solid #E2E8F0; border-radius: 14px; overflow: hidden; }
                table { width: 100%%; border-collapse: collapse; }
                th { background: #F8FAFC; padding: 11px 10px; text-align: left; font-size: 11px; text-transform: uppercase; color: #64748B; font-weight: 800; letter-spacing: 0.5px; border-bottom: 1px solid #E2E8F0; }
                .seal-wrap { display: flex; justify-content: space-between; align-items: flex-end; padding-top: 20px; border-top: 1px solid #E2E8F0; margin-top: 24px; font-size: 12px; color: #64748B; }
                .seal-box { text-align: center; border-top: 1px solid #94A3B8; padding-top: 6px; width: 180px; font-weight: 700; color: #0A1C35; }
                .email-footer { background: #071524; padding: 24px 30px; text-align: center; color: #64748B; font-size: 11px; line-height: 1.6; }
                .footer-brand { color: #ffffff; font-weight: 800; font-size: 13px; letter-spacing: 1px; margin-bottom: 4px; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <div class="brand-badge">CURIX CLINICAL OUTPATIENT &bull; DIGITAL Rx</div>
                  <h1 class="header-title">&#x269E; Your Prescription is Ready!</h1>
                  <p class="header-sub">Your digital prescription has been issued by your attending physician</p>
                </div>

                <div class="email-body">
                  <div style="font-size: 16px; font-weight: 700; color: #0A1C35; margin-bottom: 8px;">Hello %s,</div>
                  <p style="font-size: 14px; color: #64748B; line-height: 1.6; margin-bottom: 24px;">
                    Great news! Your prescription is ready. Your attending physician has finalized your medical consultation and generated your official electronic prescription slip. All prescribed medications, timings, and instructions are detailed below.
                  </p>

                  <div class="card">
                    <div class="card-title">Consultation & Patient Summary</div>
                    <table style="width: 100%%; border-collapse: collapse;">
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Patient Name</td>
                        <td style="padding: 8px 0; font-size: 13px; font-weight: 800; color: #0A1C35; text-align: right;">%s (%s)</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Attending Physician</td>
                        <td style="padding: 8px 0; font-size: 14px; font-weight: 800; color: #00A896; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Department / Unit</td>
                        <td style="padding: 8px 0; font-size: 13px; font-weight: 700; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr style="border-bottom: 1px dashed #E2E8F0;">
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Date Issued</td>
                        <td style="padding: 8px 0; font-size: 13px; font-weight: 700; color: #0A1C35; text-align: right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; font-size: 12px; font-weight: 600; color: #64748B;">Rx Slip Reference</td>
                        <td style="padding: 8px 0; font-size: 13px; font-weight: 800; color: #00A896; font-family: monospace; text-align: right;">Rx #%s (Visit %s)</td>
                      </tr>
                    </table>
                  </div>

                  <div style="margin-bottom: 22px;">
                    <span style="font-size: 11px; font-weight: 800; text-transform: uppercase; color: #64748B; letter-spacing: 0.5px;">Clinical Diagnosis</span>
                    <div class="diagnosis-box">
                      %s
                    </div>
                  </div>

                  <div>
                    <span style="font-size: 11px; font-weight: 800; text-transform: uppercase; color: #0A1C35; letter-spacing: 0.5px;">&#x211E; Prescribed Medications & Regimen</span>
                    <div class="table-container">
                      <table>
                        <thead>
                          <tr>
                            <th style="text-align: center; width: 30px;">#</th>
                            <th>Medication</th>
                            <th>Dosage</th>
                            <th>Frequency</th>
                            <th>Duration</th>
                            <th>Instructions</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                        </tbody>
                      </table>
                    </div>
                  </div>

                  %s

                  <div class="seal-wrap">
                    <div>
                      This is an official system-generated digital prescription.<br>
                      Access your full records anytime on your <strong>Curix Patient Vault</strong>.
                    </div>
                    <div class="seal-box">
                      Digitally Authorized<br>
                      <span style="font-size: 10px; font-weight: normal; color: #64748B;">Medical Officer Seal</span>
                    </div>
                  </div>
                </div>

                <div class="email-footer">
                  <div class="footer-brand">CURIX HEALTHCARE SYSTEMS</div>
                  <p style="margin: 4px 0 0 0;">HIPAA-Aligned & Encrypted Patient Health Network</p>
                  <p style="margin: 4px 0 0 0; color: #475569;">Please take prescribed medicines as instructed. For emergencies, contact our hospital helpline at +1 (800) 287-4900.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                patientName,
                patientName,
                patNumber,
                doctorName,
                department,
                dateFormatted,
                rxNumber,
                apptNumber,
                diagnosis,
                rows.toString(),
                notesSection
            );
    }

    private String buildFallbackPrescriptionHtmlBody(PrescriptionResponse rx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family: Arial, sans-serif; padding: 24px; color: #1e293b; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; max-width: 600px; margin: 0 auto;\">");
        sb.append("<h2 style=\"color: #00A896; margin-top: 0;\">Your Prescription is Ready</h2>");
        sb.append("<p>Hello <strong>").append(rx.getPatientName() != null ? rx.getPatientName() : "Valued Patient").append("</strong>,</p>");
        sb.append("<p>Your prescription has been issued by <strong>").append(rx.getDoctorName() != null ? rx.getDoctorName() : "Attending Physician").append("</strong>.</p>");
        sb.append("<p><strong>Diagnosis:</strong> ").append(rx.getDiagnosis() != null ? rx.getDiagnosis() : "Routine Consultation").append("</p>");
        sb.append("<table style=\"width: 100%; border-collapse: collapse; margin-top: 16px; border: 1px solid #e2e8f0;\">");
        sb.append("<thead><tr style=\"background: #f1f5f9;\"><th style=\"padding: 8px; border: 1px solid #e2e8f0;\">Medication</th><th style=\"padding: 8px; border: 1px solid #e2e8f0;\">Dosage</th><th style=\"padding: 8px; border: 1px solid #e2e8f0;\">Frequency</th><th style=\"padding: 8px; border: 1px solid #e2e8f0;\">Duration</th><th style=\"padding: 8px; border: 1px solid #e2e8f0;\">Instructions</th></tr></thead><tbody>");
        if (rx.getItems() != null && !rx.getItems().isEmpty()) {
            for (PrescriptionItemDto item : rx.getItems()) {
                sb.append("<tr>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #e2e8f0; font-weight: bold;\">").append(item.getMedicationName()).append("</td>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #e2e8f0; color: #00A896;\">").append(item.getDosage() != null ? item.getDosage() : "As directed").append("</td>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #e2e8f0;\">").append(item.getFrequency() != null ? item.getFrequency() : "1-0-1").append("</td>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #e2e8f0;\">").append(item.getDuration() != null ? item.getDuration() : "5 days").append("</td>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #e2e8f0; font-style: italic;\">").append(item.getInstructions() != null ? item.getInstructions() : "After meals").append("</td>");
                sb.append("</tr>");
            }
        } else {
            sb.append("<tr><td colspan=\"5\" style=\"padding: 12px; text-align: center;\">Follow medical advice as directed.</td></tr>");
        }
        sb.append("</tbody></table>");
        if (rx.getNotes() != null && !rx.getNotes().trim().isEmpty()) {
            sb.append("<div style=\"margin-top: 16px; padding: 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;\"><strong>Doctor's Notes:</strong><p style=\"margin: 4px 0 0;\">").append(rx.getNotes().trim()).append("</p></div>");
        }
        sb.append("<p style=\"margin-top: 24px; font-size: 11px; color: #64748b;\">Curix Healthcare Systems &bull; HIPAA Compliant Electronic Medical Records</p>");
        sb.append("</div>");
        return sb.toString();
    }
}
