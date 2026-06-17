package com.vladislav.training.platform.audit.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение сервиса {@code FallbackInMemoryAudit}.
 * Сценарии сосредоточены на прикладной логике.
 */
class FallbackInMemoryAuditServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T12:00:00Z");

    private final FallbackInMemoryAuditService auditService = new FallbackInMemoryAuditService();

    @Test
    void recordAuditEventRejectsAlreadyPersistedFact() {
        AuditEvent persistedEvent = new AuditEvent(
            1L,
            new AuditEventType("content.course.published"),
            "course",
            "42",
            101L,
            FIXED_INSTANT,
            null,
            null,
            null,
            "corr-1",
            "req-1",
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> auditService.recordAuditEvent(persistedEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id must be null")
            .hasMessageContaining("write-side insert");
    }
}


