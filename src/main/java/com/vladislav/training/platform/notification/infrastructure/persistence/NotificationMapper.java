package com.vladislav.training.platform.notification.infrastructure.persistence;

import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.util.List;
import org.springframework.stereotype.Component;
/**
 * Преобразователь {@code NotificationMapper}.
 */

@Component
public class NotificationMapper {

    public Notification toDomain(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Notification(
            entity.getId(),
            entity.getRecipientUserId(),
            entity.getNotificationType(),
            new NotificationChannel(entity.getChannelCode()),
            NotificationStatus.valueOf(entity.getStatus()),
            entity.getDedupKey(),
            entity.getSourceEntityType(),
            entity.getSourceEntityId(),
            entity.getScheduledAt(),
            entity.getSentAt(),
            entity.getReadAt(),
            entity.getDeliveryAttemptCount(),
            entity.getErrorCode(),
            entity.getErrorMessage(),
            entity.getPayloadSnapshot(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public NotificationEntity toEntity(Notification domain) {
        if (domain == null) {
            return null;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setId(domain.id());
        entity.setRecipientUserId(domain.recipientUserId());
        entity.setNotificationType(domain.notificationType());
        entity.setChannelCode(domain.channelCode().value());
        entity.setStatus(domain.status().name());
        entity.setDedupKey(domain.dedupKey());
        entity.setSourceEntityType(domain.sourceEntityType());
        entity.setSourceEntityId(domain.sourceEntityId());
        entity.setScheduledAt(domain.scheduledAt());
        entity.setSentAt(domain.sentAt());
        entity.setReadAt(domain.readAt());
        entity.setDeliveryAttemptCount(domain.deliveryAttemptCount());
        entity.setErrorCode(domain.errorCode());
        entity.setErrorMessage(domain.errorMessage());
        entity.setPayloadSnapshot(domain.payloadSnapshot());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<Notification> toNotifications(List<NotificationEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public NotificationRule toDomain(NotificationRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationRule(
            entity.getId(),
            entity.getRuleCode(),
            entity.getName(),
            entity.getNotificationType(),
            new NotificationChannel(entity.getChannelCode()),
            entity.isEnabled(),
            entity.getDaysBeforeDeadline(),
            entity.getRepeatIntervalDays(),
            entity.getTriggerMode(),
            entity.getRecipientScopeCode(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public NotificationRuleEntity toEntity(NotificationRule domain) {
        if (domain == null) {
            return null;
        }
        NotificationRuleEntity entity = new NotificationRuleEntity();
        entity.setId(domain.id());
        entity.setRuleCode(domain.ruleCode());
        entity.setName(domain.name());
        entity.setNotificationType(domain.notificationType());
        entity.setChannelCode(domain.channelCode().value());
        entity.setEnabled(domain.isEnabled());
        entity.setDaysBeforeDeadline(domain.daysBeforeDeadline());
        entity.setRepeatIntervalDays(domain.repeatIntervalDays());
        entity.setTriggerMode(domain.triggerMode());
        entity.setRecipientScopeCode(domain.recipientScopeCode());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<NotificationRule> toNotificationRules(List<NotificationRuleEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }
}
