package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса чтения {@code NotificationQueryServiceImpl}.
 */

@Service
@Transactional(readOnly = true)
class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;

    NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification findNotificationById(Long notificationId) {
        return notificationRepository.findNotificationById(notificationId);
    }

    @Override
    public Notification findNotificationByIdAndRecipientUserId(Long notificationId, Long recipientUserId) {
        return notificationRepository.findNotificationByIdAndRecipientUserId(notificationId, recipientUserId);
    }

    @Override
    public List<Notification> findNotificationsByRecipientUserId(Long recipientUserId) {
        return notificationRepository.findNotificationsByRecipientUserId(recipientUserId);
    }

    @Override
    public List<Notification> findNotificationsByStatus(NotificationStatus status) {
        return notificationRepository.findNotificationsByStatus(status);
    }

    @Override
    public List<Notification> findNotificationsScheduledAtOrBefore(Instant scheduledAt) {
        return notificationRepository.findNotificationsScheduledAtOrBefore(scheduledAt);
    }

    @Override
    public List<Notification> findNotificationsBySourceEntity(String sourceEntityType, String sourceEntityId) {
        return notificationRepository.findNotificationsBySourceEntity(sourceEntityType, sourceEntityId);
    }

    @Override
    public List<Notification> findNotificationsByDedupKey(String dedupKey) {
        return notificationRepository.findNotificationsByDedupKey(dedupKey);
    }
}
