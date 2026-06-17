package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, как {@code AttemptAnswerMutationCriticalAuditPayload} создаёт данные и вспомогательные объекты.
 * Это помогает не сломать начальную сборку данных.
 */
class AttemptAnswerMutationCriticalAuditPayloadFactoryTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-22T10:15:00Z");

    private final AttemptAnswerMutationCriticalAuditPayloadFactory factory =
        new AttemptAnswerMutationCriticalAuditPayloadFactory();

    @Test
    void payloadFactoryBuildsAssignedAndSelfAnswerMutationAuditShape() {
        TestAttempt assignedAttempt = new TestAttempt(
            9001L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.IN_PROGRESS,
            FIXED_INSTANT.minusSeconds(300),
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT
        );
        TestAttempt selfAttempt = new TestAttempt(
            9002L,
            101L,
            502L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.IN_PROGRESS,
            FIXED_INSTANT.minusSeconds(600),
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT
        );

        assertThat(factory.createAssignedDetails("save_or_replace", 77L, 701L, 501L, 801L, 9001L, AttemptMode.ASSIGNED, 2))
            .containsEntry("commandType", "assigned_answer_mutation")
            .containsEntry("mutationAction", "save_or_replace")
            .containsEntry("assignmentId", 77L)
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("questionId", 801L)
            .containsEntry("attemptId", 9001L)
            .containsEntry("attemptMode", AttemptMode.ASSIGNED)
            .containsEntry("answerItemCount", 2);

        assertThat(factory.createSelfDetails("clear", 502L, 802L, 9002L, AttemptMode.SELF, 0))
            .containsEntry("commandType", "self_answer_mutation")
            .containsEntry("mutationAction", "clear")
            .containsEntry("questionId", 802L)
            .containsEntry("attemptId", 9002L)
            .containsEntry("attemptMode", AttemptMode.SELF)
            .containsEntry("answerItemCount", 0);

        Map<String, Object> assignedPayload = factory.payloadAfter(assignedAttempt, "save_or_replace", 801L, 2, 77L);
        assertThat(assignedPayload)
            .containsEntry("mutationAction", "save_or_replace")
            .containsEntry("questionId", 801L)
            .containsEntry("answerItemCount", 2)
            .containsEntry("assignmentId", 77L)
            .containsKey("attempt");
        @SuppressWarnings("unchecked")
        Map<String, Object> assignedAttemptPayload = (Map<String, Object>) assignedPayload.get("attempt");
        assertThat(assignedAttemptPayload)
            .containsEntry("id", 9001L)
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("attemptMode", AttemptMode.ASSIGNED)
            .containsEntry("status", TestAttemptStatus.IN_PROGRESS);

        Map<String, Object> selfPayload = factory.payloadAfter(selfAttempt, "clear", 802L, 0, null);
        assertThat(selfPayload)
            .containsEntry("mutationAction", "clear")
            .containsEntry("questionId", 802L)
            .containsEntry("answerItemCount", 0)
            .doesNotContainKey("assignmentId")
            .containsKey("attempt");
    }
}
