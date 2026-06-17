package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, как {@code AssignedAttemptEntryCriticalAuditPayload} создаёт данные и вспомогательные объекты.
 * Это помогает не сломать начальную сборку данных.
 */
class AssignedAttemptEntryCriticalAuditPayloadFactoryTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T11:20:00Z");

    private final AssignedAttemptEntryCriticalAuditPayloadFactory factory = new AssignedAttemptEntryCriticalAuditPayloadFactory();

    @Test
    void payloadFactoryBuildsCreateOnlyAssignedEntryAuditShape() {
        TestAttempt attempt = new TestAttempt(
            9001L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.STARTED,
            FIXED_INSTANT,
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );

        assertThat(factory.createDetails(77L, 701L, 501L))
            .containsEntry("commandType", "assigned_attempt_start")
            .containsEntry("entryMode", "create")
            .containsEntry("assignmentId", 77L)
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("testId", 501L);

        assertThat(factory.payloadAfter(attempt, 77L))
            .containsEntry("assignmentId", 77L)
            .containsKey("attempt");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> attemptPayload = (java.util.Map<String, Object>) factory.payloadAfter(attempt, 77L)
            .get("attempt");
        assertThat(attemptPayload)
            .containsEntry("id", 9001L)
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("attemptMode", AttemptMode.ASSIGNED)
            .containsEntry("status", TestAttemptStatus.STARTED);
    }
}
