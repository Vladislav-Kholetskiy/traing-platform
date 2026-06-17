package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.NotificationChannel;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code NotificationSelfReadService}.
 */
public interface NotificationSelfReadService {

    List<NotificationSelfReadModel> listSelfNotifications(Long actorUserId);

    NotificationSelfReadModel findSelfNotificationById(Long actorUserId, Long notificationId);

    record NotificationSelfReadModel(
        Long id,
        String title,
        String message,
        NotificationChannel channelCode,
        Instant createdAt,
        Instant readAt,
        boolean read,
        String notificationType,
        String companyName,
        List<NotificationAssignmentRecipientReadModel> assignmentRecipients
    ) {
    }

    record NotificationAssignmentRecipientReadModel(
        Long userId,
        String fullName,
        String courseName,
        String companyName,
        String organizationalUnitName
    ) {
    }
}
