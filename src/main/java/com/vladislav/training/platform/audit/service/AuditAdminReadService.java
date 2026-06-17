package com.vladislav.training.platform.audit.service;

import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code AuditAdminReadService}.
 */
public interface AuditAdminReadService {

    List<AuditEventReadModel> listAdminAuditEvents(Long actorUserId, AuditEventReadFilter filter);

    AuditEventReadModel findAdminAuditEventById(Long actorUserId, Long auditEventId);

    /**
     * Набор фильтров для административного чтения записей аудита.
     */
    record AuditEventReadFilter(
        String eventType,
        String entityType,
        String entityId,
        Long actorUserId,
        Instant occurredFrom,
        Instant occurredTo
    ) {

        public AuditEventReadFilter(String eventType, String entityType, String entityId) {
            this(eventType, entityType, entityId, null, null, null);
        }

        public AuditEventReadFilter {
            if ((entityType == null) != (entityId == null)) {
                throw new IllegalArgumentException("entityType and entityId must be both null or both non-null");
            }
            if (eventType != null && eventType.isBlank()) {
                throw new IllegalArgumentException("eventType must not be blank");
            }
            if (entityType != null && entityType.isBlank()) {
                throw new IllegalArgumentException("entityType must not be blank");
            }
            if (entityId != null && entityId.isBlank()) {
                throw new IllegalArgumentException("entityId must not be blank");
            }
            if (occurredFrom != null && occurredTo != null && occurredFrom.isAfter(occurredTo)) {
                throw new IllegalArgumentException("occurredFrom must not be after occurredTo");
            }
        }
    }

    /**
     * Упрощённая модель записи аудита для административного списка и детального просмотра.
     */
    record AuditEventReadModel(
        Long id,
        String eventType,
        String entityType,
        String entityId,
        Long actorUserId,
        Instant occurredAt,
        String correlationId,
        String requestId,
        Instant createdAt
    ) {
    }
}
