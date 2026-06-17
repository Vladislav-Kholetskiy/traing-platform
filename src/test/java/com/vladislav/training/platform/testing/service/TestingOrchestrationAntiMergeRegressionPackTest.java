package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.DefaultCapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.controller.AssignmentAdministrativeActionController;
import com.vladislav.training.platform.assignment.controller.AssignmentCampaignLaunchController;
import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeActionService;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.testing.controller.SelfVisibleTestingReadController;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
/**
 * Собирает набор регрессионных проверок вокруг {@code TestingOrchestrationAntiMerge}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class TestingOrchestrationAntiMergeRegressionPackTest {

    private static final String TESTING_SERVICE_PACKAGE = "com.vladislav.training.platform.testing.service";

    @Test
    void orchestrationLayerDoesNotExposeGenericExecutionManagerVocabulary() {
        assertThat(discoveredTopLevelTypeNames(TESTING_SERVICE_PACKAGE))
            .doesNotContain(
                "AttemptExecutionOrchestrationService",
                "AttemptCommandCoordinator",
                "SubmitOrCloseManager",
                "AttemptLifecycleOrchestrator",
                "UnifiedAttemptCommandService",
                "ExecutionOrchestrationFacade",
                "AttemptSubmissionManager"
            );

        assertThat(publicMethodNames(AssignedAttemptSubmissionService.class))
            .containsExactly("submitAssignedAttempt")
            .doesNotContain("submitAttempt", "closeAttempt", "abandonAttempt", "resumeAttempt");

        assertThat(publicMethodNames(AssignedAttemptSubmitSequencingService.class))
            .containsExactly("submitAssignedAttempt")
            .doesNotContain("submitAttempt", "closeAttempt", "abandonAttempt", "submitSelfAttempt");

        assertThat(publicMethodNames(SelfAttemptSubmitSequencingService.class))
            .containsExactly("submitSelfAttempt")
            .doesNotContain("submitAttempt", "closeAttempt", "abandonAttempt", "submitAssignedAttempt");

        assertThat(publicMethodNames(SelfAttemptAbandonSequencingService.class))
            .containsExactly("abandonSelfAttempt")
            .doesNotContain("submitAttempt", "closeAttempt", "submitAssignedAttempt", "submitSelfAttempt");
    }

    @Test
    void assignedSubmitOrchestrationPathDoesNotDependOnSelfSubmitOrAbandonOwners() {
        assertThat(fieldTypes(AssignedAttemptSubmissionService.class))
            .doesNotContain(
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonTerminalService.class,
                SelfAttemptEntryService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class
            );

        assertThat(fieldTypes(AssignedAttemptSubmitSequencingService.class))
            .doesNotContain(
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonTerminalService.class,
                SelfAttemptEntryService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class
            );
    }

    @Test
    void selfSubmitOrchestrationPathDoesNotDependOnAssignedSubmitOrSelfAbandonOwners() {
        assertThat(fieldTypes(SelfAttemptSubmitSequencingService.class))
            .doesNotContain(
                AssignedAttemptSubmitTerminalService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmissionService.class,
                SelfAttemptAbandonTerminalService.class,
                SelfAttemptAbandonSequencingService.class
            );
    }

    @Test
    void selfAbandonOrchestrationPathDoesNotDependOnSubmitOrResultOwnersThatDoNotBelongToIt() {
        assertThat(fieldTypes(SelfAttemptAbandonSequencingService.class))
            .doesNotContain(
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmissionService.class,
                ResultRecordingService.class
            );
    }

    @Test
    void orchestrationLayerDoesNotOwnResultOrAssignmentSideEffectsDirectly() {
        assertThat(fieldTypes(AssignedAttemptSubmissionService.class))
            .containsExactlyInAnyOrder(
                AssignedAttemptSubmitSequencingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                com.vladislav.training.platform.testing.admission.AssignedAttemptSubmitAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                ResultRepository.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentStatusRecalculationService.class,
                TestAttemptRepository.class
            );

        assertThat(fieldTypes(AssignedAttemptSubmitSequencingService.class))
            .containsExactlyInAnyOrder(
                AssignedAttemptSubmitTerminalService.class,
                ResultRecordingService.class
            )
            .doesNotContain(
                ResultRepository.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentStatusRecalculationService.class,
                TestAttemptRepository.class
            );

        assertThat(fieldTypes(SelfAttemptSubmitSequencingService.class))
            .containsExactlyInAnyOrder(
                SelfAttemptSubmitTerminalService.class,
                ResultRecordingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                ResultRepository.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentStatusRecalculationService.class,
                TestAttemptRepository.class
            );

        assertThat(fieldTypes(SelfAttemptAbandonSequencingService.class))
            .containsExactlyInAnyOrder(
                SelfAttemptAbandonTerminalService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                ResultRepository.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentStatusRecalculationService.class,
                TestAttemptRepository.class
            );
    }

    @Test
    void orchestrationLayerDoesNotPullControllerAuditOrPolicyContours() {
        assertThat(publicMethodNames(AssignedAttemptSubmissionService.class))
            .doesNotContain("abandonSelfAttempt", "expireAssignedAttempt", "terminalizeAttempt", "manageTerminalState");

        assertThat(publicMethodNames(AssignedAttemptSubmitSequencingService.class))
            .doesNotContain("abandonSelfAttempt", "expireAssignedAttempt", "terminalizeAttempt", "manageTerminalState");

        assertThat(publicMethodNames(SelfAttemptSubmitSequencingService.class))
            .doesNotContain("abandonSelfAttempt", "expireAssignedAttempt", "terminalizeAttempt", "manageTerminalState");

        assertThat(publicMethodNames(SelfAttemptAbandonSequencingService.class))
            .doesNotContain("submitSelfAttempt", "submitAssignedAttempt", "expireAssignedAttempt", "terminalizeAttempt");

        assertThat(fieldTypes(AssignedAttemptSubmissionService.class))
            .doesNotContain(
                DefaultCapabilityAdmissionPolicy.class,
                AssignmentAdministrativeActionService.class,
                AssignmentCampaignCommandService.class,
                AssignmentAdministrativeActionController.class,
                AssignmentCampaignLaunchController.class,
                SelfVisibleTestingReadController.class
            );

        assertThat(fieldTypes(AssignedAttemptSubmitSequencingService.class))
            .doesNotContain(
                CriticalCommandAuditSupport.class,
                DefaultCapabilityAdmissionPolicy.class,
                AssignmentAdministrativeActionService.class,
                AssignmentCampaignCommandService.class,
                AssignmentAdministrativeActionController.class,
                AssignmentCampaignLaunchController.class,
                SelfVisibleTestingReadController.class
            );

        assertThat(fieldTypes(SelfAttemptSubmitSequencingService.class))
            .doesNotContain(
                DefaultCapabilityAdmissionPolicy.class,
                AssignmentAdministrativeActionService.class,
                AssignmentCampaignCommandService.class,
                AssignmentAdministrativeActionController.class,
                AssignmentCampaignLaunchController.class,
                SelfVisibleTestingReadController.class
            );

        assertThat(fieldTypes(SelfAttemptAbandonSequencingService.class))
            .doesNotContain(
                DefaultCapabilityAdmissionPolicy.class,
                AssignmentAdministrativeActionService.class,
                AssignmentCampaignCommandService.class,
                AssignmentAdministrativeActionController.class,
                AssignmentCampaignLaunchController.class,
                SelfVisibleTestingReadController.class
            );
    }

    @Test
    void stage10OrchestrationIsMaterializedAsThreeSeparateSurfacesWithoutGenericMerge() {
        assertThat(discoveredTopLevelTypeNames(TESTING_SERVICE_PACKAGE))
            .contains(
                "AssignedAttemptSubmissionService",
                "AssignedAttemptSubmitSequencingService",
                "SelfAttemptSubmitSequencingService",
                "SelfAttemptAbandonSequencingService"
            )
            .doesNotContain(
                "AttemptOrchestrationFacade",
                "AttemptExecutionManager",
                "SubmissionManager",
                "CloseManager",
                "UnifiedExecutionFacade"
            );
    }

    private Set<String> discoveredTopLevelTypeNames(String packageName) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isIndependent();
            }
        };
        scanner.addIncludeFilter(new RegexPatternTypeFilter(java.util.regex.Pattern.compile(".*")));

        return scanner.findCandidateComponents(packageName).stream()
            .map(beanDefinition -> beanDefinition.getBeanClassName())
            .filter(className -> !className.endsWith(".package-info"))
            .map(this::loadClass)
            .filter(type -> !type.getName().contains("$"))
            .map(Class::getSimpleName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private List<String> publicMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList();
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to load discovered orchestration type: " + className, exception);
        }
    }
}

