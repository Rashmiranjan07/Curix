package com.curix.service.impl;

import com.curix.dto.response.AppointmentResponse;
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

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>Appointment Cancelled - Curix Healthcare</title>
              <style>
                body { margin: 0; padding: 0; background-color: #F1F5F9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                .email-container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 20px; overflow: hidden; border: 1px solid #E2E8F0; }
                .email-header { background: #0A1C35; padding: 30px; text-align: center; color: #ffffff; }
                .email-body { padding: 30px; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <h2 style="margin: 0; color: #EF4444; font-size: 22px;">Appointment Cancelled</h2>
                  <p style="margin: 6px 0 0 0; color: #94A3B8; font-size: 13px;">Curix Healthcare Consultation Update</p>
                </div>
                <div class="email-body">
                  <p>Dear <strong>%s</strong>,</p>
                  <p>Your appointment with <strong>%s</strong> on <strong>%s</strong> at <strong>%s</strong> has been cancelled.</p>
                  <div style="background: #FEF2F2; border-left: 4px solid #EF4444; padding: 12px; margin: 16px 0; border-radius: 4px; font-size: 13px;">
                    <strong>Reason for cancellation:</strong> %s
                  </div>
                  <p style="font-size: 13px; color: #64748B;">You can book a new appointment anytime through your Curix Patient Portal.</p>
                </div>
                <div style="background: #071524; padding: 16px; text-align: center; color: #64748B; font-size: 11px;">
                  Curix Healthcare &bull; 24/7 Helpline: +1 (800) 287-4900
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                patientName,
                appt.getDoctorName() != null ? appt.getDoctorName() : "Doctor",
                appt.getAppointmentDate() != null ? appt.getAppointmentDate().toString() : "",
                appt.getStartTime() != null ? appt.getStartTime().toString() : "",
                reasonDisplay
            );
    }
}
