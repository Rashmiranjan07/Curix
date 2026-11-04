package com.curix.service;

import com.curix.dto.response.DashboardMetricsResponse;
import com.curix.dto.response.DoctorInsightDto;

import java.util.List;

public interface AnalyticsService {
    DashboardMetricsResponse getDashboardMetrics();
    List<DoctorInsightDto> getDoctorClinicalInsights();
}
