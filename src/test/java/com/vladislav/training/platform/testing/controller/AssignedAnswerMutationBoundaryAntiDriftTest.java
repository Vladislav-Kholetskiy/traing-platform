package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Не даёт {@code AssignedAnswerMutationBoundary} незаметно уйти от текущего замысла.
 * Тест страхует важные ограничения и исходную идею решения.
 */
class AssignedAnswerMutationBoundaryAntiDriftTest {

    @Test
    void assignedAnswerMutationFlowMustDelegateIntoActorScopedMutationCoreSavePath() throws Exception {
        assertThat(normalize(read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptAnswerMutationEntryService.java")))
            
            .contains("activeAttemptAnswerMutationService.saveOrReplaceAnswer(\n            actorUserId,\n            testAttemptId,\n            questionId,")
            .doesNotContain("activeAttemptAnswerMutationService.saveOrReplaceAnswer(\n            testAttemptId,\n            questionId,");
    }

    @Test
    void assignedAnswerMutationFlowMustDelegateIntoActorScopedMutationCoreClearPath() throws Exception {
        assertThat(normalize(read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptAnswerMutationEntryService.java")))
            
            .contains("activeAttemptAnswerMutationService.clearAnswer(actorUserId, testAttemptId, questionId, now)")
            .doesNotContain("activeAttemptAnswerMutationService.clearAnswer(testAttemptId, questionId, now)");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private String normalize(String source) {
        return source.replace("\r\n", "\n");
    }
}
