package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Question;

/**
 * Контракт сервиса {@code QuestionLifecycleService}.
 */
public interface QuestionLifecycleService {

    Question publish(Long questionId);

    Question archive(Long questionId);
}
