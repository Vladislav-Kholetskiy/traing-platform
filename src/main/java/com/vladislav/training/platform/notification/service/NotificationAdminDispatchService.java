package com.vladislav.training.platform.notification.service;

/**
 * Контракт сервиса {@code NotificationAdminDispatchService}.
 */
public interface NotificationAdminDispatchService {

    DispatchPendingNotificationsResult dispatchPendingNotifications(Long actorUserId, int limit);

    record DispatchPendingNotificationsResult(
        int processedCount
    ) {
    }
}
