package com.curix.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public class DashboardMetricsResponse {

    private long totalPatients;
    private long totalDoctors;
    private long totalDepartments;
    private long totalAppointments;
    private long totalPrescriptions;
    private long completedAppointments;
    private long cancelledAppointments;
    private long pendingOrConfirmedAppointments;
    private BigDecimal totalRevenue;
    private BigDecimal estimatedRevenue;
    private Map<String, Long> appointmentsByDepartment;
    private Map<String, Long> statusCounts;

    public DashboardMetricsResponse() {}

    // Constructor matching DashboardServiceImpl (7 arguments)
    public DashboardMetricsResponse(long totalPatients,
                                    long totalDoctors,
                                    long totalDepartments,
                                    long totalAppointments,
                                    long totalPrescriptions,
                                    BigDecimal estimatedRevenue,
                                    Map<String, Long> statusCounts) {
        this.totalPatients = totalPatients;
        this.totalDoctors = totalDoctors;
        this.totalDepartments = totalDepartments;
        this.totalAppointments = totalAppointments;
        this.totalPrescriptions = totalPrescriptions;
        this.estimatedRevenue = estimatedRevenue;
        this.totalRevenue = estimatedRevenue;
        this.statusCounts = statusCounts;
    }

    public long getTotalPatients() { return totalPatients; }
    public void setTotalPatients(long totalPatients) { this.totalPatients = totalPatients; }

    public long getTotalDoctors() { return totalDoctors; }
    public void setTotalDoctors(long totalDoctors) { this.totalDoctors = totalDoctors; }

    public long getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(long totalDepartments) { this.totalDepartments = totalDepartments; }

    public long getTotalAppointments() { return totalAppointments; }
    public void setTotalAppointments(long totalAppointments) { this.totalAppointments = totalAppointments; }

    public long getTotalPrescriptions() { return totalPrescriptions; }
    public void setTotalPrescriptions(long totalPrescriptions) { this.totalPrescriptions = totalPrescriptions; }

    public long getCompletedAppointments() { return completedAppointments; }
    public void setCompletedAppointments(long completedAppointments) { this.completedAppointments = completedAppointments; }

    public long getCancelledAppointments() { return cancelledAppointments; }
    public void setCancelledAppointments(long cancelledAppointments) { this.cancelledAppointments = cancelledAppointments; }

    public long getPendingOrConfirmedAppointments() { return pendingOrConfirmedAppointments; }
    public void setPendingOrConfirmedAppointments(long pendingOrConfirmedAppointments) { this.pendingOrConfirmedAppointments = pendingOrConfirmedAppointments; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { 
        this.totalRevenue = totalRevenue; 
        this.estimatedRevenue = totalRevenue;
    }

    public BigDecimal getEstimatedRevenue() { return estimatedRevenue; }
    public void setEstimatedRevenue(BigDecimal estimatedRevenue) { 
        this.estimatedRevenue = estimatedRevenue; 
        this.totalRevenue = estimatedRevenue;
    }

    public Map<String, Long> getAppointmentsByDepartment() { return appointmentsByDepartment; }
    public void setAppointmentsByDepartment(Map<String, Long> appointmentsByDepartment) { this.appointmentsByDepartment = appointmentsByDepartment; }

    public Map<String, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Long> statusCounts) { this.statusCounts = statusCounts; }
}