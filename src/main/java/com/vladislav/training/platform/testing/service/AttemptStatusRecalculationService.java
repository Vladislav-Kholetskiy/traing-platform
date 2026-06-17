package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Контракт сервиса {@code AttemptStatusRecalculationService}.
 */
public interface AttemptStatusRecalculationService {

    /**
     * Возвращает обновлённую попытку вместе с предыдущим состоянием и признаком,
     * что именно этот пересчёт перевёл её в просроченное состояние.
     */
    record AttemptStatusRefreshResult(
        TestAttempt refreshedAttempt,
        TestAttemptStatus previousStatus,
        boolean expiredByThisRefresh
    ) {

        public AttemptStatusRefreshResult {
            Objects.requireNonNull(refreshedAttempt, "refreshedAttempt must not be null");
            Objects.requireNonNull(previousStatus, "previousStatus must not be null");
            if (expiredByThisRefresh && refreshedAttempt.status() != TestAttemptStatus.EXPIRED) {
                throw new IllegalArgumentException("expiredByThisRefresh requires refreshed attempt status EXPIRED");
            }
        }
    }

    /**
     * Пересчитывает итоговое состояние попытки без возврата самой записи.
     */
    TestAttemptStatus recalculateAttemptStatus(Long testAttemptId, Instant effectiveAt);

    /**
     * Обновляет сохранённое состояние попытки по её идентификатору.
     */
    TestAttempt refreshAttemptStatusCache(Long testAttemptId, Instant effectiveAt);

    /**
     * Обновляет состояние попытки в контексте уже проверенного пользователя.
     */
    TestAttempt refreshAttemptStatusCache(Long actorUserId, Long testAttemptId, Instant effectiveAt);

    /**
     * Обновляет состояние попытки и возвращает краткий итог пересчёта.
     */
    AttemptStatusRefreshResult refreshAttemptStatusCacheWithVerdict(Long actorUserId, Long testAttemptId, Instant effectiveAt);

    /**
     * Обновляет состояние назначенной попытки и возвращает итог вместе с прошлым состоянием.
     */
    AttemptStatusRefreshResult refreshAssignedAttemptStatusCacheWithVerdict(
        Long actorUserId,
        Long assignmentTestId,
        Long testAttemptId,
        Instant effectiveAt
    );
}
