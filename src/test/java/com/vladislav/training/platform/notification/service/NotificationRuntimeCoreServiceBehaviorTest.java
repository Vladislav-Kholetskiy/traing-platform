package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.repository.NotificationRepository;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code NotificationRuntimeCoreService}.
 * Сценарии описывают ожидаемую работу компонента.
 */
@ExtendWith(MockitoExtension.class)
class NotificationRuntimeCoreServiceBehaviorTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T12:00:00Z");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationRuleRepository notificationRuleRepository;

    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;

    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;

    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    private NotificationCommandServiceImpl notificationCommandService;

    private NotificationRuleServiceImpl notificationRuleService;

    private NotificationQueryServiceImpl notificationQueryService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Captor
    private ArgumentCaptor<NotificationRule> notificationRuleCaptor;

    @BeforeEach
    void setUp() {
        notificationCommandService = new NotificationCommandServiceImpl(notificationRepository);
        notificationRuleService = new NotificationRuleServiceImpl(
            notificationRuleRepository,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            criticalCommandAuditSupport
        );
        notificationQueryService = new NotificationQueryServiceImpl(notificationRepository);
    }

    @Test
    void createNotificationDelegatesToRepositoryAndPreservesFactFields() {
        Notification command = notification(
            10L,
            101L,
            "DEADLINE_REMINDER_7D",
            NotificationStatus.PENDING,
            "dedup-1",
            "ASSIGNMENT",
            "ASG-1",
            FIXED_INSTANT.plusSeconds(3600),
            null,
            0,
            "{\"kind\":\"deadline\"}",
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        Notification persisted = notification(
            11L,
            101L,
            "DEADLINE_REMINDER_7D",
            NotificationStatus.PENDING,
            "dedup-1",
            "ASSIGNMENT",
            "ASG-1",
            FIXED_INSTANT.plusSeconds(3600),
            null,
            0,
            "{\"kind\":\"deadline\"}",
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(5)
        );
        when(notificationRepository.saveNotification(command)).thenReturn(persisted);

        Notification result = notificationCommandService.createNotification(command);

        assertThat(result).isEqualTo(persisted);
        verify(notificationRepository).saveNotification(command);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void scheduleNotificationReloadsFactAndPersistsOnlySchedulingUpdate() {
        Notification current = notification(
            15L,
            202L,
            "DEADLINE_REMINDER_1D",
            NotificationStatus.PENDING,
            "dedup-2",
            "ASSIGNMENT",
            "ASG-15",
            FIXED_INSTANT.plusSeconds(600),
            null,
            0,
            "{\"kind\":\"deadline\",\"offset\":1}",
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(30)
        );
        Instant newScheduledAt = FIXED_INSTANT.plusSeconds(7200);
        when(notificationRepository.findNotificationById(15L)).thenReturn(current);
        when(notificationRepository.saveNotification(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationCommandService.scheduleNotification(15L, newScheduledAt);

        verify(notificationRepository).findNotificationById(15L);
        verify(notificationRepository).saveNotification(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.id()).isEqualTo(current.id());
        assertThat(saved.recipientUserId()).isEqualTo(current.recipientUserId());
        assertThat(saved.notificationType()).isEqualTo(current.notificationType());
        assertThat(saved.channelCode()).isEqualTo(current.channelCode());
        assertThat(saved.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.dedupKey()).isEqualTo(current.dedupKey());
        assertThat(saved.sourceEntityType()).isEqualTo(current.sourceEntityType());
        assertThat(saved.sourceEntityId()).isEqualTo(current.sourceEntityId());
        assertThat(saved.scheduledAt()).isEqualTo(newScheduledAt);
        assertThat(saved.sentAt()).isNull();
        assertThat(saved.deliveryAttemptCount()).isEqualTo(current.deliveryAttemptCount());
        assertThat(saved.payloadSnapshot()).isEqualTo(current.payloadSnapshot());
        assertThat(saved.createdAt()).isEqualTo(current.createdAt());
        assertThat(saved.updatedAt()).isNotNull();
        assertThat(saved.updatedAt()).isAfterOrEqualTo(current.updatedAt());
        assertThat(result).isEqualTo(saved);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void scheduleNotificationPropagatesMissingNotification() {
        when(notificationRepository.findNotificationById(999L))
            .thenThrow(new NotFoundException("Notification not found: 999"));

        assertThatThrownBy(() -> notificationCommandService.scheduleNotification(999L, FIXED_INSTANT))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");

        verify(notificationRepository).findNotificationById(999L);
        verify(notificationRepository, never()).saveNotification(any(Notification.class));
    }

    @Test
    void markNotificationReadSetsReadTimestampForOwningRecipientOnly() {
        Notification current = notification(
            30L,
            303L,
            "assignment_campaign_assigned",
            NotificationStatus.PENDING,
            "dedup-read",
            "assignment_campaign",
            "12",
            FIXED_INSTANT,
            null,
            0,
            "{\"kind\":\"assignment\"}",
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(30)
        );
        Instant readAt = FIXED_INSTANT.plusSeconds(30);
        when(notificationRepository.findNotificationByIdAndRecipientUserId(30L, 303L)).thenReturn(current);
        when(notificationRepository.saveNotification(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationCommandService.markNotificationRead(30L, 303L, readAt);

        verify(notificationRepository).findNotificationByIdAndRecipientUserId(30L, 303L);
        verify(notificationRepository).saveNotification(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.id()).isEqualTo(current.id());
        assertThat(saved.recipientUserId()).isEqualTo(current.recipientUserId());
        assertThat(saved.status()).isEqualTo(current.status());
        assertThat(saved.sentAt()).isEqualTo(current.sentAt());
        assertThat(saved.readAt()).isEqualTo(readAt);
        assertThat(saved.deliveryAttemptCount()).isEqualTo(current.deliveryAttemptCount());
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void markAllNotificationsReadUpdatesOnlyUnreadRecipientRows() {
        Notification unreadFirst = notification(
            31L,
            404L,
            "assignment_campaign_assigned",
            NotificationStatus.PENDING,
            "dedup-read-all-1",
            "assignment_campaign",
            "13",
            FIXED_INSTANT,
            null,
            0,
            "{\"kind\":\"assignment\"}",
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(30)
        );
        Notification unreadSecond = notification(
            32L,
            404L,
            "assignment_deadline_extended",
            NotificationStatus.PENDING,
            "dedup-read-all-2",
            "ASSIGNMENT",
            "32",
            FIXED_INSTANT,
            null,
            0,
            "{\"kind\":\"assignment\"}",
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(30)
        );
        Instant readAt = FIXED_INSTANT.plusSeconds(90);
        when(notificationRepository.findNotificationsByRecipientUserId(404L))
            .thenReturn(List.of(unreadFirst, unreadSecond));
        when(notificationRepository.saveNotification(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        int updatedCount = notificationCommandService.markAllNotificationsRead(404L, readAt);

        assertThat(updatedCount).isEqualTo(2);
        verify(notificationRepository).findNotificationsByRecipientUserId(404L);
        verify(notificationRepository, org.mockito.Mockito.times(2)).saveNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
            .extracting(Notification::readAt)
            .containsExactly(readAt, readAt);
    }

    @Test
    void createRuleDelegatesToRuleRepositoryOnly() {
        CapabilityAdmissionRequest admissionRequest = org.mockito.Mockito.mock(CapabilityAdmissionRequest.class);
        NotificationRule rule = notificationRule(
            null,
            "RULE-CREATE",
            "Create rule",
            true,
            7,
            2,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        NotificationRule persisted = notificationRule(
            21L,
            "RULE-CREATE",
            "Create rule",
            true,
            7,
            2,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(1)
        );
        when(capabilityAdmissionRequestFactory.createNotificationRuleCreate()).thenReturn(admissionRequest);
        when(notificationRuleRepository.findNotificationRulesByTypeAndChannel(rule.notificationType(), rule.channelCode()))
            .thenReturn(List.of());
        when(notificationRuleRepository.saveNotificationRule(rule)).thenReturn(persisted);

        NotificationRule result = notificationRuleService.createNotificationRule(rule);

        assertThat(result).isEqualTo(persisted);
        verify(capabilityAdmissionRequestFactory).createNotificationRuleCreate();
        verify(capabilityAdmissionPolicy).check(admissionRequest);
        verify(notificationRuleRepository).findNotificationRulesByTypeAndChannel(
            rule.notificationType(),
            rule.channelCode()
        );
        verify(notificationRuleRepository).saveNotificationRule(rule);
        verifyNoMoreInteractions(notificationRuleRepository);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void updateRuleReloadsCurrentFactAndPersistsUpdatedRuleWithoutTouchingNotifications() {
        CapabilityAdmissionRequest admissionRequest = org.mockito.Mockito.mock(CapabilityAdmissionRequest.class);
        NotificationRule current = notificationRule(
            41L,
            "RULE-41",
            "Current",
            false,
            7,
            3,
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT.minusSeconds(120)
        );
        NotificationRule update = notificationRule(
            41L,
            "RULE-41",
            "Updated",
            true,
            5,
            1,
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT.minusSeconds(120)
        );
        when(capabilityAdmissionRequestFactory.createNotificationRuleUpdate(41L)).thenReturn(admissionRequest);
        when(notificationRuleRepository.findNotificationRuleById(41L)).thenReturn(current);
        when(notificationRuleRepository.findNotificationRulesByTypeAndChannel(
            update.notificationType(),
            update.channelCode()
        )).thenReturn(List.of());
        when(notificationRuleRepository.saveNotificationRule(any(NotificationRule.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRule result = notificationRuleService.updateNotificationRule(update);

        verify(capabilityAdmissionRequestFactory).createNotificationRuleUpdate(41L);
        verify(capabilityAdmissionPolicy).check(admissionRequest);
        verify(notificationRuleRepository).findNotificationRuleById(41L);
        verify(notificationRuleRepository).findNotificationRulesByTypeAndChannel(
            update.notificationType(),
            update.channelCode()
        );
        verify(notificationRuleRepository).saveNotificationRule(notificationRuleCaptor.capture());
        NotificationRule saved = notificationRuleCaptor.getValue();
        assertThat(saved.id()).isEqualTo(current.id());
        assertThat(saved.ruleCode()).isEqualTo(update.ruleCode());
        assertThat(saved.name()).isEqualTo("Updated");
        assertThat(saved.notificationType()).isEqualTo(update.notificationType());
        assertThat(saved.channelCode()).isEqualTo(update.channelCode());
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.daysBeforeDeadline()).isEqualTo(5);
        assertThat(saved.repeatIntervalDays()).isEqualTo(1);
        assertThat(saved.triggerMode()).isEqualTo(update.triggerMode());
        assertThat(saved.recipientScopeCode()).isEqualTo(update.recipientScopeCode());
        assertThat(saved.createdAt()).isEqualTo(current.createdAt());
        assertThat(saved.updatedAt()).isAfterOrEqualTo(current.updatedAt());
        assertThat(result).isEqualTo(saved);
        verifyNoMoreInteractions(notificationRuleRepository);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void updateRuleRejectsNullIdentifierBeforeRepositoryLookup() {
        NotificationRule invalid = notificationRule(
            null,
            "RULE-NO-ID",
            "No id",
            true,
            1,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> notificationRuleService.updateNotificationRule(invalid))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id");

        verifyNoInteractions(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
        verifyNoMoreInteractions(notificationRuleRepository);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void ruleQueriesDelegateReadOnlyToRuleRepository() {
        NotificationRule byId = notificationRule(51L, "RULE-ID", "Rule by id", true, 7, 2, FIXED_INSTANT, FIXED_INSTANT);
        NotificationRule byCode = notificationRule(52L, "RULE-CODE", "Rule by code", true, 7, 2, FIXED_INSTANT, FIXED_INSTANT);
        List<NotificationRule> enabled = List.of(byId, byCode);
        List<NotificationRule> filtered = List.of(byCode);
        NotificationChannel channel = new NotificationChannel("EMAIL");

        when(notificationRuleRepository.findNotificationRuleById(51L)).thenReturn(byId);
        when(notificationRuleRepository.findNotificationRuleByCode("RULE-CODE")).thenReturn(byCode);
        when(notificationRuleRepository.findEnabledNotificationRules()).thenReturn(enabled);
        when(notificationRuleRepository.findNotificationRulesByTypeAndChannel("DEADLINE_REMINDER_7D", channel))
            .thenReturn(filtered);

        assertThat(notificationRuleService.findNotificationRuleById(51L)).isEqualTo(byId);
        assertThat(notificationRuleService.findNotificationRuleByCode("RULE-CODE")).isEqualTo(byCode);
        assertThat(notificationRuleService.findEnabledNotificationRules()).containsExactlyElementsOf(enabled);
        assertThat(notificationRuleService.findNotificationRulesByTypeAndChannel("DEADLINE_REMINDER_7D", channel))
            .containsExactlyElementsOf(filtered);

        verify(notificationRuleRepository).findNotificationRuleById(51L);
        verify(notificationRuleRepository).findNotificationRuleByCode("RULE-CODE");
        verify(notificationRuleRepository).findEnabledNotificationRules();
        verify(notificationRuleRepository).findNotificationRulesByTypeAndChannel("DEADLINE_REMINDER_7D", channel);
        verify(notificationRuleRepository, never()).saveNotificationRule(any(NotificationRule.class));
        verifyNoMoreInteractions(notificationRuleRepository);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void queryServiceDelegatesReadSlicesWithoutMutation() {
        Notification byId = notification(
            61L,
            501L,
            "DEADLINE_REMINDER_7D",
            NotificationStatus.PENDING,
            "dedup-q",
            "ASSIGNMENT",
            "ASG-Q",
            FIXED_INSTANT.plusSeconds(90),
            null,
            0,
            "{\"kind\":\"deadline\"}",
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        List<Notification> byRecipient = List.of(byId);
        List<Notification> byStatus = List.of(byId);
        List<Notification> bySchedule = List.of(byId);
        List<Notification> bySource = List.of(byId);
        List<Notification> byDedup = List.of(byId);

        when(notificationRepository.findNotificationById(61L)).thenReturn(byId);
        when(notificationRepository.findNotificationsByRecipientUserId(501L)).thenReturn(byRecipient);
        when(notificationRepository.findNotificationsByStatus(NotificationStatus.PENDING)).thenReturn(byStatus);
        when(notificationRepository.findNotificationsScheduledAtOrBefore(FIXED_INSTANT.plusSeconds(120))).thenReturn(bySchedule);
        when(notificationRepository.findNotificationsBySourceEntity("ASSIGNMENT", "ASG-Q")).thenReturn(bySource);
        when(notificationRepository.findNotificationsByDedupKey("dedup-q")).thenReturn(byDedup);

        assertThat(notificationQueryService.findNotificationById(61L)).isEqualTo(byId);
        assertThat(notificationQueryService.findNotificationsByRecipientUserId(501L)).containsExactlyElementsOf(byRecipient);
        assertThat(notificationQueryService.findNotificationsByStatus(NotificationStatus.PENDING)).containsExactlyElementsOf(byStatus);
        assertThat(notificationQueryService.findNotificationsScheduledAtOrBefore(FIXED_INSTANT.plusSeconds(120)))
            .containsExactlyElementsOf(bySchedule);
        assertThat(notificationQueryService.findNotificationsBySourceEntity("ASSIGNMENT", "ASG-Q"))
            .containsExactlyElementsOf(bySource);
        assertThat(notificationQueryService.findNotificationsByDedupKey("dedup-q")).containsExactlyElementsOf(byDedup);

        verify(notificationRepository).findNotificationById(61L);
        verify(notificationRepository).findNotificationsByRecipientUserId(501L);
        verify(notificationRepository).findNotificationsByStatus(NotificationStatus.PENDING);
        verify(notificationRepository).findNotificationsScheduledAtOrBefore(FIXED_INSTANT.plusSeconds(120));
        verify(notificationRepository).findNotificationsBySourceEntity("ASSIGNMENT", "ASG-Q");
        verify(notificationRepository).findNotificationsByDedupKey("dedup-q");
        verify(notificationRepository, never()).saveNotification(any(Notification.class));
        verifyNoMoreInteractions(notificationRepository);
        verifyNoMoreInteractions(notificationRuleRepository);
    }

    @Test
    void notificationRuntimeCoreServiceSourcesStayBoundarySafe() throws Exception {
        assertSourceSafe(Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationCommandServiceImpl.java"));
        assertSourceSafe(Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"));
        Path querySource = Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryServiceImpl.java");
        assertSourceSafe(querySource);
        assertThat(Files.readString(querySource)).doesNotContain(".save(");
        assertControllerBoundaryRemainsRepositoryAndProviderFree();
    }

    private void assertSourceSafe(Path path) throws Exception {
        String source = Files.readString(path);
        assertThat(source).doesNotContain("AssignmentEntity");
        assertThat(source).doesNotContain("TestAttemptEntity");
        assertThat(source).doesNotContain("ResultEntity");
        assertThat(source).doesNotContain("CourseEntity");
        assertThat(source).doesNotContain("UserEntity");
        assertThat(source).doesNotContain("AssignmentRepository");
        assertThat(source).doesNotContain("TestAttemptRepository");
        assertThat(source).doesNotContain("ResultRepository");
        assertThat(source).doesNotContain("CourseRepository");
        assertThat(source).doesNotContain("NotificationDispatchService");
        assertThat(source).doesNotContain("NotificationDispatchServiceImpl");
        assertThat(source).doesNotContain("NotificationProvider");
        assertThat(source).doesNotContain("NotificationScheduler");
        assertThat(source).doesNotContain("Controller");
    }

    private void assertControllerBoundaryRemainsRepositoryAndProviderFree() throws Exception {
        Path controllerRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
        assertThat(controllerRoot).exists().isDirectory();

        try (var files = Files.walk(controllerRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("NotificationRuleService");
                assertThat(source).doesNotContain("NotificationRepository");
                assertThat(source).doesNotContain("NotificationRuleRepository");
                assertThat(source).doesNotContain("NotificationProvider");
                assertThat(source).doesNotContain("@PatchMapping");
                assertThat(source).doesNotContain("@DeleteMapping");
            }
        }
    }

    private Notification notification(
        Long id,
        Long recipientUserId,
        String notificationType,
        NotificationStatus status,
        String dedupKey,
        String sourceEntityType,
        String sourceEntityId,
        Instant scheduledAt,
        Instant sentAt,
        int deliveryAttemptCount,
        String payloadSnapshot,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Notification(
            id,
            recipientUserId,
            notificationType,
            new NotificationChannel("EMAIL"),
            status,
            dedupKey,
            sourceEntityType,
            sourceEntityId,
            scheduledAt,
            sentAt,
            null,
            deliveryAttemptCount,
            null,
            null,
            payloadSnapshot,
            createdAt,
            updatedAt
        );
    }

    private NotificationRule notificationRule(
        Long id,
        String ruleCode,
        String name,
        boolean enabled,
        Integer daysBeforeDeadline,
        Integer repeatIntervalDays,
        Instant createdAt,
        Instant updatedAt
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
            createdAt,
            updatedAt
        );
    }
}
