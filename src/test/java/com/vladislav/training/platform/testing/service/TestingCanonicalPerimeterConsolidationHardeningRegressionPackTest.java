package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
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
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptAbandonResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptSubmitResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestCatalogEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTopicResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 * Собирает набор регрессионных проверок вокруг {@code TestingCanonicalPerimeterConsolidationHardening}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class TestingCanonicalPerimeterConsolidationHardeningRegressionPackTest {

    private static final String TESTING_SERVICE_DIRECTORY =
        "src/main/java/com/vladislav/training/platform/testing/service";
    private static final String TESTING_CONTROLLER_DIRECTORY =
        "src/main/java/com/vladislav/training/platform/testing/controller";
    private static final String TESTING_DTO_DIRECTORY =
        "src/main/java/com/vladislav/training/platform/testing/controller/dto";

    private static final Set<String> EXPECTED_CANONICAL_PUBLIC_SERVICE_SURFACES = Set.of(
        "ActiveAttemptAnswerMutationService",
        "ActiveAttemptOwnerLocalReadService",
        "AssignedCurrentAttemptReadService",
        "AssignedAttemptAnswerMutationEntryService",
        "AssignedAttemptEntryService",
        "AssignedAttemptExpiryTerminalService",
        "AssignedAttemptSubmissionService",
        "AssignedAttemptSubmitSequencingService",
        "AssignedAttemptSubmitTerminalService",
        "SelfAttemptAnswerMutationEntryService",
        "SelfAttemptAbandonSequencingService",
        "SelfAttemptAbandonTerminalService",
        "SelfCurrentAttemptReadService",
        "SelfAttemptEntryService",
        "SelfAttemptSubmitSequencingService",
        "SelfAttemptSubmitTerminalService",
        "SelfVisibleTestingReadService"
    );

    private static final Set<String> EXPECTED_ALLOWED_SUPPORTING_SERVICE_TYPES = Set.of(
        "AssignedAttemptAnswerMutationAdmissionSupport",
        "AssignedAttemptAdmissionSupport",
        "AttemptAnswerMutationCriticalAuditCatalog",
        "AttemptAnswerMutationCriticalAuditPayloadFactory",
        "AssignedAttemptEntryCriticalAuditCatalog",
        "AssignedAttemptEntryCriticalAuditPayloadFactory",
        "AttemptStatusRecalculationService",
        "AttemptStatusRecalculationServiceImpl",
        "AttemptTerminalCriticalAuditCatalog",
        "AttemptTerminalCriticalAuditPayloadFactory",
        "AttemptTerminalizationOutcome",
        "AttemptTerminalizationReason",
        "RepositoryBackedSelfVisibleTestingProjectionReader",
        "SelfAttemptAnswerMutationAdmissionSupport",
        "SelfAttemptAdmissionSupport",
        "SelfAttemptEntryCriticalAuditCatalog",
        "SelfAttemptEntryCriticalAuditPayloadFactory",
        "SelfVisibleTestCatalogEntryReadModel",
        "SelfVisibleTestingProjectionReader",
        "SelfVisibleTestReadModel",
        "SelfVisibleTopicReadModel",
        "SelfVisibleTestVisibilityFilter"
    );

    private static final Set<String> EXPECTED_TESTING_CONTROLLER_TYPES = Set.of(
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

    private static final Set<String> EXPECTED_TESTING_DTO_TYPES = Set.of(
        "ActiveAttemptAnswerMutationRequest",
        "ActiveAttemptAnswerMutationResponse",
        "AssignedAttemptEntryResponse",
        "AssignedAttemptSubmitResponse",
        "CurrentAttemptResponse",
        "SelfAttemptAbandonResponse",
        "SelfAttemptEntryResponse",
        "SelfAttemptSubmitResponse",
        "SelfVisibleTestCatalogEntryResponse",
        "SelfVisibleTestResponse",
        "SelfVisibleTopicResponse"
    );

    @Test
    void exactProductionServicePackageTopologyEqualsCanonicalPublicSurfacesPlusAllowedSupportingTypes() {
        Set<String> productionServiceTypes = productionTestingServiceTypes();
        Set<String> expectedFullServicePackage = new LinkedHashSet<>();
        expectedFullServicePackage.addAll(EXPECTED_CANONICAL_PUBLIC_SERVICE_SURFACES);
        expectedFullServicePackage.addAll(EXPECTED_ALLOWED_SUPPORTING_SERVICE_TYPES);

        assertThat(productionServiceTypes)
            .containsExactlyInAnyOrderElementsOf(expectedFullServicePackage);
    }

    @Test
    void exactProductionControllerPackageTopologyMatchesCanonicalApiSurfaceSet() {
        assertThat(productionTestingControllerTypes())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_TESTING_CONTROLLER_TYPES);
    }

    @Test
    void exactProductionDtoPackageTopologyMatchesCanonicalExternalContractSet() {
        assertThat(productionTestingDtoTypes())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_TESTING_DTO_TYPES);
    }

    @Test
    void canonicalPublicServicePerimeterRemainsScenarioShapedWhileSupportingTypesStayNonCanonical() {
        assertThat(EXPECTED_CANONICAL_PUBLIC_SERVICE_SURFACES)
            .containsExactlyInAnyOrder(
                "ActiveAttemptAnswerMutationService",
                "ActiveAttemptOwnerLocalReadService",
                "AssignedCurrentAttemptReadService",
                "AssignedAttemptAnswerMutationEntryService",
                "AssignedAttemptEntryService",
                "AssignedAttemptExpiryTerminalService",
                "AssignedAttemptSubmissionService",
                "AssignedAttemptSubmitSequencingService",
                "AssignedAttemptSubmitTerminalService",
                "SelfAttemptAnswerMutationEntryService",
                "SelfAttemptAbandonSequencingService",
                "SelfAttemptAbandonTerminalService",
                "SelfCurrentAttemptReadService",
                "SelfAttemptEntryService",
                "SelfAttemptSubmitSequencingService",
                "SelfAttemptSubmitTerminalService",
                "SelfVisibleTestingReadService"
            )
            .doesNotContain(
                "AttemptStatusRecalculationService",
                "AttemptSubmissionService",
                "ExecutionCoordinator",
                "AttemptManager",
                "TestingFacade",
                "AttemptFacade",
                "ExecutionPipeline",
                "CompletionCoordinator",
                "AttemptLifecycleManager",
                "UniversalSubmitPipeline",
                "AttemptOrchestrator",
                "TestingApplicationService"
            );

        assertThat(EXPECTED_ALLOWED_SUPPORTING_SERVICE_TYPES)
            .contains(
                "AssignedAttemptAnswerMutationAdmissionSupport",
                "SelfAttemptAnswerMutationAdmissionSupport",
                "AttemptStatusRecalculationService"
            )
            .doesNotContainAnyElementsOf(EXPECTED_CANONICAL_PUBLIC_SERVICE_SURFACES)
            .doesNotContain(
                "AttemptSubmissionService",
                "ExecutionCoordinator",
                "AttemptManager",
                "TestingFacade",
                "AttemptFacade",
                "ExecutionPipeline",
                "CompletionCoordinator",
                "AttemptLifecycleManager",
                "UniversalSubmitPipeline",
                "AttemptOrchestrator",
                "TestingApplicationService"
            );
    }

    @Test
    void canonicalPublicSplitAcrossServicesControllersAndApiRootsRemainsExact() {
        assertThat(publicMethodNames(SelfVisibleTestingReadService.class))
            .containsExactlyInAnyOrder("findSelfVisibleTests", "findSelfVisibleTestById", "findSelfVisibleTopicById");
        assertThat(publicMethodNames(ActiveAttemptOwnerLocalReadService.class))
            .containsExactlyInAnyOrder("findActiveAssignedAttemptForActor", "findActiveSelfAttempt");
        assertThat(publicMethodNames(ActiveAttemptAnswerMutationService.class))
            .containsExactlyInAnyOrder("saveOrReplaceAnswer", "clearAnswer");
        assertThat(publicMethodNames(AssignedAttemptAnswerMutationEntryService.class))
            .containsExactlyInAnyOrder("saveOrReplaceAssignedAnswer", "clearAssignedAnswer");
        assertThat(publicMethodNames(AssignedAttemptEntryService.class))
            .containsExactly("enterAssignedAttempt");
        assertThat(publicMethodNames(SelfAttemptAnswerMutationEntryService.class))
            .containsExactlyInAnyOrder("saveOrReplaceSelfAnswer", "clearSelfAnswer");
        assertThat(publicMethodNames(SelfAttemptEntryService.class))
            .containsExactly("startOrContinueSelfAttempt");
        assertThat(publicMethodNames(AssignedAttemptSubmissionService.class))
            .containsExactly("submitAssignedAttempt");
        assertThat(publicMethodNames(AssignedAttemptSubmitSequencingService.class))
            .containsExactly("submitAssignedAttempt");
        assertThat(publicMethodNames(AssignedAttemptSubmitTerminalService.class))
            .containsExactly("submitAssignedAttempt");
        assertThat(publicMethodNames(SelfAttemptSubmitSequencingService.class))
            .containsExactly("submitSelfAttempt");
        assertThat(publicMethodNames(SelfAttemptSubmitTerminalService.class))
            .containsExactly("submitSelfAttempt");
        assertThat(publicMethodNames(SelfAttemptAbandonSequencingService.class))
            .containsExactly("abandonSelfAttempt");
        assertThat(publicMethodNames(SelfAttemptAbandonTerminalService.class))
            .containsExactly("abandonSelfAttempt");
        assertThat(publicMethodNames(AssignedAttemptExpiryTerminalService.class))
            .containsExactly("expireAssignedAttempt");

        assertThat(mappingMethodNames(SelfVisibleTestingReadController.class))
            .containsExactlyInAnyOrder("findSelfVisibleTests", "findSelfVisibleTestById", "findSelfVisibleTopicById");
        assertThat(mappingMethodNames(CurrentAttemptReadController.class))
            .containsExactlyInAnyOrder("findCurrentAssignedAttempt", "findCurrentSelfAttempt");
        assertThat(mappingMethodNames(SelfAttemptAnswerMutationController.class))
            .containsExactlyInAnyOrder("saveOrReplaceSelfAnswer", "clearSelfAnswer");
        assertThat(mappingMethodNames(AssignedAttemptAnswerMutationController.class))
            .containsExactlyInAnyOrder("saveOrReplaceAssignedAnswer", "clearAssignedAnswer");
        assertThat(mappingMethodNames(AssignedAttemptEntryController.class)).containsExactly("enterAssignedAttempt");
        assertThat(mappingMethodNames(SelfAttemptEntryController.class)).containsExactly("startOrContinueSelfAttempt");
        assertThat(mappingMethodNames(AssignedAttemptSubmitController.class)).containsExactly("submitAssignedAttempt");
        assertThat(mappingMethodNames(SelfAttemptSubmitController.class)).containsExactly("submitSelfAttempt");
        assertThat(mappingMethodNames(SelfAttemptAbandonController.class)).containsExactly("abandonSelfAttempt");

        assertThat(apiRoots()).containsExactlyInAnyOrder(
            "/api/v1/self-testing/tests",
            "/api/v1/current-attempts",
            "/api/v1/self-attempt-answers",
            "/api/v1/assigned-attempt-answers",
            "/api/v1/assigned-attempt-entries",
            "/api/v1/self-attempt-entries",
            "/api/v1/assigned-attempt-submissions",
            "/api/v1/self-attempt-submissions",
            "/api/v1/self-attempt-abandonments"
        );
    }

    @Test
    void canonicalExternalDtoContractsRemainDecomposedWithoutUnifiedLifecycleCarrier() {
        assertThat(recordComponentNames(ActiveAttemptAnswerMutationRequest.class))
            .containsExactly("answerItems")
            .doesNotContain("actionType", "commandType", "operation", "mode", "submissionType");
        assertThat(recordComponentNames(CurrentAttemptResponse.class))
            .containsExactly(
                "id",
                "userId",
                "testId",
                "assignmentTestId",
                "attemptMode",
                "status",
                "startedAt",
                "completedAt",
                "expiredAt",
                "abandonedAt",
                "lastActivityAt"
            )
            .doesNotContain("resultId");
        assertThat(recordComponentNames(AssignedAttemptEntryResponse.class))
            .containsExactly("testAttemptId", "assignmentTestId", "testId", "attemptMode", "status", "startedAt", "lastActivityAt")
            .doesNotContain("completedAt", "expiredAt", "abandonedAt", "resultId");
        assertThat(recordComponentNames(SelfAttemptEntryResponse.class))
            .containsExactly("testAttemptId", "testId", "attemptMode", "status", "startedAt", "lastActivityAt")
            .doesNotContain("assignmentTestId", "completedAt", "expiredAt", "abandonedAt", "resultId");
        assertThat(recordComponentNames(AssignedAttemptSubmitResponse.class)).containsExactly("testAttemptId", "status", "resultId");
        assertThat(recordComponentNames(SelfAttemptSubmitResponse.class)).containsExactly("testAttemptId", "resultId");
        assertThat(recordComponentNames(SelfAttemptAbandonResponse.class)).containsExactly("testAttemptId");
        assertThat(recordComponentNames(SelfVisibleTestCatalogEntryResponse.class))
            .containsExactly("id", "courseId", "courseName", "topicId", "topicName", "name", "description", "testType");
        assertThat(recordComponentNames(SelfVisibleTestResponse.class))
            .containsExactly("id", "topicId", "name", "description", "testType", "questions");
        assertThat(recordComponentNames(SelfVisibleTopicResponse.class))
            .containsExactly("topicId", "topicName", "topicDescription", "courseId", "courseName", "materials");
    }

    @Test
    void canonicalPublicServiceControllerAndDtoSourcesRejectGenericExecutionDriftGrammar() throws Exception {
        Map<String, String> serviceSources = readProductionSourcesByFileName(TESTING_SERVICE_DIRECTORY, productionTestingServiceTypes());
        Map<String, String> controllerSources = readProductionSourcesByFileName(TESTING_CONTROLLER_DIRECTORY, productionTestingControllerTypes());
        Map<String, String> dtoSources = readProductionSourcesByFileName(TESTING_DTO_DIRECTORY, productionTestingDtoTypes());
        Map<String, String> packageInfoSources = Map.of(
            "service/package-info.java", readSource(TESTING_SERVICE_DIRECTORY + "/package-info.java"),
            "controller/package-info.java", readSource(TESTING_CONTROLLER_DIRECTORY + "/package-info.java"),
            "dto/package-info.java", readSource(TESTING_DTO_DIRECTORY + "/package-info.java")
        );
        String combinedProductionSources = joinSourceValues(serviceSources, controllerSources, dtoSources);

        assertThat(combinedProductionSources)
            .doesNotContain(
                "submitAttempt(",
                "completeAttempt(",
                "executeAttemptAction(",
                "handleAttemptLifecycle(",
                "mutateAttempt(",
                "attemptCommand(",
                "actionType",
                "submissionType",
                "mode=",
                "enum Action",
                "enum CommandType",
                "enum Operation",
                "SELF|ASSIGNED",
                "execution subsystem",
                "single execution surface",
                "orchestration hub",
                "central execution"
            );

        assertThat(joinSourceValues(packageInfoSources))
            .doesNotContain(
                "commandType",
                "operation",
                "submissionType",
                "actionType",
                "mode=",
                "enum Action",
                "enum CommandType",
                "enum Operation",
                "SELF|ASSIGNED",
                "execution subsystem",
                "single execution surface",
                "orchestration hub",
                "central execution"
            );

        assertThat(filesContainingToken(serviceSources, "commandType"))
            .containsExactlyInAnyOrder(
                "AssignedAttemptEntryCriticalAuditPayloadFactory.java",
                "AttemptAnswerMutationCriticalAuditPayloadFactory.java",
                "AttemptTerminalCriticalAuditPayloadFactory.java",
                "SelfAttemptEntryCriticalAuditPayloadFactory.java"
            );
        assertThat(filesContainingToken(controllerSources, "commandType")).isEmpty();
        assertThat(filesContainingToken(dtoSources, "commandType")).isEmpty();

        assertThat(filesContainingToken(serviceSources, "operation"))
            .containsExactlyInAnyOrder(
                "AssignedAttemptAnswerMutationEntryService.java",
                "AssignedAttemptEntryService.java",
                "AssignedAttemptEntryCriticalAuditCatalog.java",
                "AssignedAttemptSubmitTerminalService.java",
                "AssignedAttemptExpiryTerminalService.java",
                "AttemptAnswerMutationCriticalAuditCatalog.java",
                "AttemptTerminalCriticalAuditCatalog.java",
                "SelfAttemptAbandonTerminalService.java",
                "SelfAttemptAnswerMutationEntryService.java",
                "SelfAttemptEntryCriticalAuditCatalog.java",
                "SelfAttemptEntryService.java",
                "SelfAttemptSubmitTerminalService.java"
            );
        assertThat(filesContainingToken(controllerSources, "operation")).isEmpty();
        assertThat(filesContainingToken(dtoSources, "operation")).isEmpty();

        assertThat(filesContainingToken(serviceSources, "generic execution facade")).isEmpty();
        assertThat(filesContainingToken(controllerSources, "generic execution facade")).isEmpty();
        assertThat(filesContainingToken(dtoSources, "generic execution facade")).isEmpty();
        assertThat(filesContainingToken(packageInfoSources, "generic execution facade")).isEmpty();
    }

    @Test
    void controllerBoundaryRejectsOwnerPolicyAuditAndAdminDrift() {
        Set<Class<?>> controllerFieldTypes = expectedControllerClasses().stream()
            .flatMap(controller -> fieldTypes(controller).stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(controllerFieldTypes)
            .doesNotContain(
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentCommandService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                AssignmentCampaignCommandService.class
            );

        assertThat(fieldTypes(CurrentAttemptReadController.class))
            .containsExactlyInAnyOrder(
                AssignedCurrentAttemptReadService.class,
                SelfCurrentAttemptReadService.class,
                InteractiveActorResolver.class
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
        assertThat(fieldTypes(SelfVisibleTestingReadController.class)).containsExactly(SelfVisibleTestingReadService.class);
    }

    @Test
    void assignedSubmitControllerRemainsOutcomeDrivenThinAdapterWithoutResultOrCountedDependencies() {
        String controllerSource = readSource(
            "src/main/java/com/vladislav/training/platform/testing/controller/AssignedAttemptSubmitController.java"
        );

        assertThat(controllerSource)
            .contains("outcome.attemptId()")
            .contains("outcome.terminalStatus()")
            .contains("outcome.recordedResult()")
            .doesNotContain("new AssignedAttemptSubmitResponse(testAttemptId,")
            .doesNotContain(".recordResult(")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentCountedResultHandoffService");
    }

    @Test
    void topologyRejectsHiddenCentralMergeThroughOrchestrationAndDtoRoleCollapse() {
        assertThat(fieldTypes(AssignedCurrentAttemptReadService.class))
            .contains(
                com.vladislav.training.platform.testing.admission.AssignedCurrentAttemptReadFoundationStateReadService.class,
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
                com.vladislav.training.platform.testing.admission.SelfCurrentAttemptReadFoundationStateReadService.class,
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
        assertThat(fieldTypes(AssignedAttemptSubmissionService.class))
            .containsExactlyInAnyOrder(
                AssignedAttemptSubmitSequencingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                com.vladislav.training.platform.testing.admission.AssignedAttemptSubmitAdmissionFoundationStateReadService.class
            );
        assertThat(fieldTypes(AssignedAttemptSubmitSequencingService.class))
            .containsExactlyInAnyOrder(AssignedAttemptSubmitTerminalService.class, ResultRecordingService.class)
            .doesNotContain(
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptExpiryTerminalService.class
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
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                AssignedAttemptExpiryTerminalService.class
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
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptSubmitSequencingService.class,
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

    private Set<String> productionTestingServiceTypes() {
        return productionTopLevelJavaNames(TESTING_SERVICE_DIRECTORY);
    }

    private Set<String> productionTestingControllerTypes() {
        return productionTopLevelJavaNames(TESTING_CONTROLLER_DIRECTORY);
    }

    private Set<String> productionTestingDtoTypes() {
        return productionTopLevelJavaNames(TESTING_DTO_DIRECTORY);
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

    private String readProductionSources(String directory, Set<String> typeNames) {
        return typeNames.stream()
            .sorted()
            .map(typeName -> readSource(directory + "/" + typeName + ".java"))
            .collect(Collectors.joining("\n"));
    }

    private Map<String, String> readProductionSourcesByFileName(String directory, Set<String> typeNames) {
        Map<String, String> sourcesByFileName = new LinkedHashMap<>();
        typeNames.stream()
            .sorted()
            .forEach(typeName -> sourcesByFileName.put(typeName + ".java", readSource(directory + "/" + typeName + ".java")));
        return sourcesByFileName;
    }

    private String joinSourceValues(Map<String, String>... sourcesByScope) {
        return Arrays.stream(sourcesByScope)
            .flatMap(scope -> scope.values().stream())
            .collect(Collectors.joining("\n"));
    }

    private Set<String> filesContainingToken(Map<String, String> sourcesByFileName, String token) {
        return sourcesByFileName.entrySet().stream()
            .filter(entry -> entry.getValue().contains(token))
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Class<?>> expectedControllerClasses() {
        return Set.of(
            SelfVisibleTestingReadController.class,
            CurrentAttemptReadController.class,
            SelfAttemptAnswerMutationController.class,
            AssignedAttemptAnswerMutationController.class,
            AssignedAttemptEntryController.class,
            SelfAttemptEntryController.class,
            AssignedAttemptSubmitController.class,
            SelfAttemptSubmitController.class,
            SelfAttemptAbandonController.class
        );
    }

    private Set<String> apiRoots() {
        return expectedControllerClasses().stream()
            .flatMap(controller -> Arrays.stream(controller.getAnnotation(RequestMapping.class).value()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> publicMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> mappingMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(this::isRequestHandler)
            .map(Method::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isRequestHandler(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
            || method.isAnnotationPresent(PostMapping.class)
            || method.isAnnotationPresent(PutMapping.class)
            || method.isAnnotationPresent(DeleteMapping.class);
    }

    private List<String> recordComponentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private String readSource(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read source: " + path, exception);
        }
    }

}
