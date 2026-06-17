package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса {@code NotificationDispatchServiceImpl}.
 */

@Service
@Transactional
class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final ObjectProvider<NotificationDeliveryGateway> notificationDeliveryGatewayProvider;

    NotificationDispatchServiceImpl(
        NotificationRepository notificationRepository,
        ObjectProvider<NotificationDeliveryGateway> notificationDeliveryGatewayProvider
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationDeliveryGatewayProvider = notificationDeliveryGatewayProvider;
    }

    @Override
    public int dispatchPendingNotifications(Instant now, int limit) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }

        List<Notification> candidates = notificationRepository.findPendingEligibleDispatchNotifications(now, limit);
        NotificationDeliveryGateway gateway = notificationDeliveryGatewayProvider != null
            ? notificationDeliveryGatewayProvider.getIfAvailable()
            : null;

        int processed = 0;
        for (Notification candidate : candidates) {
            Notification attempted = registerDispatchAttempt(candidate.id());
            if (gateway == null) {
                markNotificationFailed(
                    attempted.id(),
                    "NO_DELIVERY_GATEWAY",
                    "Notification delivery gateway is not configured"
                );
                processed++;
                continue;
            }

            try {
                NotificationDeliveryResult result = gateway.deliver(attempted);
                if (result.successful()) {
                    markNotificationSent(attempted.id(), result.sentAt());
                } else {
                    markNotificationFailed(attempted.id(), result.errorCode(), result.errorMessage());
                }
            } catch (RuntimeException exception) {
                markNotificationFailed(
                    attempted.id(),
                    "DELIVERY_EXCEPTION",
                    exception.getMessage() == null ? "Notification delivery failed" : exception.getMessage()
                );
            }
            processed++;
        }
        return processed;
    }

    @Override
    public Notification registerDispatchAttempt(Long notificationId) {
        Notification current = notificationRepository.findNotificationById(notificationId);
        Notification updated = new Notification(
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
            current.readAt(),
            current.deliveryAttemptCount() + 1,
            current.errorCode(),
            current.errorMessage(),
            current.payloadSnapshot(),
            current.createdAt(),
            Instant.now()
        );
        return notificationRepository.saveNotification(updated);
    }

    @Override
    public Notification markNotificationSent(Long notificationId, Instant sentAt) {
        Notification current = notificationRepository.findNotificationById(notificationId);
        Notification updated = new Notification(
            current.id(),
            current.recipientUserId(),
            current.notificationType(),
            current.channelCode(),
            NotificationStatus.SENT,
            current.dedupKey(),
            current.sourceEntityType(),
            current.sourceEntityId(),
            current.scheduledAt(),
            sentAt,
            current.readAt(),
            current.deliveryAttemptCount(),
            null,
            null,
            current.payloadSnapshot(),
            current.createdAt(),
            Instant.now()
        );
        return notificationRepository.saveNotification(updated);
    }

    @Override
    public Notification markNotificationFailed(Long notificationId, String errorCode, String errorMessage) {
        Notification current = notificationRepository.findNotificationById(notificationId);
        Notification updated = new Notification(
            current.id(),
            current.recipientUserId(),
            current.notificationType(),
            current.channelCode(),
            NotificationStatus.FAILED,
            current.dedupKey(),
            current.sourceEntityType(),
            current.sourceEntityId(),
            current.scheduledAt(),
            current.sentAt(),
            current.readAt(),
            current.deliveryAttemptCount(),
            errorCode,
            errorMessage,
            current.payloadSnapshot(),
            current.createdAt(),
            Instant.now()
        );
        return notificationRepository.saveNotification(updated);
    }
}
