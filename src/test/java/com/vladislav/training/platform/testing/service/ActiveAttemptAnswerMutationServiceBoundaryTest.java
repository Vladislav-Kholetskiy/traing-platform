package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code ActiveAttemptAnswerMutationService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class ActiveAttemptAnswerMutationServiceBoundaryTest {

    @Test
    void mutationServiceDependsOnlyOnAttemptAnswerWritesAndNarrowContentOwnershipReads() {
        assertThat(fieldTypes(ActiveAttemptAnswerMutationService.class))
            .containsExactlyInAnyOrder(
                TestAttemptRepository.class,
                TestQuestionRepository.class,
                QuestionRepository.class,
                AnswerOptionRepository.class,
                UserAnswerRepository.class,
                UserAnswerItemRepository.class
            )
            .doesNotContain(
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                ResultRecordingService.class,
                CriticalCommandAuditSupport.class,
                AssignmentStatusRecalculationService.class
            );
    }

    @Test
    void mutationServiceKeepsAnswerMutationVocabularyWithoutLifecycleOrResultDrift() {
        assertThat(Arrays.stream(ActiveAttemptAnswerMutationService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactlyInAnyOrder("saveOrReplaceAnswer", "clearAnswer")
            .doesNotContain(
                "appendAnswerItem",
                "removeSingleItem",
                "patchAnswerItem",
                "mergeAnswerState",
                "startOrContinueAssignedAttempt",
                "startOrContinueSelfAttempt",
                "submitAttempt",
                "expireAssignedAttempt",
                "abandonSelfAttempt",
                "recordResult",
                "recalculateAttemptStatus"
            );
    }

    @Test
    void mutationServiceCanonizesExplicitAttemptLockingForMutationCriticalSection() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/ActiveAttemptAnswerMutationService.java"
        ));

        assertThat(source)
            .contains("findAndLockTestAttemptByIdAndUserId")
            .doesNotContain("findAndLockTestAttemptById(testAttemptId)")
            .doesNotContain("findTestAttemptById");
    }

    @Test
    void queryOnlyReadLayerRemainsSeparateFromAnswerMutationLayer() {
        assertThat(Arrays.stream(ActiveAttemptOwnerLocalReadService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactlyInAnyOrder("findActiveAssignedAttemptForActor", "findActiveSelfAttempt");

        assertThat(fieldTypes(ActiveAttemptOwnerLocalReadService.class))
            .doesNotContain(ActiveAttemptAnswerMutationService.class);
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
