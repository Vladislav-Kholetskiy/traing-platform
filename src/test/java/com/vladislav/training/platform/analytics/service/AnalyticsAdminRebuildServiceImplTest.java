package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AnalyticsAdminRebuildServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsAdminRebuildServiceImplTest {

    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private AnalyticsResultRebuildService analyticsResultRebuildService;

    @Test
    void rebuildChecksAdmissionAndDelegatesToRuntimeRebuild() {
        Instant periodStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2027-01-01T00:00:00Z");
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            1L,
            "ANALYTICS_RESULT_REBUILD",
            com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.ANALYTICS_AGGREGATE,
            null,
            com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
            periodStart
        );
        AnalyticsResultRebuildOutcome expectedOutcome = new AnalyticsResultRebuildOutcome(
            periodStart,
            periodEnd,
            10L,
            10L,
            0L,
            2L,
            1L,
            10L,
            List.of()
        );
        when(capabilityAdmissionRequestFactory.createAnalyticsResultRebuild(1L)).thenReturn(request);
        when(analyticsResultRebuildService.rebuildResultAnalytics(periodStart, periodEnd)).thenReturn(expectedOutcome);

        AnalyticsAdminRebuildServiceImpl service = new AnalyticsAdminRebuildServiceImpl(
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            analyticsResultRebuildService
        );

        AnalyticsResultRebuildOutcome actualOutcome = service.rebuildResultAnalytics(1L, periodStart, periodEnd);

        assertThat(actualOutcome).isEqualTo(expectedOutcome);
        verify(capabilityAdmissionRequestFactory).createAnalyticsResultRebuild(1L);
        verify(capabilityAdmissionPolicy).check(request);
        verify(analyticsResultRebuildService).rebuildResultAnalytics(periodStart, periodEnd);
    }

    @Test
    void deniedAdmissionFailsClosedBeforeRuntimeRebuildExecution() {
        Instant periodStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2027-01-01T00:00:00Z");
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            1L,
            "ANALYTICS_RESULT_REBUILD",
            com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.ANALYTICS_AGGREGATE,
            null,
            com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
            periodStart
        );
        PolicyViolationException denial = new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "denied");
        when(capabilityAdmissionRequestFactory.createAnalyticsResultRebuild(1L)).thenReturn(request);
        doThrow(denial).when(capabilityAdmissionPolicy).check(request);

        AnalyticsAdminRebuildServiceImpl service = new AnalyticsAdminRebuildServiceImpl(
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            analyticsResultRebuildService
        );

        assertThatThrownBy(() -> service.rebuildResultAnalytics(1L, periodStart, periodEnd))
            .isSameAs(denial);

        verify(capabilityAdmissionRequestFactory).createAnalyticsResultRebuild(1L);
        verify(capabilityAdmissionPolicy).check(request);
        verifyNoInteractions(analyticsResultRebuildService);
    }
}
