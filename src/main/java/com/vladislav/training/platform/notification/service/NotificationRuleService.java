package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import java.util.List;
/**
 * Контракт сервиса {@code NotificationRuleService}.
 */
public interface NotificationRuleService {

    NotificationRule createNotificationRule(NotificationRule notificationRule);

    NotificationRule updateNotificationRule(NotificationRule notificationRule);

    NotificationRule enableNotificationRule(Long notificationRuleId);

    NotificationRule disableNotificationRule(Long notificationRuleId);

    NotificationRule findNotificationRuleById(Long notificationRuleId);

    NotificationRule findNotificationRuleByCode(String ruleCode);

    List<NotificationRule> findEnabledNotificationRules();

    List<NotificationRule> findNotificationRulesByTypeAndChannel(String notificationType, NotificationChannel channelCode);
}
