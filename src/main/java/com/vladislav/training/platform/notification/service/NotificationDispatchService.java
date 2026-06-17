package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.Notification;
import java.time.Instant;
/**
 * Контракт сервиса {@code NotificationDispatchService}.
 */
public interface NotificationDispatchService {

    int dispatchPendingNotifications(Instant now, int limit);

    Notification registerDispatchAttempt(Long notificationId);

    Notification markNotificationSent(Long notificationId, Instant sentAt);

    Notification markNotificationFailed(Long notificationId, String errorCode, String errorMessage);
}
