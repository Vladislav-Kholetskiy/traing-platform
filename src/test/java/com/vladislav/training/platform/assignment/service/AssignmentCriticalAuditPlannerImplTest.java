package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
/**
 * Проверяет поведение {@code AssignmentCriticalAuditPlannerImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentCriticalAuditPlannerImplTest {

    private final AssignmentCriticalAuditPlannerImpl support = new AssignmentCriticalAuditPlannerImpl();

    @Test
    void launchCompanionPlanFixesCampaignRootAndSynchronousCompanionSemantics() {
        AssignmentCriticalAuditPlanner.LaunchAuditPlan plan = support.planLaunchAudit(41L);

        assertThat(plan.auditCatalog()).isEqualTo(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH);
        assertThat(plan.auditEntityType()).isEqualTo("assignment_campaign");
        assertThat(plan.auditEntityId()).isEqualTo(41L);
        assertThat(plan.requiresSynchronousCommandBoundary()).isTrue();
        assertThat(plan.allowsAsyncBestEffortTail()).isFalse();
        assertThat(plan.runtimeEmbedded()).isFalse();
        assertThat(plan.substitutesOwnerHistory()).isFalse();
        assertThat(plan.auditFoundationType()).isEqualTo("CriticalCommandAuditSupport");
    }

    @Test
    void administrativeCompanionPlanDifferentiatesTypedAdministrativeFamilies() {
        assertAdministrativePlan(CapabilityOperationCode.ASSIGNMENT_CANCEL, "ASSIGNMENT_CANCELLED");
        assertAdministrativePlan(CapabilityOperationCode.ASSIGNMENT_DEADLINE_EXTEND, "ASSIGNMENT_DEADLINE_EXTENDED");
        assertAdministrativePlan(CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW, "ASSIGNMENT_REPLACED_WITH_NEW");
    }

    @Test
    void administrativeCompanionPlanRejectsLaunchOperationOnAdministrativeSeam() {
        assertThatThrownBy(() -> support.planAdministrativeAudit(
            CapabilityOperationCode.ASSIGNMENT_CAMPAIGN_LAUNCH,
            55L
        ))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readinessSupportDoesNotAdvertiseSpringRuntimeOrAsyncBehavior() {
        assertThat(AssignmentCriticalAuditPlannerImpl.class.isAnnotationPresent(Service.class)).isFalse();
        assertThat(AssignmentCriticalAuditPlannerImpl.class.isAnnotationPresent(Component.class)).isFalse();
        assertThat(Stream.of(AssignmentCriticalAuditPlannerImpl.class.getDeclaredMethods())
            .noneMatch(this::hasAsyncAnnotation)).isTrue();
    }

    private void assertAdministrativePlan(CapabilityOperationCode operationCode, String auditEventType) {
        AssignmentCriticalAuditPlanner.AdministrativeAuditPlan plan =
            support.planAdministrativeAudit(operationCode, 55L);

        assertThat(plan.auditCatalog().operationCode()).isEqualTo(operationCode);
        assertThat(plan.auditCatalog().auditEventType().value()).isEqualTo(auditEventType);
        assertThat(plan.auditEntityType()).isEqualTo("assignment");
        assertThat(plan.auditEntityId()).isEqualTo(55L);
        assertThat(plan.requiresSynchronousCommandBoundary()).isTrue();
        assertThat(plan.allowsAsyncBestEffortTail()).isFalse();
        assertThat(plan.runtimeEmbedded()).isFalse();
        assertThat(plan.substitutesOwnerHistory()).isFalse();
        assertThat(plan.auditFoundationType()).isEqualTo("CriticalCommandAuditSupport");
    }

    private boolean hasAsyncAnnotation(Method method) {
        return method.isAnnotationPresent(Async.class);
    }
}


