package com.curix.service;

import com.curix.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    void logAction(Long actorId, String actorEmail, String action, String entityName, Long entityId, String details, String ipAddress);
    Page<AuditLogResponse> getAuditLogs(Pageable pageable);
}