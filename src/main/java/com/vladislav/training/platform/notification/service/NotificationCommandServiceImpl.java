package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.repository.NotificationRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация командного сервиса {@code NotificationCommandServiceImpl}.
 */

@Service
@Transactional
class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;

    NotificationCommandServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification createNotification(Notification notification) {
        return notificationRepository.saveNotification(notification);
    }

    @Override
    public Notification scheduleNotification(Long notificationId, Instant scheduledAt) {
        Notification current = notificationRepository.findNotificationById(notificationId);
        Notification scheduled = new Notification(
            current.id(),
            current.recipientUserId(),
            current.notificationType(),
            current.channelCode(),
            current.status(),
            current.dedupKey(),
            current.sourceEntityType(),
            current.sourceEntityId(),
            scheduledAt,
            current.sentAt(),
            current.readAt(),
            current.deliveryAttemptCount(),
            current.errorCode(),
            current.errorMessage(),
            current.payloadSnapshot(),
            current.createdAt(),
            Instant.now()
        );
        return notificationRepository.saveNotification(scheduled);
    }

    @Override
    public Notification markNotificationRead(Long notificationId, Long recipientUserId, Instant readAt) {
        if (readAt == null) {
            throw new IllegalArgumentException("readAt must not be null");
        }
        Notification current = notificationRepository.findNotificationByIdAndRecipientUserId(
            notificationId,
            recipientUserId
        );
        if (current.readAt() != null) {
            return current;
        }
        return notificationRepository.saveNotification(markRead(current, readAt));
    }

    @Override
    public int markAllNotificationsRead(Long recipientUserId, Instant readAt) {
        if (readAt == null) {
            throw new IllegalArgumentException("readAt must not be null");
        }
        int updatedCount = 0;
        for (Notification current : notificationRepository.findNotificationsByRecipientUserId(recipientUserId)) {
            if (current.readAt() != null) {
                continue;
            }
            notificationRepository.saveNotification(markRead(current, readAt));
            updatedCount++;
        }
        return updatedCount;
    }

    private Notification markRead(Notification current, Instant readAt) {
        return new Notification(
            current.id(),
            current.recipientUserId(),
            current.notificationType(),
            current.channelCode(),
            current.status(),
            current.dedupKey(),
            current.sourceEntityType(),
            current.sourceEntityId(),
            current.scheduledAt(),
            current.sentAt(),
            readAt,
            current.deliveryAttemptCount(),
            current.errorCode(),
            current.errorMessage(),
            current.payloadSnapshot(),
            current.createdAt(),
            Instant.now()
        );
    }
}
