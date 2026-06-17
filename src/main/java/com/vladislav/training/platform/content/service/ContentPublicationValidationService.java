package com.vladislav.training.platform.content.service;

/**
 * Контракт сервиса {@code ContentPublicationValidationService}.
 */
public interface ContentPublicationValidationService {

    void validateQuestionPublishable(Long questionId);

    void validateTestPublishable(Long testId);
}
