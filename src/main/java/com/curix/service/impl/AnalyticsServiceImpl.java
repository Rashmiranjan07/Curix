package com.curix.service.impl;

import com.curix.dto.response.AppointmentResponse;
import com.curix.dto.response.DashboardMetricsResponse;
import com.curix.dto.response.DoctorInsightDto;
import com.curix.entity.Appointment;
import com.curix.entity.DoctorProfile;
import com.curix.entity.Prescription;
import com.curix.enums.AppointmentStatus;
import com.curix.repository.AppointmentRepository;
import com.curix.repository.DepartmentRepository;
import com.curix.repository.DoctorProfileRepository;
import com.curix.repository.PatientProfileRepository;
import com.curix.repository.PrescriptionRepository;
import com.curix.service.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;

    public AnalyticsServiceImpl(PatientProfileRepository patientProfileRepository,
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
    public DashboardMetricsResponse getDashboardMetrics() {
        DashboardMetricsResponse metrics = new DashboardMetricsResponse();

        metrics.setTotalPatients(patientProfileRepository.count());
        metrics.setTotalDoctors(doctorProfileRepository.count());
        metrics.setTotalDepartments(departmentRepository.count());
        metrics.setTotalPrescriptions(prescriptionRepository.count());

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

    @Override
    @Transactional(readOnly = true)
    public List<DoctorInsightDto> getDoctorClinicalInsights() {
        List<DoctorProfile> doctors = doctorProfileRepository.findAll();
        List<Appointment> allAppointments = appointmentRepository.findAll();
        List<Prescription> allPrescriptions = prescriptionRepository.findAll();

        Map<Long, Prescription> rxByApptId = new HashMap<>();
        Map<Long, List<Prescription>> rxByDocId = new HashMap<>();
        for (Prescription p : allPrescriptions) {
            if (p.getAppointment() != null && p.getAppointment().getId() != null) {
                rxByApptId.put(p.getAppointment().getId(), p);
            }
            if (p.getDoctor() != null && p.getDoctor().getId() != null) {
                rxByDocId.computeIfAbsent(p.getDoctor().getId(), k -> new ArrayList<>()).add(p);
            }
        }

        Map<Long, List<Appointment>> apptsByDoc = new HashMap<>();
        for (Appointment a : allAppointments) {
            if (a.getDoctor() != null && a.getDoctor().getId() != null) {
                apptsByDoc.computeIfAbsent(a.getDoctor().getId(), k -> new ArrayList<>()).add(a);
            }
        }

        List<DoctorInsightDto> insights = new ArrayList<>();
        for (DoctorProfile doc : doctors) {
            DoctorInsightDto dto = new DoctorInsightDto();
            dto.setDoctorId(doc.getId());
            if (doc.getUser() != null) {
                dto.setUserId(doc.getUser().getId());
                dto.setEmail(doc.getUser().getEmail());
                dto.setActive(doc.getUser().isActive());
            }
            dto.setDoctorName("Dr. " + doc.getFirstName() + " " + doc.getLastName());
            dto.setDepartmentName(doc.getDepartment() != null ? doc.getDepartment().getName() : "General Medicine");
            dto.setDepartmentId(doc.getDepartment() != null ? doc.getDepartment().getId() : null);
            dto.setSpecialty(doc.getSpecialty());
            dto.setLicenseNumber(doc.getLicenseNumber());
            dto.setConsultationFee(doc.getConsultationFee());

            List<Appointment> docAppts = apptsByDoc.getOrDefault(doc.getId(), Collections.emptyList());
            long total = docAppts.size();
            long completed = 0;
            long active = 0;
            long cancelled = 0;
            long rxIssuedForAppts = 0;
            BigDecimal revenue = BigDecimal.ZERO;
            Set<Long> uniquePatients = new HashSet<>();

            List<AppointmentResponse> apptResponses = new ArrayList<>();
            for (Appointment a : docAppts) {
                if (a.getStatus() == AppointmentStatus.COMPLETED) {
                    completed++;
                    if (a.getConsultationFee() != null) {
                        revenue = revenue.add(a.getConsultationFee());
                    }
                } else if (a.getStatus() == AppointmentStatus.CANCELLED) {
                    cancelled++;
                } else {
                    active++;
                }

                if (a.getPatient() != null && a.getPatient().getId() != null) {
                    uniquePatients.add(a.getPatient().getId());
                }

                AppointmentResponse res = mapAppointmentToResponse(a);
                Prescription rx = rxByApptId.get(a.getId());
                if (rx != null) {
                    res.setHasPrescription(true);
                    res.setPrescriptionId(rx.getId());
                    rxIssuedForAppts++;
                } else {
                    res.setHasPrescription(false);
                    res.setPrescriptionId(null);
                }
                apptResponses.add(res);
            }

            List<Prescription> docRxs = rxByDocId.getOrDefault(doc.getId(), Collections.emptyList());
            long totalRxIssued = Math.max(rxIssuedForAppts, (long) docRxs.size());
            long pendingRx = Math.max(0, completed - rxIssuedForAppts);
            double rxRate = completed > 0 ? Math.round((rxIssuedForAppts * 100.0 / completed) * 10.0) / 10.0 : 0.0;

            apptResponses.sort((a, b) -> {
                int c = b.getAppointmentDate().compareTo(a.getAppointmentDate());
                if (c != 0) return c;
                return b.getStartTime() != null && a.getStartTime() != null ? b.getStartTime().compareTo(a.getStartTime()) : 0;
            });

            dto.setTotalAppointments(total);
            dto.setCompletedAppointments(completed);
            dto.setPrescriptionsIssued(totalRxIssued);
            dto.setPendingPrescriptions(pendingRx);
            dto.setPrescriptionRate(rxRate);
            dto.setActiveAppointments(active);
            dto.setCancelledAppointments(cancelled);
            dto.setUniquePatientsCount(uniquePatients.size());
            dto.setTotalRevenue(revenue);
            dto.setAppointments(apptResponses);

            insights.add(dto);
        }

        insights.sort((a, b) -> Long.compare(b.getTotalAppointments(), a.getTotalAppointments()));
        return insights;
    }

    private AppointmentResponse mapAppointmentToResponse(Appointment appt) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(appt.getId());
        if (appt.getPatient() != null) {
            res.setPatientId(appt.getPatient().getId());
            res.setPatientName(appt.getPatient().getFirstName() + " " + appt.getPatient().getLastName());
            res.setPatientNumber(appt.getPatient().getPatientNumber());
        }
        if (appt.getDoctor() != null) {
            res.setDoctorId(appt.getDoctor().getId());
            res.setDoctorName("Dr. " + appt.getDoctor().getFirstName() + " " + appt.getDoctor().getLastName());
            res.setDepartmentName(appt.getDoctor().getDepartment() != null ? appt.getDoctor().getDepartment().getName() : "General");
        }
        res.setAppointmentDate(appt.getAppointmentDate());
        res.setStartTime(appt.getStartTime());
        res.setEndTime(appt.getEndTime());
        res.setStatus(appt.getStatus());
        res.setConsultationFee(appt.getConsultationFee());
        res.setReasonForVisit(appt.getReasonForVisit());
        res.setCancellationReason(appt.getCancellationReason());
        res.setCreatedAt(appt.getCreatedAt());
        return res;
    }
}
