package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Вспомогательный тип {@code AssignmentCampaignTargetingSupport}.
 */
final class AssignmentCampaignTargetingSupport {

    private static final Set<AssignmentCampaignTargetingBasis> SUPPORTED_BASES = Set.of(
        new AssignmentCampaignTargetingBasis("ORG_UNIT")
    );

    private AssignmentCampaignTargetingSupport() {
    }

    static Set<String> supportedBasisTypes() {
        return SUPPORTED_BASES.stream()
            .map(AssignmentCampaignTargetingBasis::value)
            .collect(Collectors.toUnmodifiableSet());
    }

    static AssignmentCampaignTargetingBasis requireSupportedPreviewBasis(String basisType) {
        return requireSupported(
            basisType,
            "DOWNSTREAM_OPERATION_NOT_READY",
            "Предпросмотр кампании назначения"
        );
    }

    static AssignmentCampaignTargetingBasis requireSupportedLaunchBasis(String basisType) {
        return requireSupported(
            basisType,
            "ASSIGNMENT_CAMPAIGN_TARGETING_NOT_READY",
            "Запуск кампании назначения"
        );
    }

    private static AssignmentCampaignTargetingBasis requireSupported(
        String rawBasisType,
        String errorCode,
        String operationName
    ) {
        AssignmentCampaignTargetingBasis basis = new AssignmentCampaignTargetingBasis(rawBasisType);
        if (!SUPPORTED_BASES.contains(basis)) {
            throw new PolicyViolationException(
                errorCode,
                operationName + " поддерживает только такие варианты основания: " + String.join(", ", supportedBasisTypes())
            );
        }
        return basis;
    }
}

