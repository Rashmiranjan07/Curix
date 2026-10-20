package com.curix.service;

import com.curix.dto.response.DashboardMetricsResponse;

public interface DashboardService {
    DashboardMetricsResponse getHospitalMetrics();
}