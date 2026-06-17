package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeActionService;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code ManagerialReadScopeProjection}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class ManagerialReadScopeProjectionServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T19:00:00Z");

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;

    @Test
    void managerialScopeProjectionReturnsManagerialReadScopeWithManagerSemantics() {
        ManagerialReadScopeProjectionService service = new ManagerialReadScopeProjectionService(accessSpecificationPolicy);
        AccessReadScope scopedAccess = AccessReadScope.scoped(Set.of(30L), Set.of("/dept/30"));
        when(accessSpecificationPolicy.resolveReadScope(any())).thenReturn(scopedAccess);

        ManagerialReadScope projected = service.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );

        assertThat(projected.actorUserId()).isEqualTo(101L);
        assertThat(projected.effectiveAt()).isEqualTo(FIXED_INSTANT);
        assertThat(projected.contour()).isEqualTo(AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION);
        assertThat(projected.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.MANAGER);
        assertThat(projected.readScope()).isEqualTo(scopedAccess);

        ArgumentCaptor<AccessPolicyQueryContext> contextCaptor = ArgumentCaptor.forClass(AccessPolicyQueryContext.class);
        verify(accessSpecificationPolicy).resolveReadScope(contextCaptor.capture());
        AccessPolicyQueryContext context = contextCaptor.getValue();
        assertThat(context.actorUserId()).isEqualTo(101L);
        assertThat(context.effectiveAt()).isEqualTo(FIXED_INSTANT);
        assertThat(context.contour()).isEqualTo(AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION);
        assertThat(context.readType()).isEqualTo(AccessReadType.LIST);
        assertThat(context.targetEntityFamily()).isEqualTo("managerial_current_supervision");
        assertThat(context.subjectScope()).isEqualTo(AccessReadSubjectScope.UNSPECIFIED);
        assertThat(context.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.MANAGER);
    }

    @Test
    void nonManagerialContourIsRejected() {
        ManagerialReadScopeProjectionService service = new ManagerialReadScopeProjectionService(accessSpecificationPolicy);

        assertThatThrownBy(() -> service.project(101L, FIXED_INSTANT, AccessReadArea.ASSIGNMENT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("managerial contours");
    }

    @Test
    void missingOrUnsupportedAccessFoundationReturnsDenyAll() {
        ManagerialReadScopeProjectionService service = new ManagerialReadScopeProjectionService(accessSpecificationPolicy);
        when(accessSpecificationPolicy.resolveReadScope(any())).thenReturn(null);

        ManagerialReadScope projected = service.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );

        assertThat(projected.contour()).isEqualTo(AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS);
        assertThat(projected.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.MANAGER);
        assertThat(projected.readScope()).isEqualTo(AccessReadScope.denyAll());
        assertThat(projected.readScope().readAllowed()).isFalse();
    }

    @Test
    void managerialHistoricalAnalyticsProjectionUsesCanonicalManagerAnalyticsContext() {
        ManagerialReadScopeProjectionService service = new ManagerialReadScopeProjectionService(accessSpecificationPolicy);
        AccessReadScope scopedAccess = AccessReadScope.scoped(Set.of(30L), Set.of("/dept/30"));
        when(accessSpecificationPolicy.resolveReadScope(any())).thenReturn(scopedAccess);

        ManagerialReadScope projected = service.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );

        assertThat(projected.actorUserId()).isEqualTo(101L);
        assertThat(projected.effectiveAt()).isEqualTo(FIXED_INSTANT);
        assertThat(projected.contour()).isEqualTo(AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS);
        assertThat(projected.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.MANAGER);
        assertThat(projected.readScope()).isEqualTo(scopedAccess);

        ArgumentCaptor<AccessPolicyQueryContext> contextCaptor = ArgumentCaptor.forClass(AccessPolicyQueryContext.class);
        verify(accessSpecificationPolicy).resolveReadScope(contextCaptor.capture());
        AccessPolicyQueryContext context = contextCaptor.getValue();
        assertThat(context.actorUserId()).isEqualTo(101L);
        assertThat(context.effectiveAt()).isEqualTo(FIXED_INSTANT);
        assertThat(context.contour()).isEqualTo(AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS);
        assertThat(context.readType()).isEqualTo(AccessReadType.ANALYTICS);
        assertThat(context.targetEntityFamily()).isEqualTo("managerial_historical_analytics");
        assertThat(context.subjectScope()).isEqualTo(AccessReadSubjectScope.UNSPECIFIED);
        assertThat(context.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.MANAGER);
    }

    @Test
    void policyFailureReturnsDenyAllInsteadOfEscalatingToMutationOrAdmissionPath() {
        ManagerialReadScopeProjectionService service = new ManagerialReadScopeProjectionService(accessSpecificationPolicy);
        when(accessSpecificationPolicy.resolveReadScope(any()))
            .thenThrow(new PolicyViolationException("managerial contour still fail-closed"));

        ManagerialReadScope projected = service.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );

        assertThat(projected.readScope()).isEqualTo(AccessReadScope.denyAll());
        assertThat(projected.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.MANAGER);
    }

    @Test
    void managerialScopeProjectionDoesNotDependOnManagementRelationAdmissionAssignmentOrAnalyticsCollaborators()
        throws IOException {
        assertThat(fieldTypes(ManagerialReadScopeProjectionService.class))
            .contains(AccessSpecificationPolicy.class)
            .doesNotContain(
                ManagementRelationRepository.class,
                CapabilityAdmissionPolicy.class,
                AssignmentAdministrativeActionService.class,
                AssignmentCommandService.class,
                AssignmentStatusRecalculationService.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class
            );

        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/ManagerialReadScopeProjectionService.java"
        ));
        assertThat(source)
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AssignmentAdministrativeActionService")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("EffectiveAccessResolver")
            .contains("AccessSpecificationPolicy")
            .contains("resolveReadScope");
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }
}
