package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code NotificationAdminReadService}.
 */
public interface NotificationAdminReadService {

    List<NotificationAdminReadModel> listAdminNotifications(
        Long actorUserId,
        NotificationAdminReadFilter filter
    );

    NotificationAdminReadModel findAdminNotificationById(Long actorUserId, Long notificationId);

    record NotificationAdminReadFilter(
        NotificationStatus status,
        String sourceEntityType,
        String sourceEntityId,
        String dedupKey
    ) {

        public NotificationAdminReadFilter {
            if ((sourceEntityType == null) != (sourceEntityId == null)) {
                throw new IllegalArgumentException(
                    "sourceEntityType and sourceEntityId must be both null or both non-null"
                );
            }
        }
    }

    record NotificationAdminReadModel(
        Long id,
        Long recipientUserId,
        String notificationType,
        NotificationChannel channelCode,
        NotificationStatus status,
        String sourceEntityType,
        String sourceEntityId,
        Instant scheduledAt,
        Instant sentAt,
        Instant readAt,
        int deliveryAttemptCount,
        String errorCode,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
