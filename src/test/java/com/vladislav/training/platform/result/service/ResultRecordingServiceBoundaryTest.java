package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignCourseRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import com.vladislav.training.platform.testing.service.ActiveAttemptOwnerLocalReadService;
import com.vladislav.training.platform.testing.service.AttemptStatusRecalculationService;
import com.vladislav.training.platform.testing.service.AssignedAttemptExpiryTerminalService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitTerminalService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonTerminalService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitTerminalService;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code ResultRecordingService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class ResultRecordingServiceBoundaryTest {

    @Test
    void resultRecordingImplementationDependsOnlyOnResultOwnerEntryCollaborators() {
        assertThat(fieldTypes(ResultRecordingServiceImpl.class))
            .extracting(Class::getName)
            .containsExactlyInAnyOrder(
                "com.vladislav.training.platform.result.repository.ResultRepository",
                "com.vladislav.training.platform.testing.repository.TestAttemptRepository",
                "com.vladislav.training.platform.result.service.ResultRecordingSnapshotFactsProvider",
                "com.vladislav.training.platform.result.service.ResultRecordingSubordinateSnapshotMaterializer",
                "com.vladislav.training.platform.result.service.ResultRecordingIdempotentReplayValidator",
                "com.vladislav.training.platform.result.service.ResultRecordingChildSnapshotCompletenessValidator",
                "com.vladislav.training.platform.result.service.CountedAssignmentResultValidityGate",
                "com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService",
                "com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport",
                "com.vladislav.training.platform.audit.service.SystemActorResolver",
                "com.vladislav.training.platform.result.domain.ResultSnapshotAssembler",
                "com.vladislav.training.platform.result.service.ResultRecordingCriticalAuditPayloadFactory"
            )
            .doesNotContain(
                AssignedAttemptSubmitTerminalService.class.getName(),
                AssignedAttemptExpiryTerminalService.class.getName(),
                SelfAttemptSubmitTerminalService.class.getName(),
                SelfAttemptAbandonTerminalService.class.getName(),
                ActiveAttemptAnswerMutationService.class.getName(),
                ActiveAttemptOwnerLocalReadService.class.getName(),
                AssignmentCampaignReadRepository.class.getName(),
                AssignmentCampaignRepository.class.getName(),
                AssignmentCampaignCourseRepository.class.getName(),
                AssignmentCampaignRecipientSnapshotRepository.class.getName(),
                AssignmentSelfScopedReadRepository.class.getName(),
                AssignmentTestRepository.class.getName(),
                AssignmentRepository.class.getName(),
                AttemptStatusRecalculationService.class.getName(),
                AssignmentStatusRecalculationService.class.getName()
            );
    }

    @Test
    void subordinateSnapshotMaterializerDependsOnlyOnSnapshotRepositoriesAndCanonicalReadSources() {
        assertThat(fieldTypes(ResultRecordingSubordinateSnapshotMaterializer.class))
            .extracting(Class::getName)
            .containsExactlyInAnyOrder(
                ResultQuestionSnapshotRepository.class.getName(),
                ResultAnswerOptionSnapshotRepository.class.getName(),
                TestQuestionRepository.class.getName(),
                QuestionRepository.class.getName(),
                AnswerOptionRepository.class.getName(),
                UserAnswerRepository.class.getName(),
                UserAnswerItemRepository.class.getName(),
                ResultQuestionScoringEvaluator.class.getName()
            )
            .doesNotContain(
                ResultRepository.class.getName(),
                AssignmentCampaignReadRepository.class.getName(),
                AssignmentCampaignRepository.class.getName(),
                AssignmentCampaignCourseRepository.class.getName(),
                AssignmentCampaignRecipientSnapshotRepository.class.getName(),
                AssignmentSelfScopedReadRepository.class.getName(),
                AssignmentTestRepository.class.getName(),
                AssignmentRepository.class.getName(),
                AttemptStatusRecalculationService.class.getName(),
                AssignmentStatusRecalculationService.class.getName(),
                ActiveAttemptAnswerMutationService.class.getName(),
                ActiveAttemptOwnerLocalReadService.class.getName()
            );
    }

    @Test
    void resultRecordingKeepsCanonicalOwnerEntryVocabularyWithoutMutableManagerDrift() {
        assertThat(Arrays.stream(ResultRecordingServiceImpl.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("recordResult")
            .doesNotContain(
                "findResultByTestAttemptId",
                "refreshResult",
                "updateResult",
                "deleteResult",
                "submitAssignedAttempt",
                "closeAttempt"
            );
    }

    @Test
    void countedAssignmentValidityGateStaysPurelyResultFactBasedWithoutRepositoryDrift() {
        assertThat(fieldTypes(CountedAssignmentResultValidator.class)).isEmpty();

        assertThat(Arrays.stream(CountedAssignmentResultValidator.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("allowsAssignmentCountedHandoff")
            .doesNotContain("findResultById", "findAssignmentById", "refreshAttemptStatusCache");
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}

