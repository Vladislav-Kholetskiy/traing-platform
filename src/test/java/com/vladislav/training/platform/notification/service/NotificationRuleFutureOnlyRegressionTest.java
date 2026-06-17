package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет, что {@code NotificationRuleFutureOnly} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
@ExtendWith(MockitoExtension.class)
class NotificationRuleFutureOnlyRegressionTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-09T10:00:00Z");
    private static final Path RULE_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
    );
    private static final Path NOTIFICATION_COMMAND_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationCommandServiceImpl.java"
    );

    @Mock
    private NotificationRuleRepository notificationRuleRepository;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;

    private NotificationRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationRuleServiceImpl(
            notificationRuleRepository,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            criticalCommandAuditSupport
        );
    }

    @Test
    void ruleConfigurationMustRemainFutureOnlyAndMustNotRewriteHistoricalNotificationFacts() throws Exception {
        String source = Files.readString(RULE_SERVICE);

        assertThat(source)
            .doesNotContain("NotificationRepository")
            .doesNotContain("saveNotification(")
            .doesNotContain("deleteNotification(")
            .doesNotContain("findNotificationById(")
            .doesNotContain("findNotifications")
            .doesNotContain("backfill")
            .doesNotContain("repair")
            .doesNotContain("rebuild")
            .doesNotContain("reconcile")
            .doesNotContain("recover")
            .doesNotContain("historical notification")
            .doesNotContain("rewrite historical")
            .doesNotContain("old windows")
            .doesNotContain("scheduler");
    }

    @Test
    void enableDisableUpdateMustMutateOnlyRuleFactsAndLeaveDeliveryFactsUntouched() {
        NotificationRule current = notificationRule(81L, "RULE-81", false, 7, 2);
        NotificationRule update = notificationRule(81L, "RULE-81", true, 5, 1);

        when(capabilityAdmissionRequestFactory.createNotificationRuleUpdate(81L)).thenReturn(request("NOTIFICATION_RULE_UPDATE", 81L));
        when(capabilityAdmissionRequestFactory.createNotificationRuleEnable(81L)).thenReturn(request("NOTIFICATION_RULE_ENABLE", 81L));
        when(capabilityAdmissionRequestFactory.createNotificationRuleDisable(81L)).thenReturn(request("NOTIFICATION_RULE_DISABLE", 81L));
        when(notificationRuleRepository.findNotificationRuleById(81L)).thenReturn(current);
        when(notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            current.notificationType(),
            current.channelCode()
        )).thenReturn(List.of());
        when(notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            update.notificationType(),
            update.channelCode()
        )).thenReturn(List.of());
        when(notificationRuleRepository.saveNotificationRule(any(NotificationRule.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(900L);
        when(criticalCommandAuditSupport.buildAuditContext(anyString(), any(CapabilityOperationCode.class), anyMap()))
            .thenReturn(new AuditContext("{\"contour\":\"administrative\"}"));

        service.updateNotificationRule(update);
        service.enableNotificationRule(81L);
        service.disableNotificationRule(81L);

        verify(notificationRuleRepository, times(3)).findNotificationRuleById(81L);
        verify(notificationRuleRepository, times(3)).saveNotificationRule(any(NotificationRule.class));
        verifyNoInteractionsWithDeliveryContour();
    }

    @Test
    void semanticDuplicateGuardMustStaySourceRowSemanticAndNotCollapseIntoActiveStatusOnlySuppression() throws Exception {
        String source = Files.readString(RULE_SERVICE);

        assertThat(source)
            .contains("findNotificationRulesByTypeAndChannel")
            .contains("filter(NotificationRule::isEnabled)")
            .contains("left.triggerMode()")
            .contains("left.recipientScopeCode()")
            .contains("left.daysBeforeDeadline()")
            .contains("left.repeatIntervalDays()")
            .doesNotContain("findNotificationsByStatus(")
            .doesNotContain("NotificationStatus")
            .doesNotContain("active-status-only");
    }

    @Test
    void ruleConfigurationMustNotBecomeOwnerTruthOrDeliveryBackfillFacade() throws Exception {
        String ruleSource = Files.readString(RULE_SERVICE);
        String commandSource = Files.readString(NOTIFICATION_COMMAND_SERVICE);

        assertThat(ruleSource)
            .doesNotContain("Assignment")
            .doesNotContain("Content")
            .doesNotContain("Testing")
            .doesNotContain("Result")
            .doesNotContain("updateAssignment")
            .doesNotContain("patchOwner")
            .doesNotContain("ownerTable");

        assertThat(commandSource)
            .doesNotContain("backfill")
            .doesNotContain("rebuild")
            .doesNotContain("repair")
            .doesNotContain("reconcile");
    }

    private void verifyNoInteractionsWithDeliveryContour() {
        verify(notificationRuleRepository, never()).findNotificationRuleByCode(any());
    }

    private CapabilityAdmissionRequest request(String operationCode, Long targetEntityId) {
        return new CapabilityAdmissionRequest(
            101L,
            operationCode,
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            targetEntityId,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
    }

    private NotificationRule notificationRule(
        Long id,
        String ruleCode,
        boolean enabled,
        Integer daysBeforeDeadline,
        Integer repeatIntervalDays
    ) {
        return new NotificationRule(
            id,
            ruleCode,
            "Rule " + ruleCode,
            "DEADLINE_REMINDER_7D",
            new NotificationChannel("EMAIL"),
            enabled,
            daysBeforeDeadline,
            repeatIntervalDays,
            "DEADLINE_OFFSET",
            "ASSIGNEE",
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT
        );
    }
}
