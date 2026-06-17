package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code NotificationRuleCommandAdmission}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class NotificationRuleCommandAdmissionTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T08:00:00Z");

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
    void createRuleUsesTypedAdmissionBeforeRepositoryMutation() {
        NotificationRule candidate = notificationRule(null, "RULE-CREATE", "Create", true, 7, 1);
        CapabilityAdmissionRequest request = request("NOTIFICATION_RULE_CREATE", null);
        NotificationRule saved = notificationRule(41L, "RULE-CREATE", "Create", true, 7, 1);

        when(capabilityAdmissionRequestFactory.createNotificationRuleCreate()).thenReturn(request);
        when(notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            candidate.notificationType(),
            candidate.channelCode()
        )).thenReturn(List.of());
        when(notificationRuleRepository.saveNotificationRule(candidate)).thenReturn(saved);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(anyString(), any(CapabilityOperationCode.class), anyMap()))
            .thenReturn(new AuditContext("{\"action\":\"create\"}"));

        service.createNotificationRule(candidate);

        InOrder inOrder = inOrder(
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            notificationRuleRepository,
            criticalCommandAuditSupport
        );
        inOrder.verify(capabilityAdmissionRequestFactory).createNotificationRuleCreate();
        inOrder.verify(capabilityAdmissionPolicy).check(request);
        inOrder.verify(notificationRuleRepository).findNotificationRulesByTypeAndChannel(
            candidate.notificationType(),
            candidate.channelCode()
        );
        inOrder.verify(notificationRuleRepository).saveNotificationRule(candidate);
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("notification_rule"),
            eq(41L),
            eq(null),
            eq(saved),
            any()
        );
    }

    @Test
    void deniedCreateDoesNotMutateRuleAndDoesNotWriteSuccessAudit() {
        NotificationRule candidate = notificationRule(null, "RULE-DENIED", "Denied", true, 7, 1);
        CapabilityAdmissionRequest request = request("NOTIFICATION_RULE_CREATE", null);

        when(capabilityAdmissionRequestFactory.createNotificationRuleCreate()).thenReturn(request);
        doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "denied create"))
            .when(capabilityAdmissionPolicy).check(request);

        assertThatThrownBy(() -> service.createNotificationRule(candidate))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("denied create");

        verify(capabilityAdmissionRequestFactory).createNotificationRuleCreate();
        verify(capabilityAdmissionPolicy).check(request);
        verifyNoInteractions(notificationRuleRepository, criticalCommandAuditSupport);
    }

    @Test
    void updateRuleUsesTypedAdmissionBeforeRepositoryMutation() {
        NotificationRule current = notificationRule(51L, "RULE-51", "Current", false, 7, 2);
        NotificationRule update = notificationRule(51L, "RULE-51", "Updated", true, 5, 1);
        CapabilityAdmissionRequest request = request("NOTIFICATION_RULE_UPDATE", 51L);

        when(capabilityAdmissionRequestFactory.createNotificationRuleUpdate(51L)).thenReturn(request);
        when(notificationRuleRepository.findNotificationRuleById(51L)).thenReturn(current);
        when(notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            update.notificationType(),
            update.channelCode()
        )).thenReturn(List.of());
        when(notificationRuleRepository.saveNotificationRule(any(NotificationRule.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(anyString(), any(CapabilityOperationCode.class), anyMap()))
            .thenReturn(new AuditContext("{\"action\":\"update\"}"));

        service.updateNotificationRule(update);

        InOrder inOrder = inOrder(
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            notificationRuleRepository,
            criticalCommandAuditSupport
        );
        inOrder.verify(capabilityAdmissionRequestFactory).createNotificationRuleUpdate(51L);
        inOrder.verify(capabilityAdmissionPolicy).check(request);
        inOrder.verify(notificationRuleRepository).findNotificationRuleById(51L);
        inOrder.verify(notificationRuleRepository).findNotificationRulesByTypeAndChannel(
            update.notificationType(),
            update.channelCode()
        );
        inOrder.verify(notificationRuleRepository).saveNotificationRule(any(NotificationRule.class));
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("notification_rule"),
            eq(51L),
            eq(current),
            any(NotificationRule.class),
            any()
        );
    }

    @Test
    void deniedUpdateDoesNotMutateRuleAndDoesNotWriteSuccessAudit() {
        NotificationRule update = notificationRule(61L, "RULE-61", "Denied update", true, 7, 1);
        CapabilityAdmissionRequest request = request("NOTIFICATION_RULE_UPDATE", 61L);

        when(capabilityAdmissionRequestFactory.createNotificationRuleUpdate(61L)).thenReturn(request);
        doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "denied update"))
            .when(capabilityAdmissionPolicy).check(request);

        assertThatThrownBy(() -> service.updateNotificationRule(update))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("denied update");

        verify(capabilityAdmissionRequestFactory).createNotificationRuleUpdate(61L);
        verify(capabilityAdmissionPolicy).check(request);
        verifyNoInteractions(notificationRuleRepository, criticalCommandAuditSupport);
    }

    @Test
    void enableRuleUsesDedicatedAdmissionBuilderBeforeMutation() {
        NotificationRule current = notificationRule(71L, "RULE-71", "Enable", false, 7, 2);
        CapabilityAdmissionRequest request = request("NOTIFICATION_RULE_ENABLE", 71L);

        when(capabilityAdmissionRequestFactory.createNotificationRuleEnable(71L)).thenReturn(request);
        when(notificationRuleRepository.findNotificationRuleById(71L)).thenReturn(current);
        when(notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            current.notificationType(),
            current.channelCode()
        )).thenReturn(List.of());
        when(notificationRuleRepository.saveNotificationRule(any(NotificationRule.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(anyString(), any(CapabilityOperationCode.class), anyMap()))
            .thenReturn(new AuditContext("{\"action\":\"enable\"}"));

        service.enableNotificationRule(71L);

        InOrder inOrder = inOrder(
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            notificationRuleRepository,
            criticalCommandAuditSupport
        );
        inOrder.verify(capabilityAdmissionRequestFactory).createNotificationRuleEnable(71L);
        inOrder.verify(capabilityAdmissionPolicy).check(request);
        inOrder.verify(notificationRuleRepository).findNotificationRuleById(71L);
        inOrder.verify(notificationRuleRepository).findNotificationRulesByTypeAndChannel(
            current.notificationType(),
            current.channelCode()
        );
        inOrder.verify(notificationRuleRepository).saveNotificationRule(any(NotificationRule.class));
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("notification_rule"),
            eq(71L),
            eq(current),
            any(NotificationRule.class),
            any()
        );
    }

    @Test
    void deniedEnableDoesNotMutateRuleAndDoesNotWriteSuccessAudit() {
        CapabilityAdmissionRequest request = request("NOTIFICATION_RULE_ENABLE", 71L);
        when(capabilityAdmissionRequestFactory.createNotificationRuleEnable(71L)).thenReturn(request);
        doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "denied enable"))
            .when(capabilityAdmissionPolicy).check(request);

        assertThatThrownBy(() -> service.enableNotificationRule(71L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("denied enable");

        verify(capabilityAdmissionRequestFactory).createNotificationRuleEnable(71L);
        verify(capabilityAdmissionPolicy).check(request);
        verifyNoInteractions(notificationRuleRepository, criticalCommandAuditSupport);
    }

    @Test
    void disableRuleUsesDedicatedAdmissionBuilderBeforeMutation() {
        NotificationRule current = notificationRule(81L, "RULE-81", "Disable", true, 7, 2);
        CapabilityAdmissionRequest request = request("NOTIFICATION_RULE_DISABLE", 81L);

        when(capabilityAdmissionRequestFactory.createNotificationRuleDisable(81L)).thenReturn(request);
        when(notificationRuleRepository.findNotificationRuleById(81L)).thenReturn(current);
        when(notificationRuleRepository.saveNotificationRule(any(NotificationRule.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(anyString(), any(CapabilityOperationCode.class), anyMap()))
            .thenReturn(new AuditContext("{\"action\":\"disable\"}"));

        service.disableNotificationRule(81L);

        InOrder inOrder = inOrder(
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            notificationRuleRepository,
            criticalCommandAuditSupport
        );
        inOrder.verify(capabilityAdmissionRequestFactory).createNotificationRuleDisable(81L);
        inOrder.verify(capabilityAdmissionPolicy).check(request);
        inOrder.verify(notificationRuleRepository).findNotificationRuleById(81L);
        inOrder.verify(notificationRuleRepository).saveNotificationRule(any(NotificationRule.class));
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("notification_rule"),
            eq(81L),
            eq(current),
            any(NotificationRule.class),
            any()
        );
        verify(notificationRuleRepository, never()).findNotificationRulesByTypeAndChannel(any(), any());
    }

    @Test
    void deniedDisableDoesNotMutateRuleAndDoesNotWriteSuccessAudit() {
        CapabilityAdmissionRequest request = request("NOTIFICATION_RULE_DISABLE", 81L);
        when(capabilityAdmissionRequestFactory.createNotificationRuleDisable(81L)).thenReturn(request);
        doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "denied disable"))
            .when(capabilityAdmissionPolicy).check(request);

        assertThatThrownBy(() -> service.disableNotificationRule(81L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("denied disable");

        verify(capabilityAdmissionRequestFactory).createNotificationRuleDisable(81L);
        verify(capabilityAdmissionPolicy).check(request);
        verifyNoInteractions(notificationRuleRepository, criticalCommandAuditSupport);
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
        String name,
        boolean enabled,
        Integer daysBeforeDeadline,
        Integer repeatIntervalDays
    ) {
        return new NotificationRule(
            id,
            ruleCode,
            name,
            "DEADLINE_REMINDER_7D",
            new NotificationChannel("EMAIL"),
            enabled,
            daysBeforeDeadline,
            repeatIntervalDays,
            "DEADLINE_OFFSET",
            "ASSIGNEE",
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT
        );
    }
}
