package com.vladislav.training.platform.result.service;
/**
 * Контракт сервиса {@code ResultRecordingService}.
 */
public interface ResultRecordingService {

    /**
     * Обеспечивает существование результата для завершённой попытки и возвращает его идентификатор.
     */
    Long recordResult(Long testAttemptId);
}
