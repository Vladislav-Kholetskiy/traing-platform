package com.vladislav.training.platform.notification.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Адаптер репозитория {@code JpaNotificationRuleRepositoryAdapter}.
 */

@Repository
@Transactional(readOnly = true)
public class JpaNotificationRuleRepositoryAdapter implements NotificationRuleRepository {

    private final SpringDataNotificationRuleJpaRepository repository;
    private final NotificationMapper mapper;

    public JpaNotificationRuleRepositoryAdapter(
        SpringDataNotificationRuleJpaRepository repository,
        NotificationMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public NotificationRule findNotificationRuleById(Long notificationRuleId) {
        return repository.findById(notificationRuleId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Notification rule not found: " + notificationRuleId));
    }

    @Override
    public NotificationRule findNotificationRuleByCode(String ruleCode) {
        return repository.findByRuleCode(ruleCode)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Notification rule not found by code: " + ruleCode));
    }

    @Override
    public List<NotificationRule> findEnabledNotificationRules() {
        return mapper.toNotificationRules(repository.findAllByEnabledTrueOrderByIdAsc());
    }

    @Override
    public List<NotificationRule> findNotificationRulesByTypeAndChannel(
        String notificationType,
        NotificationChannel channelCode
    ) {
        return mapper.toNotificationRules(
            repository.findAllByNotificationTypeAndChannelCodeOrderByIdAsc(notificationType, channelCode.value())
        );
    }

    @Override
    @Transactional
    public NotificationRule saveNotificationRule(NotificationRule notificationRule) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(notificationRule)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist notification rule", exception);
        }
    }
}
