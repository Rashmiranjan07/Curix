package com.curix.service.impl;

import com.curix.dto.response.DashboardMetricsResponse;
import com.curix.entity.Appointment;
import com.curix.enums.AppointmentStatus;
import com.curix.repository.AppointmentRepository;
import com.curix.repository.DepartmentRepository;
import com.curix.repository.DoctorProfileRepository;
import com.curix.repository.PatientProfileRepository;
import com.curix.service.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;

    public AnalyticsServiceImpl(PatientProfileRepository patientProfileRepository,
                                DoctorProfileRepository doctorProfileRepository,
                                DepartmentRepository departmentRepository,
                                AppointmentRepository appointmentRepository) {
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsResponse getDashboardMetrics() {
        DashboardMetricsResponse metrics = new DashboardMetricsResponse();

        metrics.setTotalPatients(patientProfileRepository.count());
        metrics.setTotalDoctors(doctorProfileRepository.count());
        metrics.setTotalDepartments(departmentRepository.count());

        List<Appointment> allAppointments = appointmentRepository.findAll();
        metrics.setTotalAppointments(allAppointments.size());

        long completed = 0;
        long cancelled = 0;
        long active = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, Long> deptCounts = new HashMap<>();

        for (Appointment appt : allAppointments) {
            if (appt.getStatus() == AppointmentStatus.COMPLETED) {
                completed++;
                if (appt.getConsultationFee() != null) {
                    totalRevenue = totalRevenue.add(appt.getConsultationFee());
                }
            } else if (appt.getStatus() == AppointmentStatus.CANCELLED) {
                cancelled++;
            } else {
                active++;
            }

            String deptName = (appt.getDoctor() != null && appt.getDoctor().getDepartment() != null)
                    ? appt.getDoctor().getDepartment().getName()
                    : "General";
            deptCounts.put(deptName, deptCounts.getOrDefault(deptName, 0L) + 1);
        }

        metrics.setCompletedAppointments(completed);
        metrics.setCancelledAppointments(cancelled);
        metrics.setPendingOrConfirmedAppointments(active);
        metrics.setTotalRevenue(totalRevenue);
        metrics.setAppointmentsByDepartment(deptCounts);

        return metrics;
    }
}