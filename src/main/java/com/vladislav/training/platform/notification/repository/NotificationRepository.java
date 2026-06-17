package com.vladislav.training.platform.notification.repository;

import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.List;
/**
 * Контракт репозитория {@code NotificationRepository}.
 */
public interface NotificationRepository {

    Notification findNotificationById(Long notificationId);

    Notification findNotificationByIdAndRecipientUserId(Long notificationId, Long recipientUserId);

    List<Notification> findPendingEligibleDispatchNotifications(Instant now, int limit);

    List<Notification> findPendingEligibleDispatchCandidates(Instant now, int limit);

    List<Notification> findNotificationsByRecipientUserId(Long recipientUserId);

    List<Notification> findNotificationsByStatus(NotificationStatus status);

    List<Notification> findNotificationsScheduledAtOrBefore(Instant scheduledAt);

    List<Notification> findNotificationsBySourceEntity(String sourceEntityType, String sourceEntityId);

    List<Notification> findNotificationsByDedupKey(String dedupKey);

    Notification saveNotification(Notification notification);
}
