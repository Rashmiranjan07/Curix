package com.curix.service.impl;

import com.curix.dto.response.DashboardMetricsResponse;
import com.curix.entity.Appointment;
import com.curix.enums.AppointmentStatus;
import com.curix.repository.AppointmentRepository;
import com.curix.repository.DepartmentRepository;
import com.curix.repository.DoctorProfileRepository;
import com.curix.repository.PatientProfileRepository;
import com.curix.repository.PrescriptionRepository;
import com.curix.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;

    public DashboardServiceImpl(PatientProfileRepository patientProfileRepository,
                                DoctorProfileRepository doctorProfileRepository,
                                DepartmentRepository departmentRepository,
                                AppointmentRepository appointmentRepository,
                                PrescriptionRepository prescriptionRepository) {
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsResponse getHospitalMetrics() {
        long totalPatients = patientProfileRepository.count();
        long totalDoctors = doctorProfileRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalPrescriptions = prescriptionRepository.count();

        List<Appointment> allAppointments = appointmentRepository.findAll();
        long totalAppointments = allAppointments.size();

        Map<String, Long> statusCounts = new HashMap<>();
        for (AppointmentStatus status : AppointmentStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }

        BigDecimal revenueEstimate = BigDecimal.ZERO;

        for (Appointment appt : allAppointments) {
            String statusKey = appt.getStatus().name();
            statusCounts.put(statusKey, statusCounts.getOrDefault(statusKey, 0L) + 1);

            // Compute revenue for CONFIRMED or COMPLETED appointments
            if (appt.getStatus() == AppointmentStatus.CONFIRMED || appt.getStatus() == AppointmentStatus.COMPLETED) {
                if (appt.getConsultationFee() != null) {
                    revenueEstimate = revenueEstimate.add(appt.getConsultationFee());
                }
            }
        }

        return new DashboardMetricsResponse(
                totalPatients,
                totalDoctors,
                totalDepartments,
                totalAppointments,
                totalPrescriptions,
                revenueEstimate,
                statusCounts
        );
    }
}