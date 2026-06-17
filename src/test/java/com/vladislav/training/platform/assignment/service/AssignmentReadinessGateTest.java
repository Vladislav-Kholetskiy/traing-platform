package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.assignment.controller.AssignmentAdministrativeActionController;
import com.vladislav.training.platform.assignment.controller.AssignmentCampaignLaunchController;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentReadRepository;
import com.vladislav.training.platform.assignment.infrastructure.scheduler.AssignmentStatusRecalculationScheduler;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.OrganizationalTargetingQueryService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
/**
 * Проверяет поведение {@code AssignmentReadinessGate}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentReadinessGateTest {

    @Test
    void commandPolicyClosureRemainsCarrierCompleteAndWiredThroughCanonicalAdmissionPath() throws Exception {
        assertThat(enumNames(CapabilityOperationCode.class))
            .contains(
                "ASSIGNMENT_CAMPAIGN_LAUNCH",
                "ASSIGNMENT_CANCEL",
                "ASSIGNMENT_DEADLINE_EXTEND",
                "ASSIGNMENT_REPLACE_WITH_NEW"
            );
        assertThat(enumNames(CapabilityTargetEntityType.class))
            .contains("ASSIGNMENT_CAMPAIGN", "ASSIGNMENT");
        assertThat(permittedSubclassNames(CapabilityAdmissionPayload.class))
            .contains(
                "AssignmentCampaignLaunch",
                "AssignmentCancel",
                "AssignmentDeadlineExtend",
                "AssignmentReplaceWithNew"
            );
        assertThat(methodNames(CapabilityAdmissionRequestFactory.class))
            .contains(
                "createAssignmentCampaignLaunch",
                "createAssignmentCancel",
                "createAssignmentDeadlineExtend",
                "createAssignmentReplaceWithNew"
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/application/policy/CapabilityAdmissionFoundationFacts.java"))
            .contains("resolveAssignmentCampaignLaunchFoundationState")
            .contains("resolveAssignmentAdministrativeFoundationState")
            .contains("requireAssignmentReplacementCampaignCompatibility");

        assertThat(read("src/main/java/com/vladislav/training/platform/application/policy/DefaultCapabilityAdmissionPolicy.java"))
            .contains("case ASSIGNMENT_CAMPAIGN_LAUNCH")
            .contains("case ASSIGNMENT_CANCEL")
            .contains("case ASSIGNMENT_DEADLINE_EXTEND")
            .contains("case ASSIGNMENT_REPLACE_WITH_NEW");
    }

    @Test
    void queryPolicyClosureRemainsCarrierCompleteAndPreviewSeparatedFromPostLaunchRead() throws Exception {
        assertThat(enumNames(AccessReadArea.class))
            .contains("ASSIGNMENT", "ASSIGNMENT_CAMPAIGN");
        assertThat(enumNames(AccessReadSubjectScope.class))
            .contains("UNSPECIFIED", "ACTOR_SELF");
        assertThat(methodNames(AccessSpecificationPolicy.class))
            .contains("canReadAssignmentData", "canReadAssignmentCampaignData");

        assertThat(fieldTypes(AssignmentCampaignPreviewServiceImpl.class))
            .contains(
                OrganizationQueryService.class,
                OrganizationalTargetingQueryService.class,
                MandatoryAssignmentRecipientEligibilityService.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class
            )
            .doesNotContain(AssignmentCampaignReadRepository.class, AssignmentReadRepository.class);

        assertThat(fieldTypes(AssignmentCampaignQueryServiceImpl.class))
            .contains(
                AssignmentCampaignReadRepository.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class
            )
            .doesNotContain(
                OrganizationQueryService.class,
                OrganizationalTargetingQueryService.class,
                UserQueryService.class,
                AssignmentReadRepository.class
            );

        assertThat(fieldTypes(AssignmentQueryServiceImpl.class))
            .contains(
                AssignmentReadRepository.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class
            )
            .doesNotContain(
                OrganizationQueryService.class,
                OrganizationalTargetingQueryService.class,
                UserQueryService.class,
                AssignmentCampaignReadRepository.class
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"))
            .contains("\"assignment_campaign_preview\"")
            .contains("\"assignment_campaign\"")
            .contains("\"assignment_campaign_course\"")
            .contains("\"assignment\"")
            .contains("AccessReadSubjectScope.ACTOR_SELF");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignPreviewServiceImpl.java"))
            .contains("assignment_campaign_preview")
            .contains("AccessReadArea.ASSIGNMENT_CAMPAIGN")
            .doesNotContain("AssignmentCampaignReadRepository");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignQueryServiceImpl.java"))
            .contains("\"assignment_campaign\"")
            .contains("\"assignment_campaign_course\"")
            .doesNotContain("snapshotReadNotReady");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentQueryServiceImpl.java"))
            .contains("\"assignment\"")
            .doesNotContain("assignmentReadNotReady");
    }

    @Test
    void dangerousDriftAndHalfWiredSurfaceGuardsRemainFailClosed() throws Exception {
        assertThat(methodNames(AssignmentCampaignCommandService.class))
            .containsExactly("launchAssignmentCampaign")
            .doesNotContain(
                "createAssignmentCampaign",
                "recordRecipientSnapshot",
                "updateAssignmentCampaign",
                "addCourseToCampaign",
                "removeCourseFromCampaign"
            );

        assertThat(methodNames(AssignmentAdministrativeActionService.class))
            .containsExactlyInAnyOrder("cancelAssignment", "extendAssignmentDeadline", "replaceWithNewAssignment")
            .doesNotContain("patchAssignment", "updateAssignment", "changeAssignee", "editAssignmentTests");

        Method replaceWithNewMethod = AssignmentAdministrativeActionService.class.getDeclaredMethod(
            "replaceWithNewAssignment",
            AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand.class
        );
        assertThat(Arrays.asList(replaceWithNewMethod.getParameterTypes()))
            .doesNotContain(com.vladislav.training.platform.assignment.domain.Assignment.class);

        assertThat(fieldTypes(AssignmentCampaignCommandServiceImpl.class))
            .contains(
                AssignmentCommandService.class,
                AssignmentStatusRecalculationService.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                CriticalCommandAuditSupport.class
            )
            .doesNotContain(AssignmentCampaignQueryService.class, AssignmentQueryService.class);

        assertThat(fieldTypes(AssignmentAdministrativeActionServiceImpl.class))
            .contains(
                AssignmentStatusRecalculationService.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                CriticalCommandAuditSupport.class
            )
            .doesNotContain(AssignmentCampaignQueryService.class, AssignmentQueryService.class);

        assertThat(fieldTypes(AssignmentCampaignLaunchController.class))
            .containsExactly(AssignmentCampaignCommandService.class);
        assertThat(fieldTypes(AssignmentAdministrativeActionController.class))
            .containsExactlyInAnyOrder(
                AssignmentAdministrativeActionService.class,
                com.vladislav.training.platform.content.service.CourseQueryService.class
            );
        assertThat(Path.of("src/main/java/com/vladislav/training/platform/assignment/controller/AssignmentReadController.java"))
            .doesNotExist();
    }

    @Test
    void skeletonAndAuditCarriersRemainPresentAsCanonicalImplementationTargets() throws Exception {
        assertFilesExist(
            "src/main/java/com/vladislav/training/platform/assignment/controller/AssignmentCampaignLaunchController.java",
            "src/main/java/com/vladislav/training/platform/assignment/controller/AssignmentAdministrativeActionController.java",
            "src/main/java/com/vladislav/training/platform/assignment/controller/dto/LaunchAssignmentCampaignRequest.java",
            "src/main/java/com/vladislav/training/platform/assignment/controller/dto/CancelAssignmentRequest.java",
            "src/main/java/com/vladislav/training/platform/assignment/controller/dto/ExtendAssignmentDeadlineRequest.java",
            "src/main/java/com/vladislav/training/platform/assignment/controller/dto/ReplaceAssignmentWithNewRequest.java",
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandService.java",
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandServiceImpl.java",
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignPreviewService.java",
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignQueryService.java",
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentQueryService.java",
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationService.java",
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationServiceImpl.java",
            "src/main/java/com/vladislav/training/platform/assignment/repository/AssignmentCampaignRepository.java",
            "src/main/java/com/vladislav/training/platform/assignment/repository/AssignmentCampaignReadRepository.java",
            "src/main/java/com/vladislav/training/platform/assignment/repository/AssignmentReadRepository.java",
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/AssignmentCampaignEntity.java",
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/AssignmentEntity.java",
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/AssignmentAdministrativeActionEntity.java",
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/AssignmentPersistenceMapper.java",
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/JpaAssignmentCampaignReadRepositoryAdapter.java",
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/JpaAssignmentReadRepositoryAdapter.java",
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/scheduler/AssignmentSchedulerConfiguration.java",
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/scheduler/AssignmentStatusRecalculationScheduler.java"
        );

        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.auditEntityType()).isEqualTo("assignment_campaign");
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.anchorsCampaignRoot()).isTrue();
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_CANCEL.auditEntityType()).isEqualTo("assignment");
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_DEADLINE_EXTEND.auditEntityType()).isEqualTo("assignment");
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_REPLACE_WITH_NEW.auditEntityType()).isEqualTo("assignment");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/infrastructure/scheduler/AssignmentSchedulerConfiguration.java"))
            .contains("@EnableScheduling")
            .contains("@ConditionalOnProperty(name = \"assignment.scheduler.enabled\", havingValue = \"true\")");
        assertThat(AssignmentStatusRecalculationScheduler.class.isAnnotationPresent(org.springframework.stereotype.Component.class))
            .isTrue();
        assertThat(Stream.of(AssignmentStatusRecalculationScheduler.class.getDeclaredMethods())
            .anyMatch(method -> method.isAnnotationPresent(Scheduled.class))).isTrue();
    }

    private Set<String> enumNames(Class<? extends Enum<?>> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> permittedSubclassNames(Class<?> sealedType) {
        return Arrays.stream(sealedType.getPermittedSubclasses())
            .map(Class::getSimpleName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Stream.of(type.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void assertNoInstanceFields(Class<?> type) {
        assertThat(Stream.of(type.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .toList()).isEmpty();
    }

    private void assertFilesExist(String... relativePaths) {
        for (String relativePath : relativePaths) {
            assertThat(Path.of(relativePath))
                .withFailMessage("Expected readiness carrier is missing: %s", relativePath)
                .isRegularFile();
        }
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}


