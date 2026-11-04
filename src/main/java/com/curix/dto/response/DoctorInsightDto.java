package com.curix.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class DoctorInsightDto {

    private Long doctorId;
    private Long userId;
    private String doctorName;
    private String email;
    private String departmentName;
    private Long departmentId;
    private String specialty;
    private String licenseNumber;
    private BigDecimal consultationFee;
    private boolean active;

    private long totalAppointments;
    private long completedAppointments;
    private long prescriptionsIssued;
    private long pendingPrescriptions;
    private double prescriptionRate;
    private long activeAppointments;
    private long cancelledAppointments;
    private long uniquePatientsCount;
    private BigDecimal totalRevenue;

    private List<AppointmentResponse> appointments;

    public DoctorInsightDto() {}

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getTotalAppointments() { return totalAppointments; }
    public void setTotalAppointments(long totalAppointments) { this.totalAppointments = totalAppointments; }

    public long getCompletedAppointments() { return completedAppointments; }
    public void setCompletedAppointments(long completedAppointments) { this.completedAppointments = completedAppointments; }

    public long getPrescriptionsIssued() { return prescriptionsIssued; }
    public void setPrescriptionsIssued(long prescriptionsIssued) { this.prescriptionsIssued = prescriptionsIssued; }

    public long getPendingPrescriptions() { return pendingPrescriptions; }
    public void setPendingPrescriptions(long pendingPrescriptions) { this.pendingPrescriptions = pendingPrescriptions; }

    public double getPrescriptionRate() { return prescriptionRate; }
    public void setPrescriptionRate(double prescriptionRate) { this.prescriptionRate = prescriptionRate; }

    public long getActiveAppointments() { return activeAppointments; }
    public void setActiveAppointments(long activeAppointments) { this.activeAppointments = activeAppointments; }

    public long getCancelledAppointments() { return cancelledAppointments; }
    public void setCancelledAppointments(long cancelledAppointments) { this.cancelledAppointments = cancelledAppointments; }

    public long getUniquePatientsCount() { return uniquePatientsCount; }
    public void setUniquePatientsCount(long uniquePatientsCount) { this.uniquePatientsCount = uniquePatientsCount; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public List<AppointmentResponse> getAppointments() { return appointments; }
    public void setAppointments(List<AppointmentResponse> appointments) { this.appointments = appointments; }
}
