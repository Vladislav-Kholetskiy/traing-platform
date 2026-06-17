package com.vladislav.training.platform.notification.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataNotificationRuleJpaRepository}.
 */
public interface SpringDataNotificationRuleJpaRepository extends JpaRepository<NotificationRuleEntity, Long> {

    Optional<NotificationRuleEntity> findByRuleCode(String ruleCode);

    List<NotificationRuleEntity> findAllByEnabledTrueOrderByIdAsc();

    List<NotificationRuleEntity> findAllByNotificationTypeAndChannelCodeOrderByIdAsc(
        String notificationType,
        String channelCode
    );
}
