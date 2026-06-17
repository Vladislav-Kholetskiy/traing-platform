package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.Notification;
import java.time.Instant;
/**
 * Контракт командного сервиса {@code NotificationCommandService}.
 */
public interface NotificationCommandService {

    Notification createNotification(Notification notification);

    Notification scheduleNotification(Long notificationId, Instant scheduledAt);

    Notification markNotificationRead(Long notificationId, Long recipientUserId, Instant readAt);

    int markAllNotificationsRead(Long recipientUserId, Instant readAt);
}
