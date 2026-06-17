package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code ManagerialReadScope}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ManagerialReadScopeContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T18:00:00Z");

    @Test
    void managerialReadScopeCarriesExpectedReadOnlyFields() {
        AccessReadScope scopedAccess = AccessReadScope.scoped(Set.of(30L), Set.of("/dept/30"));

        ManagerialReadScope scope = new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            scopedAccess
        );

        assertThat(scope.actorUserId()).isEqualTo(101L);
        assertThat(scope.effectiveAt()).isEqualTo(FIXED_INSTANT);
        assertThat(scope.contour()).isEqualTo(AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION);
        assertThat(scope.readScope()).isEqualTo(scopedAccess);
        assertThat(scope.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.MANAGER);
        assertThat(orderedComponentNames(ManagerialReadScope.class))
            .containsExactly("actorUserId", "effectiveAt", "contour", "readScope");
    }

    @Test
    void managerialReadScopeRejectsNullMandatoryFields() {
        assertThatThrownBy(() -> new ManagerialReadScope(
            null,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.fullAccess()
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("actorUserId must not be null");
        assertThatThrownBy(() -> new ManagerialReadScope(
            101L,
            null,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.fullAccess()
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("effectiveAt must not be null");
        assertThatThrownBy(() -> new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            null,
            AccessReadScope.fullAccess()
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("contour must not be null");
        assertThatThrownBy(() -> new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("readScope must not be null");
    }

    @Test
    void managerialReadScopeRemainsFailClosedWhenConstructedThroughDenyAllFactory() {
        ManagerialReadScope scope = ManagerialReadScope.denyAll(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );

        assertThat(scope.contour()).isEqualTo(AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS);
        assertThat(scope.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.MANAGER);
        assertThat(scope.readScope()).isEqualTo(AccessReadScope.denyAll());
        assertThat(scope.readScope().readAllowed()).isFalse();
        assertThat(scope.readScope().fullOrganizationalUnitAccess()).isFalse();
        assertThat(scope.readScope().unitOnlyIds()).isEmpty();
        assertThat(scope.readScope().subtreePaths()).isEmpty();
    }

    @Test
    void managerialReadScopeRejectsNonManagerialContours() {
        assertThatThrownBy(() -> new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.ASSIGNMENT,
            AccessReadScope.fullAccess()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("managerial contours");
    }

    @Test
    void managerialReadScopeDoesNotAccumulateMutationOrAdmissionFields() {
        assertThat(componentNames(ManagerialReadScope.class))
            .doesNotContain(
                "managementRelationId",
                "managementRelationTypeId",
                "capabilityAdmission",
                "mutationAllowed",
                "assignmentCommand",
                "recalculationRequested",
                "analyticsRefreshRequested",
                "analyticsRebuildRequested"
            );
        assertThat(methodNames(ManagerialReadScope.class))
            .contains("subjectSemantics")
            .doesNotContain("check", "admit", "recalculate", "refresh", "rebuild", "mutate");
    }

    @Test
    void managerialReadScopeStaysIndependentFromManagementRelationAdmissionAndAnalyticsDependencies() throws IOException {
        assertThat(fieldTypes(ManagerialReadScope.class))
            .doesNotContain(
                ManagementRelation.class,
                ManagementRelationRepository.class,
                CapabilityAdmissionPolicy.class,
                AssignmentCommandService.class,
                AssignmentStatusRecalculationService.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class
            );

        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/ManagerialReadScope.java"
        ));
        assertThat(source)
            .doesNotContain("ManagementRelation")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService");
    }

    private Set<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private java.util.List<String> orderedComponentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }
}
