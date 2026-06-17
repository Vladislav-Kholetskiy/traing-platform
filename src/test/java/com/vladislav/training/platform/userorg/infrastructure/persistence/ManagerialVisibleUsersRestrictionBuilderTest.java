package com.vladislav.training.platform.userorg.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import java.io.IOException;
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
 * Проверяет поведение {@code ManagerialVisibleUsersRestrictionBuilder}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ManagerialVisibleUsersRestrictionBuilderTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T20:00:00Z");

    @Test
    void denyAllScopeProducesFailClosedRestriction() {
        ManagerialReadScope scope = ManagerialReadScope.denyAll(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(scope);

        assertThat(restriction.failClosed()).isTrue();
        assertThat(restriction.fullAccess()).isFalse();
        assertThat(restriction.unitRestricted()).isFalse();
        assertThat(restriction.subtreeRestricted()).isFalse();
        assertThat(restriction.readScope()).isEqualTo(AccessReadScope.denyAll());
        assertThat(restriction.toSpecification("userId")).isNotNull();
    }

    @Test
    void unitOnlyScopeProducesUnitRestrictedPreFilter() {
        ManagerialReadScope scope = new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.scoped(Set.of(30L, 31L), Set.of())
        );

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(scope);

        assertThat(restriction.failClosed()).isFalse();
        assertThat(restriction.fullAccess()).isFalse();
        assertThat(restriction.unitRestricted()).isTrue();
        assertThat(restriction.subtreeRestricted()).isFalse();
        assertThat(restriction.readScope().unitOnlyIds()).containsExactlyInAnyOrder(30L, 31L);
        assertThat(restriction.readScope().subtreePaths()).isEmpty();
    }

    @Test
    void subtreeScopeProducesPathPrefixRestrictedPreFilter() {
        ManagerialReadScope scope = new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.scoped(Set.of(), Set.of("/root/sales", "/root/hr"))
        );

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(scope);

        assertThat(restriction.failClosed()).isFalse();
        assertThat(restriction.fullAccess()).isFalse();
        assertThat(restriction.unitRestricted()).isFalse();
        assertThat(restriction.subtreeRestricted()).isTrue();
        assertThat(restriction.readScope().subtreePaths()).containsExactlyInAnyOrder("/root/sales", "/root/hr");
        assertThat(restriction.readScope().unitOnlyIds()).isEmpty();
    }

    @Test
    void fullAccessScopeRemainsPreFilterAbstractionWithoutPostFilteringVocabulary() {
        ManagerialReadScope scope = new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.fullAccess()
        );

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(scope);

        assertThat(restriction.failClosed()).isFalse();
        assertThat(restriction.fullAccess()).isTrue();
        assertThat(restriction.toSpecification("userId")).isNotNull();
        assertThat(methodNames(ManagerialVisibleUsersRestrictionBuilder.ManagerialVisibleUsersRestriction.class))
            .contains("toSpecification")
            .doesNotContain("filter", "filterInMemory", "collectVisibleUsers");
    }

    @Test
    void unsupportedEmptyScopedAccessFailsClosed() {
        ManagerialReadScope scope = new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.scoped(Set.of(), Set.of())
        );

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(scope);

        assertThat(restriction.failClosed()).isTrue();
        assertThat(restriction.readScope()).isEqualTo(AccessReadScope.denyAll());
    }

    @Test
    void restrictionContractRejectsNullUserIdField() {
        var restriction = ManagerialVisibleUsersRestrictionBuilder.ManagerialVisibleUsersRestriction.denyAll(FIXED_INSTANT);

        assertThatThrownBy(() -> restriction.toSpecification(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("userIdField must not be null");
    }

    @Test
    void managerialVisibleUserRestrictionBuilderStaysIndependentFromManagementRelationAdmissionAndAnalyticsDependencies()
        throws IOException {
        String builderSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/userorg/infrastructure/persistence/ManagerialVisibleUsersRestrictionBuilder.java"
        ));

        assertThat(builderSource)
            .contains("UserOrgReadScopeJpaSupport.currentUserVisibleWithinScope")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("filterInMemory")
            .doesNotContain("collectVisibleUsers");

        assertThat(componentNames(ManagerialVisibleUsersRestrictionBuilder.ManagerialVisibleUsersRestriction.class))
            .containsExactlyInAnyOrder("effectiveAt", "readScope");
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(Collectors.toUnmodifiableSet());
    }
}
