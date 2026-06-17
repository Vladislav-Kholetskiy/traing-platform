package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.service.AssignmentSelfScopedQueryService;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.SelfAttemptEntryFoundationStateReadService;
import com.vladislav.training.platform.testing.admission.SelfExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitTerminalService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code CurrentAttemptReadCommandSeparation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class CurrentAttemptReadCommandSeparationRegressionTest {

    @Test
    void currentAttemptReadBoundaryMustUseDedicatedAssignedFoundationReadSeamInsteadOfBroadAssignmentContextMaterialization() {
        assertThat(fieldTypes(CurrentAttemptReadController.class))
            .contains(AssignedCurrentAttemptReadService.class)
            .contains(SelfCurrentAttemptReadService.class)
            .doesNotContain(
                AssignmentSelfScopedQueryService.class,
                SelfAttemptEntryFoundationStateReadService.class,
                SelfExecutionAdmissionFoundationStateReadService.class
            );
    }

    @Test
    void currentAttemptReadSourceMustNotDelegateIntoStartMutationSubmitOrResultPaths() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/controller/CurrentAttemptReadController.java"))
            .doesNotContain("findAssignedLearningContext(")
            .doesNotContain("findSelfAttemptEntryFoundationState(")
            .doesNotContain("findSelfExecutionAdmissionFoundationState(")
            .doesNotContain("findAssignedCurrentAttemptReadFoundationState(")
            .doesNotContain("findSelfCurrentAttemptReadFoundationState(")
            .doesNotContain("startOrContinueAssignedAttempt(")
            .doesNotContain("startOrContinueSelfAttempt(")
            .doesNotContain("saveOrReplaceAnswer(")
            .doesNotContain("clearAnswer(")
            .doesNotContain("submitAssignedAttempt(")
            .doesNotContain("submitSelfAttempt(")
            .doesNotContain("abandonSelfAttempt(")
            .doesNotContain("recordResult(");
    }

    @Test
    void currentAttemptReadBoundaryMustNotDependOnCommandOrMutationServices() {
        assertThat(fieldTypes(CurrentAttemptReadController.class))
            .doesNotContain(
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                ActiveAttemptAnswerMutationService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                ResultRecordingService.class
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
