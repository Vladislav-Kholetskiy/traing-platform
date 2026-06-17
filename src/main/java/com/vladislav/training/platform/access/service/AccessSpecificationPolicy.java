package com.vladislav.training.platform.access.service;

import java.time.Instant;

/**
 * Интерфейс {@code AccessSpecificationPolicy}.
 */
public interface AccessSpecificationPolicy {

    default AccessReadScope resolveReadScope(AccessPolicyQueryContext context) {
        return AccessReadScope.denyAll();
    }

    default boolean canRead(AccessPolicyQueryContext context) {
        return resolveReadScope(context).readAllowed();
    }

    default boolean canReadUserAdministrationData(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadAccessManagementData(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadAssignmentData(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadAssignmentCampaignData(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadContentAuthoringData(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadContentLifecycleData(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadContentFinalControlData(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadManagerialCurrentSupervision(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadManagerialHistoricalAnalytics(Long userId, Instant effectiveAt) {
        return false;
    }

    default boolean canReadExpertQuestionAnalytics(Long userId, Instant effectiveAt) {
        return false;
    }
}

