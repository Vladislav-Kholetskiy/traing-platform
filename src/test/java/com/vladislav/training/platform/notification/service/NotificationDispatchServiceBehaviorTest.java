package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.repository.NotificationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code NotificationDispatchService}.
 * Сценарии описывают ожидаемую работу компонента.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceBehaviorTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T10:00:00Z");

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationDispatchServiceImpl notificationDispatchService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Test
    void registerDispatchAttemptReloadsFactAndPersistsAttemptUpdateWithoutSentSideEffects() {
        Notification current = notification(
            100L,
            501L,
            "DEADLINE_REMINDER_7D",
            NotificationStatus.PENDING,
            "dedup-register",
            "ASSIGNMENT",
            "ASG-100",
            FIXED_INSTANT.plusSeconds(300),
            null,
            1,
            null,
            null,
            "{\"kind\":\"deadline\"}",
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(60)
        );
        when(notificationRepository.findNotificationById(100L)).thenReturn(current);
        when(notificationRepository.saveNotification(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationDispatchService.registerDispatchAttempt(100L);

        verify(notificationRepository).findNotificationById(100L);
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
        assertThat(saved.scheduledAt()).isEqualTo(current.scheduledAt());
        assertThat(saved.sentAt()).isNull();
        assertThat(saved.deliveryAttemptCount()).isEqualTo(current.deliveryAttemptCount() + 1);
        assertThat(saved.errorCode()).isEqualTo(current.errorCode());
        assertThat(saved.errorMessage()).isEqualTo(current.errorMessage());
        assertThat(saved.payloadSnapshot()).isEqualTo(current.payloadSnapshot());
        assertThat(saved.createdAt()).isEqualTo(current.createdAt());
        assertThat(saved.updatedAt()).isAfterOrEqualTo(current.updatedAt());
        assertThat(result).isEqualTo(saved);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void registerDispatchAttemptPropagatesMissingNotification() {
        when(notificationRepository.findNotificationById(999L))
            .thenThrow(new NotFoundException("Notification not found: 999"));

        assertThatThrownBy(() -> notificationDispatchService.registerDispatchAttempt(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");

        verify(notificationRepository).findNotificationById(999L);
        verify(notificationRepository, never()).saveNotification(any(Notification.class));
    }

    @Test
    void markNotificationSentPersistsSentFactAndPreservesScalarIdentityFields() {
        Notification current = notification(
            200L,
            601L,
            "DEADLINE_REMINDER_1D",
            NotificationStatus.PENDING,
            "dedup-sent",
            "ASSIGNMENT",
            "ASG-200",
            FIXED_INSTANT.plusSeconds(600),
            null,
            2,
            "E-OLD",
            "Old error",
            "{\"kind\":\"deadline\",\"offset\":1}",
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT.minusSeconds(90)
        );
        Instant sentAt = FIXED_INSTANT.plusSeconds(120);
        when(notificationRepository.findNotificationById(200L)).thenReturn(current);
        when(notificationRepository.saveNotification(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationDispatchService.markNotificationSent(200L, sentAt);

        verify(notificationRepository).findNotificationById(200L);
        verify(notificationRepository).saveNotification(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.id()).isEqualTo(current.id());
        assertThat(saved.recipientUserId()).isEqualTo(current.recipientUserId());
        assertThat(saved.notificationType()).isEqualTo(current.notificationType());
        assertThat(saved.channelCode()).isEqualTo(current.channelCode());
        assertThat(saved.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.dedupKey()).isEqualTo(current.dedupKey());
        assertThat(saved.sourceEntityType()).isEqualTo(current.sourceEntityType());
        assertThat(saved.sourceEntityId()).isEqualTo(current.sourceEntityId());
        assertThat(saved.scheduledAt()).isEqualTo(current.scheduledAt());
        assertThat(saved.sentAt()).isEqualTo(sentAt);
        assertThat(saved.deliveryAttemptCount()).isEqualTo(current.deliveryAttemptCount());
        assertThat(saved.errorCode()).isNull();
        assertThat(saved.errorMessage()).isNull();
        assertThat(saved.payloadSnapshot()).isEqualTo(current.payloadSnapshot());
        assertThat(saved.createdAt()).isEqualTo(current.createdAt());
        assertThat(saved.updatedAt()).isAfterOrEqualTo(current.updatedAt());
        assertThat(result).isEqualTo(saved);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void markNotificationSentPropagatesMissingNotification() {
        when(notificationRepository.findNotificationById(777L))
            .thenThrow(new NotFoundException("Notification not found: 777"));

        assertThatThrownBy(() -> notificationDispatchService.markNotificationSent(777L, FIXED_INSTANT))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("777");

        verify(notificationRepository).findNotificationById(777L);
        verify(notificationRepository, never()).saveNotification(any(Notification.class));
    }

    @Test
    void markNotificationFailedPersistsFailedFactAndErrorFieldsWithoutSentMutation() {
        Notification current = notification(
            300L,
            701L,
            "DEADLINE_REMINDER_7D",
            NotificationStatus.PENDING,
            "dedup-failed",
            "ASSIGNMENT",
            "ASG-300",
            FIXED_INSTANT.plusSeconds(900),
            null,
            3,
            null,
            null,
            "{\"kind\":\"deadline\",\"offset\":7}",
            FIXED_INSTANT.minusSeconds(500),
            FIXED_INSTANT.minusSeconds(100)
        );
        when(notificationRepository.findNotificationById(300L)).thenReturn(current);
        when(notificationRepository.saveNotification(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationDispatchService.markNotificationFailed(300L, "SMTP_TIMEOUT", "Provider timeout");

        verify(notificationRepository).findNotificationById(300L);
        verify(notificationRepository).saveNotification(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.id()).isEqualTo(current.id());
        assertThat(saved.recipientUserId()).isEqualTo(current.recipientUserId());
        assertThat(saved.notificationType()).isEqualTo(current.notificationType());
        assertThat(saved.channelCode()).isEqualTo(current.channelCode());
        assertThat(saved.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(saved.dedupKey()).isEqualTo(current.dedupKey());
        assertThat(saved.sourceEntityType()).isEqualTo(current.sourceEntityType());
        assertThat(saved.sourceEntityId()).isEqualTo(current.sourceEntityId());
        assertThat(saved.scheduledAt()).isEqualTo(current.scheduledAt());
        assertThat(saved.sentAt()).isEqualTo(current.sentAt());
        assertThat(saved.deliveryAttemptCount()).isEqualTo(current.deliveryAttemptCount());
        assertThat(saved.errorCode()).isEqualTo("SMTP_TIMEOUT");
        assertThat(saved.errorMessage()).isEqualTo("Provider timeout");
        assertThat(saved.payloadSnapshot()).isEqualTo(current.payloadSnapshot());
        assertThat(saved.createdAt()).isEqualTo(current.createdAt());
        assertThat(saved.updatedAt()).isAfterOrEqualTo(current.updatedAt());
        assertThat(result).isEqualTo(saved);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void markNotificationFailedPropagatesMissingNotification() {
        when(notificationRepository.findNotificationById(555L))
            .thenThrow(new NotFoundException("Notification not found: 555"));

        assertThatThrownBy(() -> notificationDispatchService.markNotificationFailed(555L, "ERR", "Message"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("555");

        verify(notificationRepository).findNotificationById(555L);
        verify(notificationRepository, never()).saveNotification(any(Notification.class));
    }

    @Test
    void dispatchServiceSourceStaysProviderSchedulerControllerAndOwnerFree() throws Exception {
        Path dispatchSource = Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationDispatchServiceImpl.java"
        );
        String source = Files.readString(dispatchSource);
        assertThat(source).doesNotContain("EmailNotificationProvider");
        assertThat(source).doesNotContain("SmsNotificationProvider");
        assertThat(source).doesNotContain("TelegramNotificationProvider");
        assertThat(source).doesNotContain("NotificationProviderRegistry");
        assertThat(source).doesNotContain("NotificationScheduler");
        assertThat(source).doesNotContain("DeadlineReminderScheduler");
        assertThat(source).doesNotContain("RestController");
        assertThat(source).doesNotContain("Controller");
        assertThat(source).doesNotContain("RequestMapping");
        assertThat(source).doesNotContain("AssignmentEntity");
        assertThat(source).doesNotContain("TestAttemptEntity");
        assertThat(source).doesNotContain("ResultEntity");
        assertThat(source).doesNotContain("CourseEntity");
        assertThat(source).doesNotContain("UserEntity");
        assertThat(source).doesNotContain("AssignmentRepository");
        assertThat(source).doesNotContain("TestAttemptRepository");
        assertThat(source).doesNotContain("ResultRepository");
        assertThat(source).doesNotContain("CourseRepository");
        assertControllerBoundaryRemainsProviderAndRepositoryFree();

        List<Path> runtimeCoreSources = List.of(
            Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationCommandServiceImpl.java"),
            Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryServiceImpl.java"),
            Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java")
        );
        for (Path runtimeCoreSource : runtimeCoreSources) {
            assertThat(Files.readString(runtimeCoreSource)).doesNotContain("NotificationDispatchService");
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
        String errorCode,
        String errorMessage,
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
            errorCode,
            errorMessage,
            payloadSnapshot,
            createdAt,
            updatedAt
        );
    }

    private void assertControllerBoundaryRemainsProviderAndRepositoryFree() throws Exception {
        Path controllerRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
        assertThat(controllerRoot).exists().isDirectory();

        try (var files = Files.walk(controllerRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("NotificationDeliveryGateway");
                assertThat(source).doesNotContain("EmailNotificationProvider");
                assertThat(source).doesNotContain("SmsNotificationProvider");
                assertThat(source).doesNotContain("TelegramNotificationProvider");
                assertThat(source).doesNotContain("NotificationProviderRegistry");
                assertThat(source).doesNotContain("@PatchMapping");
                assertThat(source).doesNotContain("@DeleteMapping");
            }
        }
    }
}
