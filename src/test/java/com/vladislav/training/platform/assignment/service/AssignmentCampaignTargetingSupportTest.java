package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.common.exception.PolicyViolationException;
import org.junit.jupiter.api.Test;
/**
 * Проверяет вспомогательную логику {@code AssignmentCampaignTargeting}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
class AssignmentCampaignTargetingSupportTest {

    @Test
    void supportedBasisSetIsExplicitlyFixedForAssignmentFinishScope() {
        assertThat(AssignmentCampaignTargetingSupport.supportedBasisTypes())
            .containsExactly("ORG_UNIT");
    }

    @Test
    void previewAndLaunchAcceptTheSameSupportedBasisSet() {
        assertThat(AssignmentCampaignTargetingSupport.requireSupportedPreviewBasis("ORG_UNIT").value())
            .isEqualTo("ORG_UNIT");
        assertThat(AssignmentCampaignTargetingSupport.requireSupportedLaunchBasis("ORG_UNIT").value())
            .isEqualTo("ORG_UNIT");
    }

    @Test
    void previewAndLaunchRejectTheSameUnsupportedBasisFailClosed() {
        assertThatThrownBy(() -> AssignmentCampaignTargetingSupport.requireSupportedPreviewBasis("POSITION"))
            .isInstanceOf(PolicyViolationException.class);
        assertThatThrownBy(() -> AssignmentCampaignTargetingSupport.requireSupportedLaunchBasis("POSITION"))
            .isInstanceOf(PolicyViolationException.class);
    }
}


