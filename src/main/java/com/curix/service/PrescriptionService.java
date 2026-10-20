package com.curix.service;

import com.curix.dto.request.CreatePrescriptionRequest;
import com.curix.dto.response.PrescriptionResponse;

import java.util.List;

public interface PrescriptionService {

    PrescriptionResponse createPrescription(Long doctorUserId, CreatePrescriptionRequest request);

    PrescriptionResponse getPrescriptionById(Long prescriptionId, Long requestingUserId);

    PrescriptionResponse getPrescriptionByAppointmentId(Long appointmentId, Long requestingUserId);

    List<PrescriptionResponse> getMyPrescriptionsAsPatient(Long patientUserId);

    List<PrescriptionResponse> getMyPrescriptionsAsDoctor(Long doctorUserId);
}