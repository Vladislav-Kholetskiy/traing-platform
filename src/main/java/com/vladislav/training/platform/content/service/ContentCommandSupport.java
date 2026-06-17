package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Вспомогательный тип {@code ContentCommandSupport}.
 */
@Component
class ContentCommandSupport {

    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory requestFactory;

    ContentCommandSupport(
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory requestFactory
    ) {
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.requestFactory = requestFactory;
    }

    void checkCreate(CapabilityOperationCode operationCode, CapabilityTargetEntityType targetEntityType) {
        capabilityAdmissionPolicy.check(requestFactory.create(operationCode.code(), targetEntityType, null));
    }

    void checkCreate(String operationCode, CapabilityTargetEntityType targetEntityType) {
        capabilityAdmissionPolicy.check(requestFactory.create(operationCode, targetEntityType, null));
    }

    void checkChildRootCreate(
        CapabilityOperationCode operationCode,
        CapabilityTargetEntityType targetEntityType,
        Long parentEntityId,
        CapabilityTargetEntityType parentEntityType
    ) {
        capabilityAdmissionPolicy.check(requestFactory.create(
            operationCode.code(),
            targetEntityType,
            null,
            new CapabilityAdmissionPayload.ContentMutation(parentEntityId, parentEntityType)
        ));
    }

    void checkDraftUpdate(CapabilityOperationCode operationCode, CapabilityTargetEntityType targetEntityType, Long targetEntityId) {
        capabilityAdmissionPolicy.check(requestFactory.create(operationCode.code(), targetEntityType, targetEntityId));
    }

    void checkDraftUpdate(
        CapabilityOperationCode operationCode,
        CapabilityTargetEntityType targetEntityType,
        Long targetEntityId,
        Long parentEntityId,
        CapabilityTargetEntityType parentEntityType
    ) {
        capabilityAdmissionPolicy.check(requestFactory.create(
            operationCode.code(),
            targetEntityType,
            targetEntityId,
            new CapabilityAdmissionPayload.ContentMutation(parentEntityId, parentEntityType)
        ));
    }

    void checkUpdate(CapabilityOperationCode operationCode, CapabilityTargetEntityType targetEntityType, Long targetEntityId) {
        capabilityAdmissionPolicy.check(requestFactory.create(operationCode.code(), targetEntityType, targetEntityId));
    }

    void checkFinal(CapabilityOperationCode operationCode, Long topicId) {
        capabilityAdmissionPolicy.check(requestFactory.create(
            operationCode.code(),
            CapabilityTargetEntityType.TOPIC_FINAL_CONTROL,
            topicId,
            new CapabilityAdmissionPayload.TopicFinalControlMutation(topicId, null)
        ));
    }

    void checkFinal(CapabilityOperationCode operationCode, Long topicId, Long testId) {
        capabilityAdmissionPolicy.check(requestFactory.create(
            operationCode.code(),
            CapabilityTargetEntityType.TOPIC_FINAL_CONTROL,
            topicId,
            new CapabilityAdmissionPayload.TopicFinalControlMutation(topicId, testId)
        ));
    }

    void requireDraft(ContentStatus status, String entityName) {
        if (status != ContentStatus.DRAFT) {
            throw new ConflictException(entityName + " must be DRAFT");
        }
    }

    void requirePublished(ContentStatus status, String entityName) {
        if (status != ContentStatus.PUBLISHED) {
            throw new ConflictException(entityName + " must be PUBLISHED");
        }
    }

    void validateParentForChildRootAuthoring(ContentStatus parentStatus, String parentName) {
        if (parentStatus == ContentStatus.ARCHIVED) {
            throw new ConflictException(parentName + " ARCHIVED parent cannot accept child-root authoring");
        }
    }

    void validateThreshold(BigDecimal thresholdPercent) {
        if (thresholdPercent == null
            || thresholdPercent.compareTo(BigDecimal.ZERO) < 0
            || thresholdPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidationException("thresholdPercent must be between 0 and 100");
        }
    }

    void requireNotBlank(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank");
        }
    }

    void requireNonNull(String fieldName, Object value) {
        if (value == null) {
            throw new ValidationException(fieldName + " must not be null");
        }
    }

    void validateNonNegative(String fieldName, Integer value) {
        if (value != null && value < 0) {
            throw new ValidationException(fieldName + " must be non-negative");
        }
    }

    void validateNonNegative(String fieldName, int value) {
        if (value < 0) {
            throw new ValidationException(fieldName + " must be non-negative");
        }
    }

    void validatePositive(String fieldName, BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }

    void validateAnswerOptionFields(String body, Object answerOptionRole, Boolean isCorrect, int displayOrder, Integer canonicalOrderPosition) {
        requireNotBlank("body", body);
        requireNonNull("answerOptionRole", answerOptionRole);
        validateNonNegative("displayOrder", displayOrder);
        validateNonNegative("canonicalOrderPosition", canonicalOrderPosition);
        if ("CHOICE_OPTION".equals(String.valueOf(answerOptionRole))) {
            requireNonNull("isCorrect", isCorrect);
        } else if (isCorrect != null) {
            throw new ValidationException("isCorrect is allowed only for CHOICE_OPTION");
        }
    }

    void validateActiveFinalEligibility(Test test) {
        if (test.testType() != TestType.CONTROL || test.status() != ContentStatus.PUBLISHED) {
            throw new ConflictException("Active final test must be CONTROL and PUBLISHED");
        }
    }
}
