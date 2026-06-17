package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, как {@code ResultRecordingCriticalAuditPayload} создаёт данные и вспомогательные объекты.
 * Это помогает не сломать начальную сборку данных.
 */
class ResultRecordingCriticalAuditPayloadFactoryTest {

    private final ResultRecordingCriticalAuditPayloadFactory factory = new ResultRecordingCriticalAuditPayloadFactory();

    @Test
    void createsNarrowRecordingDetailsAndPayloadAfterForNewlyMaterializedResult() {
        Result result = recordedResult();

        Map<String, Object> details = factory.createRecordingDetails(result, TestAttemptStatus.COMPLETED, "interactive");
        Map<String, Object> payloadAfter = factory.payloadAfter(result);

        assertThat(details).containsEntry("commandType", "result_record");
        assertThat(details).containsEntry("recordingKind", "immutable_result_materialization");
        assertThat(details).containsEntry("testAttemptId", 9001L);
        assertThat(details).containsEntry("attemptMode", AttemptMode.ASSIGNED);
        assertThat(details).containsEntry("terminalStatus", TestAttemptStatus.COMPLETED);
        assertThat(details).containsEntry("actorSource", "interactive");
        assertThat(details).containsEntry("assignmentId", 801L);
        assertThat(details).containsEntry("assignmentTestId", 701L);

        assertThat(payloadAfter).containsEntry("id", 401L);
        assertThat(payloadAfter).containsEntry("testAttemptId", 9001L);
        assertThat(payloadAfter).containsEntry("attemptMode", AttemptMode.ASSIGNED);
        assertThat(payloadAfter).containsEntry("assignmentId", 801L);
        assertThat(payloadAfter).containsEntry("assignmentTestId", 701L);
        assertThat(payloadAfter).containsEntry("withinDeadline", true);
        assertThat(payloadAfter).containsEntry("countedInAssignment", false);
        assertThat(payloadAfter).containsEntry("snapshotFinalTopicControlFlag", true);
    }

    private Result recordedResult() {
        return new Result(
            401L,
            9001L,
            3001L,
            AttemptMode.ASSIGNED,
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("8.00"),
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                true,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            true,
            false,
            Instant.parse("2026-04-19T10:15:00Z"),
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            Instant.parse("2026-04-19T10:16:00Z")
        );
    }
}
