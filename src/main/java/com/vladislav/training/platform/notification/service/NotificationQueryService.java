package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.List;
/**
 * Контракт сервиса чтения {@code NotificationQueryService}.
 */
public interface NotificationQueryService {

    Notification findNotificationById(Long notificationId);

    Notification findNotificationByIdAndRecipientUserId(Long notificationId, Long recipientUserId);

    List<Notification> findNotificationsByRecipientUserId(Long recipientUserId);

    List<Notification> findNotificationsByStatus(NotificationStatus status);

    List<Notification> findNotificationsScheduledAtOrBefore(Instant scheduledAt);

    List<Notification> findNotificationsBySourceEntity(String sourceEntityType, String sourceEntityId);

    List<Notification> findNotificationsByDedupKey(String dedupKey);
}
