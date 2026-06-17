package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, как {@code SelfAttemptEntryCriticalAuditPayload} создаёт данные и вспомогательные объекты.
 * Это помогает не сломать начальную сборку данных.
 */
class SelfAttemptEntryCriticalAuditPayloadFactoryTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T17:00:00Z");

    private final SelfAttemptEntryCriticalAuditPayloadFactory factory = new SelfAttemptEntryCriticalAuditPayloadFactory();

    @Test
    void payloadFactoryBuildsCreateOnlySelfEntryAuditShape() {
        TestAttempt attempt = new TestAttempt(
            9002L,
            101L,
            501L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.STARTED,
            FIXED_INSTANT,
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );

        assertThat(factory.createDetails(501L))
            .containsEntry("commandType", "self_attempt_start")
            .containsEntry("entryMode", "create")
            .containsEntry("testId", 501L);

        assertThat(factory.payloadAfter(attempt)).containsKey("attempt");
        @SuppressWarnings("unchecked")
        Map<String, Object> attemptPayload = (Map<String, Object>) factory.payloadAfter(attempt).get("attempt");
        assertThat(attemptPayload)
            .containsEntry("id", 9002L)
            .containsEntry("assignmentTestId", null)
            .containsEntry("attemptMode", AttemptMode.SELF)
            .containsEntry("status", TestAttemptStatus.STARTED);
    }
}
