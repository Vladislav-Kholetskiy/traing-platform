package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AccessSpecificationPolicy}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AccessSpecificationPolicyContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-30T08:00:00Z");

    private static final class FailClosedAccessSpecificationPolicy implements AccessSpecificationPolicy {
    }

    @Test
    void defaultContractIsFailClosed() {
        AccessSpecificationPolicy policy = new FailClosedAccessSpecificationPolicy();
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.USER_ADMINISTRATION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "app_user"
        );

        assertThat(policy.resolveReadScope(context)).isEqualTo(AccessReadScope.denyAll());
        assertThat(policy.canRead(context)).isFalse();
        assertThat(policy.canReadUserAdministrationData(101L, FIXED_INSTANT)).isFalse();
        assertThat(policy.canReadAccessManagementData(101L, FIXED_INSTANT)).isFalse();
        assertThat(policy.canReadManagerialCurrentSupervision(101L, FIXED_INSTANT)).isFalse();
        assertThat(policy.canReadManagerialHistoricalAnalytics(101L, FIXED_INSTANT)).isFalse();
        assertThat(policy.canReadExpertQuestionAnalytics(101L, FIXED_INSTANT)).isFalse();
        assertThat(context.subjectScope()).isEqualTo(AccessReadSubjectScope.UNSPECIFIED);
        assertThat(context.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.UNSPECIFIED);
    }
}
