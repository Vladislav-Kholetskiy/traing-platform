package com.vladislav.training.platform.notification.repository;

import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import java.util.List;
/**
 * Контракт репозитория {@code NotificationRuleRepository}.
 */
public interface NotificationRuleRepository {

    NotificationRule findNotificationRuleById(Long notificationRuleId);

    NotificationRule findNotificationRuleByCode(String ruleCode);

    List<NotificationRule> findEnabledNotificationRules();

    List<NotificationRule> findNotificationRulesByTypeAndChannel(String notificationType, NotificationChannel channelCode);

    NotificationRule saveNotificationRule(NotificationRule notificationRule);
}
