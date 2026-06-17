package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса {@code NotificationRuleServiceImpl}.
 */

@Service
@Transactional
class NotificationRuleServiceImpl implements NotificationRuleService {

    private static final String TARGET_MODULE = "notification";
    private static final String ENTITY_TYPE_NOTIFICATION_RULE = "notification_rule";

    private final NotificationRuleRepository notificationRuleRepository;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;

    NotificationRuleServiceImpl(
        NotificationRuleRepository notificationRuleRepository,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        CriticalCommandAuditSupport criticalCommandAuditSupport
    ) {
        this.notificationRuleRepository = Objects.requireNonNull(
            notificationRuleRepository,
            "notificationRuleRepository must not be null"
        );
        this.capabilityAdmissionPolicy = Objects.requireNonNull(
            capabilityAdmissionPolicy,
            "capabilityAdmissionPolicy must not be null"
        );
        this.capabilityAdmissionRequestFactory = Objects.requireNonNull(
            capabilityAdmissionRequestFactory,
            "capabilityAdmissionRequestFactory must not be null"
        );
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
    }

    @Override
    public NotificationRule createNotificationRule(NotificationRule notificationRule) {
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createNotificationRuleCreate());
        List<NotificationRule> sameTypeAndChannelRules = notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            notificationRule.notificationType(),
            notificationRule.channelCode()
        );
        ensureNoActiveSemanticDuplicate(notificationRule, sameTypeAndChannelRules, null);
        NotificationRule saved = notificationRuleRepository.saveNotificationRule(notificationRule);
        recordAudit(
            "create",
            CapabilityOperationCode.NOTIFICATION_RULE_CREATE,
            new AuditEventType("notification.rule.created"),
            null,
            saved
        );
        return saved;
    }

    @Override
    public NotificationRule updateNotificationRule(NotificationRule notificationRule) {
        if (notificationRule.id() == null) {
            throw new IllegalArgumentException("notificationRule.id must not be null for update");
        }
        capabilityAdmissionPolicy.check(
            capabilityAdmissionRequestFactory.createNotificationRuleUpdate(notificationRule.id())
        );
        NotificationRule current = notificationRuleRepository.findNotificationRuleById(notificationRule.id());
        List<NotificationRule> sameTypeAndChannelRules = notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            notificationRule.notificationType(),
            notificationRule.channelCode()
        );
        NotificationRule updated = new NotificationRule(
            current.id(),
            notificationRule.ruleCode(),
            notificationRule.name(),
            notificationRule.notificationType(),
            notificationRule.channelCode(),
            notificationRule.isEnabled(),
            notificationRule.daysBeforeDeadline(),
            notificationRule.repeatIntervalDays(),
            notificationRule.triggerMode(),
            notificationRule.recipientScopeCode(),
            current.createdAt(),
            Instant.now()
        );
        ensureNoActiveSemanticDuplicate(updated, sameTypeAndChannelRules, current.id());
        NotificationRule saved = notificationRuleRepository.saveNotificationRule(updated);
        recordAudit(
            "update",
            CapabilityOperationCode.NOTIFICATION_RULE_UPDATE,
            new AuditEventType("notification.rule.updated"),
            current,
            saved
        );
        return saved;
    }

    @Override
    public NotificationRule enableNotificationRule(Long notificationRuleId) {
        Objects.requireNonNull(notificationRuleId, "notificationRuleId must not be null");
        capabilityAdmissionPolicy.check(
            capabilityAdmissionRequestFactory.createNotificationRuleEnable(notificationRuleId)
        );
        NotificationRule current = notificationRuleRepository.findNotificationRuleById(notificationRuleId);
        List<NotificationRule> sameTypeAndChannelRules = notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            current.notificationType(),
            current.channelCode()
        );
        NotificationRule enabled = new NotificationRule(
            current.id(),
            current.ruleCode(),
            current.name(),
            current.notificationType(),
            current.channelCode(),
            true,
            current.daysBeforeDeadline(),
            current.repeatIntervalDays(),
            current.triggerMode(),
            current.recipientScopeCode(),
            current.createdAt(),
            Instant.now()
        );
        ensureNoActiveSemanticDuplicate(enabled, sameTypeAndChannelRules, current.id());
        NotificationRule saved = notificationRuleRepository.saveNotificationRule(enabled);
        recordAudit(
            "enable",
            CapabilityOperationCode.NOTIFICATION_RULE_ENABLE,
            new AuditEventType("notification.rule.enabled"),
            current,
            saved
        );
        return saved;
    }

    @Override
    public NotificationRule disableNotificationRule(Long notificationRuleId) {
        Objects.requireNonNull(notificationRuleId, "notificationRuleId must not be null");
        capabilityAdmissionPolicy.check(
            capabilityAdmissionRequestFactory.createNotificationRuleDisable(notificationRuleId)
        );
        NotificationRule current = notificationRuleRepository.findNotificationRuleById(notificationRuleId);
        NotificationRule disabled = new NotificationRule(
            current.id(),
            current.ruleCode(),
            current.name(),
            current.notificationType(),
            current.channelCode(),
            false,
            current.daysBeforeDeadline(),
            current.repeatIntervalDays(),
            current.triggerMode(),
            current.recipientScopeCode(),
            current.createdAt(),
            Instant.now()
        );
        NotificationRule saved = notificationRuleRepository.saveNotificationRule(disabled);
        recordAudit(
            "disable",
            CapabilityOperationCode.NOTIFICATION_RULE_DISABLE,
            new AuditEventType("notification.rule.disabled"),
            current,
            saved
        );
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationRule findNotificationRuleById(Long notificationRuleId) {
        return notificationRuleRepository.findNotificationRuleById(notificationRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationRule findNotificationRuleByCode(String ruleCode) {
        return notificationRuleRepository.findNotificationRuleByCode(ruleCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRule> findEnabledNotificationRules() {
        return notificationRuleRepository.findEnabledNotificationRules();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRule> findNotificationRulesByTypeAndChannel(
        String notificationType,
        NotificationChannel channelCode
    ) {
        return notificationRuleRepository.findNotificationRulesByTypeAndChannel(notificationType, channelCode);
    }

    private void ensureNoActiveSemanticDuplicate(
        NotificationRule candidate,
        List<NotificationRule> sameTypeAndChannelRules,
        Long excludedRuleId
    ) {
        if (!candidate.isEnabled()) {
            return;
        }
        boolean duplicateExists = sameTypeAndChannelRules.stream()
            .filter(NotificationRule::isEnabled)
            .filter(existingRule -> !Objects.equals(existingRule.id(), excludedRuleId))
            .anyMatch(existingRule -> isSameActiveSemanticDuplicate(existingRule, candidate));
        if (duplicateExists) {
            throw new ConflictException(
                "Active notification rule semantic duplicate detected for type "
                    + candidate.notificationType()
                    + " and channel "
                    + candidate.channelCode().value()
            );
        }
    }

    private boolean isSameActiveSemanticDuplicate(NotificationRule left, NotificationRule right) {
        return Objects.equals(left.notificationType(), right.notificationType())
            && Objects.equals(left.channelCode(), right.channelCode())
            && Objects.equals(left.triggerMode(), right.triggerMode())
            && Objects.equals(left.recipientScopeCode(), right.recipientScopeCode())
            && Objects.equals(left.daysBeforeDeadline(), right.daysBeforeDeadline())
            && Objects.equals(left.repeatIntervalDays(), right.repeatIntervalDays());
    }

    private void recordAudit(
        String action,
        CapabilityOperationCode operationCode,
        AuditEventType eventType,
        NotificationRule payloadBefore,
        NotificationRule payloadAfter
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", action);
        details.put("ruleId", payloadAfter.id());
        details.put("ruleCode", payloadAfter.ruleCode());
        details.put("notificationType", payloadAfter.notificationType());
        details.put("channelCode", payloadAfter.channelCode().value());
        details.put("isEnabled", payloadAfter.isEnabled());
        AuditContext context = criticalCommandAuditSupport.buildAuditContext(TARGET_MODULE, operationCode, details);
        criticalCommandAuditSupport.recordAudit(
            criticalCommandAuditSupport.resolveInteractiveActorUserId(),
            eventType,
            ENTITY_TYPE_NOTIFICATION_RULE,
            payloadAfter.id(),
            payloadBefore,
            payloadAfter,
            context
        );
    }
}
