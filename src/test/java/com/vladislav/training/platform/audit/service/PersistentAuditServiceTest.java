package com.vladislav.training.platform.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.repository.AuditEventRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code PersistentAudit}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class PersistentAuditServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T12:00:00Z");

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private PersistentAuditService auditService;

    @Test
    void recordAuditEventPersistsImmutableFactSynchronously() {
        AuditEvent commandEvent = new AuditEvent(
            null,
            new AuditEventType("userorg.app_user.created"),
            "app_user",
            "15",
            101L,
            FIXED_INSTANT,
            null,
            null,
            null,
            "corr-1",
            "req-1",
            FIXED_INSTANT
        );
        AuditEvent persistedEvent = new AuditEvent(
            700L,
            commandEvent.eventType(),
            commandEvent.entityType(),
            commandEvent.entityId(),
            commandEvent.actorUserId(),
            commandEvent.occurredAt(),
            commandEvent.payloadBefore(),
            commandEvent.payloadAfter(),
            commandEvent.contextPayload(),
            commandEvent.correlationId(),
            commandEvent.requestId(),
            commandEvent.createdAt()
        );
        when(auditEventRepository.saveAuditEvent(commandEvent)).thenReturn(persistedEvent);

        AuditEvent result = auditService.recordAuditEvent(commandEvent);

        assertThat(result.id()).isEqualTo(700L);
        verify(auditEventRepository).saveAuditEvent(commandEvent);
    }

    @Test
    void recordAuditEventRejectsAlreadyPersistedFact() {
        AuditEvent persistedEvent = new AuditEvent(
            1L,
            new AuditEventType("userorg.app_user.created"),
            "app_user",
            "15",
            101L,
            FIXED_INSTANT,
            null,
            null,
            null,
            null,
            null,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> auditService.recordAuditEvent(persistedEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be null");
    }
}