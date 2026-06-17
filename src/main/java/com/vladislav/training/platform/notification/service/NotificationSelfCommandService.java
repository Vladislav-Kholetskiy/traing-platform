package com.vladislav.training.platform.notification.service;

import java.time.Instant;

/**
 * Контракт командного сервиса {@code NotificationSelfCommandService}.
 */
public interface NotificationSelfCommandService {

    NotificationSelfReadService.NotificationSelfReadModel markSelfNotificationRead(
        Long actorUserId,
        Long notificationId
    );

    MarkAllSelfNotificationsReadResult markAllSelfNotificationsRead(Long actorUserId);

    record MarkAllSelfNotificationsReadResult(
        int updatedCount,
        Instant readAt
    ) {
    }
}
