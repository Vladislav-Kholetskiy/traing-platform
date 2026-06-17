package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.AssignedCurrentAttemptReadFoundationStateReadService;
import com.vladislav.training.platform.testing.admission.AssignedAttemptSubmitAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.admission.SelfCurrentAttemptReadFoundationStateReadService;
import com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.controller.AssignedAttemptAnswerMutationController;
import com.vladislav.training.platform.testing.controller.AssignedAttemptEntryController;
import com.vladislav.training.platform.testing.controller.AssignedAttemptSubmitController;
import com.vladislav.training.platform.testing.controller.CurrentAttemptReadController;
import com.vladislav.training.platform.testing.controller.SelfAttemptAnswerMutationController;
import com.vladislav.training.platform.testing.controller.SelfAttemptAbandonController;
import com.vladislav.training.platform.testing.controller.SelfAttemptEntryController;
import com.vladislav.training.platform.testing.controller.SelfAttemptSubmitController;
import com.vladislav.training.platform.testing.controller.SelfVisibleTestingReadController;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationRequest;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptSubmitResponse;
import com.vladislav.training.platform.testing.controller.dto.CurrentAttemptResponse;
import com.vladislav.training.platform.testing.service.AssignedAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptAbandonResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptSubmitResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestCatalogEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestResponse;
import com.vladislav.training.platform.testing.service.SelfAttemptAnswerMutationEntryService;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
/**
 * Собирает набор регрессионных проверок вокруг {@code TestingPackageTopologyAndCrossSliceDependencyHardening}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class TestingPackageTopologyAndCrossSliceDependencyHardeningRegressionPackTest {

    private static final String TESTING_SERVICE_PACKAGE = "com.vladislav.training.platform.testing.service";
    private static final String TESTING_CONTROLLER_PACKAGE = "com.vladislav.training.platform.testing.controller";
    private static final String TESTING_CONTROLLER_DTO_PACKAGE = "com.vladislav.training.platform.testing.controller.dto";

    @Test
    void testingPackagesDoNotExposeGenericExecutionCoordinatorManagerFacadePipelineOrHubTypes() {
        Set<String> topLevelTypeNames = new LinkedHashSet<>();
        topLevelTypeNames.addAll(discoveredTopLevelTypeNames(TESTING_SERVICE_PACKAGE));
        topLevelTypeNames.addAll(discoveredTopLevelTypeNames(TESTING_CONTROLLER_PACKAGE));
        topLevelTypeNames.addAll(discoveredTopLevelTypeNames(TESTING_CONTROLLER_DTO_PACKAGE));

        assertThat(topLevelTypeNames)
            .doesNotContain(
                "ExecutionCoordinator",
                "AttemptManager",
                "TestingFacade",
                "AttemptFacade",
                "ExecutionPipeline",
                "CompletionCoordinator",
                "AttemptLifecycleManager",
                "UniversalSubmitPipeline",
                "AttemptOrchestrator",
                "TestingApplicationService",
                "ExecutionCompletionCoordinator",
                "TestingExecutionSubsystem"
            );
    }

    @Test
    void testingTopologyRemainsScenarioSplitAcrossCanonicalServiceControllerAndDtoClusters() {
        assertThat(discoveredTopLevelTypeNames(TESTING_SERVICE_PACKAGE))
            .contains(
                "SelfVisibleTestingReadService",
                "ActiveAttemptOwnerLocalReadService",
                "AssignedCurrentAttemptReadService",
                "ActiveAttemptAnswerMutationService",
                "AssignedAttemptAnswerMutationEntryService",
                "AssignedAttemptEntryService",
                "SelfAttemptAnswerMutationEntryService",
                "SelfAttemptEntryService",
                "AssignedAttemptSubmissionService",
                "AssignedAttemptSubmitSequencingService",
                "AssignedAttemptSubmitTerminalService",
                "SelfCurrentAttemptReadService",
                "SelfAttemptSubmitSequencingService",
                "SelfAttemptSubmitTerminalService",
                "SelfAttemptAbandonSequencingService",
                "SelfAttemptAbandonTerminalService",
                "AssignedAttemptExpiryTerminalService"
            )
            .doesNotContain(
                "ExecutionMonolithService",
                "AttemptExecutionManager",
                "UnifiedExecutionFacade",
                "TestingFacade"
            );

        assertThat(productionTopLevelJavaNames("src/main/java/com/vladislav/training/platform/testing/controller"))
            .containsExactlyInAnyOrder(
                "AssignedAttemptAnswerMutationController",
                "AssignedAttemptEntryController",
                "AssignedAttemptSubmitController",
                "CurrentAttemptReadController",
                "SelfAttemptAnswerMutationController",
                "SelfAttemptAbandonController",
                "SelfAttemptEntryController",
                "SelfAttemptSubmitController",
                "SelfVisibleTestingReadController"
            );

        assertThat(productionTopLevelJavaNames("src/main/java/com/vladislav/training/platform/testing/controller/dto"))
            .contains(
                "SelfVisibleTestCatalogEntryResponse",
                "SelfVisibleTestResponse",
                "CurrentAttemptResponse",
                "ActiveAttemptAnswerMutationRequest",
                "ActiveAttemptAnswerMutationResponse",
                "AssignedAttemptEntryResponse",
                "SelfAttemptEntryResponse",
                "AssignedAttemptSubmitResponse",
                "SelfAttemptSubmitResponse",
                "SelfAttemptAbandonResponse"
            )
            .doesNotContain(
                "AttemptRequest",
                "AttemptResponse",
                "ExecutionRequest",
                "ExecutionResponse"
            );
    }

    @Test
    void crossSliceDirectDependenciesRemainNarrowWithoutHiddenCentralExecutionClass() {
        assertThat(fieldTypes(SelfVisibleTestingReadController.class))
            .containsExactly(SelfVisibleTestingReadService.class);
        assertThat(fieldTypes(CurrentAttemptReadController.class))
            .containsExactlyInAnyOrder(
                AssignedCurrentAttemptReadService.class,
                SelfCurrentAttemptReadService.class,
                InteractiveActorResolver.class
            );
        assertThat(fieldTypes(AssignedCurrentAttemptReadService.class))
            .contains(
                AssignedCurrentAttemptReadFoundationStateReadService.class,
                ActiveAttemptOwnerLocalReadService.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class
            )
            .doesNotContain(
                ResultRecordingService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class
            );
        assertThat(fieldTypes(SelfCurrentAttemptReadService.class))
            .contains(
                SelfCurrentAttemptReadFoundationStateReadService.class,
                ActiveAttemptOwnerLocalReadService.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class
            )
            .doesNotContain(
                ResultRecordingService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class
            );
        assertThat(fieldTypes(SelfAttemptAnswerMutationController.class))
            .containsExactly(SelfAttemptAnswerMutationEntryService.class);
        assertThat(fieldTypes(AssignedAttemptAnswerMutationController.class))
            .containsExactly(AssignedAttemptAnswerMutationEntryService.class);
        assertThat(fieldTypes(AssignedAttemptEntryController.class)).containsExactly(AssignedAttemptEntryService.class);
        assertThat(fieldTypes(SelfAttemptEntryController.class)).containsExactly(SelfAttemptEntryService.class);
        assertThat(fieldTypes(AssignedAttemptSubmitController.class)).containsExactly(AssignedAttemptSubmissionService.class);
        assertThat(fieldTypes(SelfAttemptSubmitController.class)).containsExactly(SelfAttemptSubmitSequencingService.class);
        assertThat(fieldTypes(SelfAttemptAbandonController.class)).containsExactly(SelfAttemptAbandonSequencingService.class);

        assertThat(fieldTypes(SelfVisibleTestingReadService.class))
            .doesNotContain(
                ActiveAttemptOwnerLocalReadService.class,
                ActiveAttemptAnswerMutationService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                AssignedAttemptExpiryTerminalService.class
            );

        assertThat(fieldTypes(ActiveAttemptOwnerLocalReadService.class))
            .doesNotContain(
                ActiveAttemptAnswerMutationService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                AssignedAttemptExpiryTerminalService.class
            );

        assertThat(fieldTypes(ActiveAttemptAnswerMutationService.class))
            .doesNotContain(
                ActiveAttemptOwnerLocalReadService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                AssignedAttemptExpiryTerminalService.class
            );

        assertThat(fieldTypes(AssignedAttemptEntryService.class))
            .doesNotContain(
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                AssignedAttemptExpiryTerminalService.class,
                ResultRecordingService.class
            );

        assertThat(fieldTypes(AssignedAttemptAnswerMutationEntryService.class))
            .contains(
                ActiveAttemptAnswerMutationService.class,
                AttemptStatusRecalculationService.class
            )
            .doesNotContain(
                SelfAttemptAnswerMutationEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                AssignedAttemptExpiryTerminalService.class,
                ResultRecordingService.class
            );

        assertThat(fieldTypes(SelfAttemptAnswerMutationEntryService.class))
            .contains(ActiveAttemptAnswerMutationService.class)
            .doesNotContain(
                AttemptStatusRecalculationService.class,
                AssignedAttemptAnswerMutationEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                AssignedAttemptExpiryTerminalService.class,
                ResultRecordingService.class
            );

        assertThat(fieldTypes(SelfAttemptEntryService.class))
            .doesNotContain(
                AssignedAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                AssignedAttemptExpiryTerminalService.class,
                ResultRecordingService.class
            );

        assertThat(fieldTypes(AssignedAttemptSubmissionService.class))
            .containsExactlyInAnyOrder(
                AssignedAttemptSubmitSequencingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                AssignedAttemptSubmitAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptExpiryTerminalService.class,
                ResultRecordingService.class
            );

        assertThat(fieldTypes(AssignedAttemptSubmitSequencingService.class))
            .containsExactlyInAnyOrder(AssignedAttemptSubmitTerminalService.class, ResultRecordingService.class)
            .doesNotContain(
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                AssignedAttemptExpiryTerminalService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class
            );

        assertThat(fieldTypes(SelfAttemptSubmitSequencingService.class))
            .containsExactlyInAnyOrder(
                SelfAttemptSubmitTerminalService.class,
                ResultRecordingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                SelfAttemptTerminalAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                AssignedAttemptExpiryTerminalService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class
            );

        assertThat(fieldTypes(SelfAttemptAbandonSequencingService.class))
            .containsExactlyInAnyOrder(
                SelfAttemptAbandonTerminalService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                SelfAttemptTerminalAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                AssignedAttemptExpiryTerminalService.class,
                ResultRecordingService.class
            );

        assertThat(fieldTypes(AssignedAttemptExpiryTerminalService.class))
            .doesNotContain(ResultRecordingService.class)
            .doesNotContain(
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class
            );
    }

    @Test
    void packageLevelVocabularyDoesNotDriftIntoGenericExecutionSubsystemWording() throws Exception {
        String servicePackageInfo = Files.readString(Path.of("src/main/java/com/vladislav/training/platform/testing/service/package-info.java"));
        String controllerPackageInfo = Files.readString(Path.of("src/main/java/com/vladislav/training/platform/testing/controller/package-info.java"));
        String dtoPackageInfo = Files.readString(Path.of("src/main/java/com/vladislav/training/platform/testing/controller/dto/package-info.java"));

        assertThat(servicePackageInfo)
            .contains("not a single generic execution surface")
            .contains("Assigned entry and self entry remain separate command contours")
            .doesNotContain("execution subsystem", "central execution", "orchestration hub");

        assertThat(controllerPackageInfo)
            .contains("must not become a single generic execution surface")
            .contains("Read adapters and command adapters remain separate")
            .doesNotContain("execution subsystem", "central execution");

        assertThat(dtoPackageInfo)
            .contains("not a home for attempt lifecycle payloads")
            .doesNotContain("execution subsystem", "generic execution grammar", "central execution");
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
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private Set<String> productionTopLevelJavaNames(String directory) {
        try {
            return Files.list(Path.of(directory))
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .map(path -> path.getFileName().toString().replace(".java", ""))
                .filter(name -> !name.equals("package-info"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read production topology directory: " + directory, exception);
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to load discovered testing type: " + className, exception);
        }
    }
}
