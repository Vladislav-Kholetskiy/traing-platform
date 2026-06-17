package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, как {@code AttemptTerminalCriticalAuditPayload} создаёт данные и вспомогательные объекты.
 * Это помогает не сломать начальную сборку данных.
 */
class AttemptTerminalCriticalAuditPayloadFactoryTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T21:00:00Z");

    private final AttemptTerminalCriticalAuditPayloadFactory factory = new AttemptTerminalCriticalAuditPayloadFactory();

    @Test
    void assignedExpiryDetailsUseSystemActorSourceWhileInteractiveCommandsStayInteractive() {
        assertThat(factory.createAssignedSubmitDetails(701L, 501L))
            .containsEntry("commandType", "assigned_attempt_submit")
            .containsEntry("terminalType", "completed")
            .containsEntry("actorSource", "interactive");
        assertThat(factory.createAssignedExpiryDetails(701L, 501L))
            .containsEntry("commandType", "assigned_attempt_expire")
            .containsEntry("terminalType", "expired")
            .containsEntry("actorSource", "system");
        assertThat(factory.createSelfSubmitDetails(501L))
            .containsEntry("commandType", "self_attempt_submit")
            .containsEntry("terminalType", "completed")
            .containsEntry("actorSource", "interactive");
        assertThat(factory.createSelfAbandonDetails(501L))
            .containsEntry("commandType", "self_attempt_abandon")
            .containsEntry("terminalType", "abandoned")
            .containsEntry("actorSource", "interactive");
    }

    @Test
    void payloadAfterContainsTerminalAttemptFieldsForAuditSnapshot() {
        TestAttempt attempt = new TestAttempt(
            1L,
            101L,
            501L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.ABANDONED,
            FIXED_INSTANT,
            null,
            null,
            FIXED_INSTANT.plusSeconds(60),
            FIXED_INSTANT.plusSeconds(60),
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(60)
        );

        Map<String, Object> payload = factory.payloadAfter(attempt);

        assertThat(payload).containsKeys("id", "userId", "testId", "attemptMode", "status", "abandonedAt", "updatedAt");
        assertThat(payload).containsEntry("status", TestAttemptStatus.ABANDONED);
        assertThat(payload).containsEntry("assignmentTestId", null);
    }
}
