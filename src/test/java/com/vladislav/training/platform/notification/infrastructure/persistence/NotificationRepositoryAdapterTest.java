package com.vladislav.training.platform.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
/**
 * Проверяет поведение {@code NotificationRepositoryAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class NotificationRepositoryAdapterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T20:30:00Z");
    private static final Path NOTIFICATION_PERSISTENCE_PACKAGE =
        Path.of("src/main/java/com/vladislav/training/platform/notification/infrastructure/persistence");
    private static final Path NOTIFICATION_CONTROLLER_PACKAGE =
        Path.of("src/main/java/com/vladislav/training/platform/notification/controller");

    @Mock
    private SpringDataNotificationJpaRepository notificationJpaRepository;
    @Mock
    private SpringDataNotificationRuleJpaRepository notificationRuleJpaRepository;

    private final NotificationMapper mapper = new NotificationMapper();

    @Test
    void notificationAdapterMapsPrimaryKeySaveAndLookupSlices() {
        JpaNotificationRepositoryAdapter adapter = new JpaNotificationRepositoryAdapter(notificationJpaRepository, mapper);
        NotificationEntity entity = notificationEntity(11L, 101L, "DEDUP-11");

        when(notificationJpaRepository.findById(11L)).thenReturn(Optional.of(entity));
        when(notificationJpaRepository.findAllByRecipientUserIdOrderByIdAsc(101L)).thenReturn(List.of(entity));
        when(notificationJpaRepository.findAllByStatusOrderByIdAsc(NotificationStatus.PENDING.name())).thenReturn(List.of(entity));
        when(notificationJpaRepository.findAllByScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(FIXED_INSTANT))
            .thenReturn(List.of(entity));
        when(notificationJpaRepository.findAllBySourceEntityTypeAndSourceEntityIdOrderByIdAsc("ASSIGNMENT", "asg-41"))
            .thenReturn(List.of(entity));
        when(notificationJpaRepository.findAllByDedupKeyOrderByIdAsc("DEDUP-11")).thenReturn(List.of(entity));
        when(notificationJpaRepository.save(any(NotificationEntity.class))).thenReturn(entity);

        Notification found = adapter.findNotificationById(11L);
        Notification saved = adapter.saveNotification(mapper.toDomain(entity));

        assertThat(found.id()).isEqualTo(11L);
        assertThat(found.recipientUserId()).isEqualTo(101L);
        assertThat(found.channelCode().value()).isEqualTo("EMAIL");
        assertThat(found.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(found.dedupKey()).isEqualTo("DEDUP-11");
        assertThat(found.payloadSnapshot()).isEqualTo("{\"kind\":\"deadline\"}");

        assertThat(adapter.findNotificationsByRecipientUserId(101L))
            .singleElement()
            .satisfies(notification -> assertThat(notification.recipientUserId()).isEqualTo(101L));
        assertThat(adapter.findNotificationsByStatus(NotificationStatus.PENDING))
            .singleElement()
            .satisfies(notification -> assertThat(notification.status()).isEqualTo(NotificationStatus.PENDING));
        assertThat(adapter.findNotificationsScheduledAtOrBefore(FIXED_INSTANT))
            .singleElement()
            .satisfies(notification -> assertThat(notification.scheduledAt()).isEqualTo(FIXED_INSTANT));
        assertThat(adapter.findNotificationsBySourceEntity("ASSIGNMENT", "asg-41"))
            .singleElement()
            .satisfies(notification -> {
                assertThat(notification.sourceEntityType()).isEqualTo("ASSIGNMENT");
                assertThat(notification.sourceEntityId()).isEqualTo("asg-41");
            });
        assertThat(adapter.findNotificationsByDedupKey("DEDUP-11"))
            .singleElement()
            .satisfies(notification -> assertThat(notification.dedupKey()).isEqualTo("DEDUP-11"));

        assertThat(saved.id()).isEqualTo(11L);
        assertThat(saved.recipientUserId()).isEqualTo(101L);
        assertThat(saved.dedupKey()).isEqualTo("DEDUP-11");
        assertThat(saved.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.channelCode().value()).isEqualTo("EMAIL");
        assertThat(saved.payloadSnapshot()).isEqualTo("{\"kind\":\"deadline\"}");
        verify(notificationJpaRepository).save(any(NotificationEntity.class));
    }

    @Test
    void notificationAdapterThrowsForMissingPrimaryKeyAndWrapsConstraintViolations() {
        JpaNotificationRepositoryAdapter adapter = new JpaNotificationRepositoryAdapter(notificationJpaRepository, mapper);
        Notification domain = mapper.toDomain(notificationEntity(12L, 102L, "DEDUP-12"));

        when(notificationJpaRepository.findById(999L)).thenReturn(Optional.empty());
        when(notificationJpaRepository.save(any(NotificationEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate dedup"));

        assertThatThrownBy(() -> adapter.findNotificationById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");
        assertThatThrownBy(() -> adapter.saveNotification(domain))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("notification")
            .hasRootCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void notificationRuleAdapterMapsPrimaryKeyCodeEnabledAndSave() {
        JpaNotificationRuleRepositoryAdapter adapter =
            new JpaNotificationRuleRepositoryAdapter(notificationRuleJpaRepository, mapper);
        NotificationRuleEntity entity = notificationRuleEntity(21L, "DEADLINE_REMINDER_7D");

        when(notificationRuleJpaRepository.findById(21L)).thenReturn(Optional.of(entity));
        when(notificationRuleJpaRepository.findByRuleCode("DEADLINE_REMINDER_7D")).thenReturn(Optional.of(entity));
        when(notificationRuleJpaRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(entity));
        when(notificationRuleJpaRepository.findAllByNotificationTypeAndChannelCodeOrderByIdAsc("DEADLINE_REMINDER_7D", "EMAIL"))
            .thenReturn(List.of(entity));
        when(notificationRuleJpaRepository.save(any(NotificationRuleEntity.class))).thenReturn(entity);

        NotificationRule foundById = adapter.findNotificationRuleById(21L);
        NotificationRule foundByCode = adapter.findNotificationRuleByCode("DEADLINE_REMINDER_7D");
        NotificationRule saved = adapter.saveNotificationRule(mapper.toDomain(entity));

        assertThat(foundById.id()).isEqualTo(21L);
        assertThat(foundById.ruleCode()).isEqualTo("DEADLINE_REMINDER_7D");
        assertThat(foundById.channelCode().value()).isEqualTo("EMAIL");
        assertThat(foundById.isEnabled()).isTrue();

        assertThat(foundByCode.ruleCode()).isEqualTo("DEADLINE_REMINDER_7D");
        assertThat(adapter.findEnabledNotificationRules())
            .singleElement()
            .satisfies(rule -> assertThat(rule.isEnabled()).isTrue());
        assertThat(adapter.findNotificationRulesByTypeAndChannel("DEADLINE_REMINDER_7D", new NotificationChannel("EMAIL")))
            .singleElement()
            .satisfies(rule -> {
                assertThat(rule.notificationType()).isEqualTo("DEADLINE_REMINDER_7D");
                assertThat(rule.channelCode().value()).isEqualTo("EMAIL");
            });

        assertThat(saved.id()).isEqualTo(21L);
        assertThat(saved.ruleCode()).isEqualTo("DEADLINE_REMINDER_7D");
        assertThat(saved.repeatIntervalDays()).isEqualTo(3);
        verify(notificationRuleJpaRepository).save(any(NotificationRuleEntity.class));
    }

    @Test
    void notificationRuleAdapterThrowsForMissingRowsAndWrapsConstraintViolations() {
        JpaNotificationRuleRepositoryAdapter adapter =
            new JpaNotificationRuleRepositoryAdapter(notificationRuleJpaRepository, mapper);
        NotificationRule domain = mapper.toDomain(notificationRuleEntity(22L, "OVERDUE_ESCALATION"));

        when(notificationRuleJpaRepository.findById(998L)).thenReturn(Optional.empty());
        when(notificationRuleJpaRepository.findByRuleCode("MISSING_RULE")).thenReturn(Optional.empty());
        when(notificationRuleJpaRepository.save(any(NotificationRuleEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate rule_code"));

        assertThatThrownBy(() -> adapter.findNotificationRuleById(998L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("998");
        assertThatThrownBy(() -> adapter.findNotificationRuleByCode("MISSING_RULE"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("MISSING_RULE");
        assertThatThrownBy(() -> adapter.saveNotificationRule(domain))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("notification rule")
            .hasRootCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void notificationMapperPreservesScalarFieldsAcrossDomainAndEntityBoundaries() {
        NotificationEntity notificationEntity = notificationEntity(31L, 301L, "DEDUP-31");
        NotificationRuleEntity notificationRuleEntity = notificationRuleEntity(41L, "ASSIGNMENT_CANCELLED");

        Notification notificationDomain = mapper.toDomain(notificationEntity);
        NotificationEntity notificationRoundTrip = mapper.toEntity(notificationDomain);
        NotificationRule notificationRuleDomain = mapper.toDomain(notificationRuleEntity);
        NotificationRuleEntity notificationRuleRoundTrip = mapper.toEntity(notificationRuleDomain);

        assertThat(notificationDomain.id()).isEqualTo(31L);
        assertThat(notificationDomain.recipientUserId()).isEqualTo(301L);
        assertThat(notificationDomain.notificationType()).isEqualTo("DEADLINE_REMINDER_7D");
        assertThat(notificationDomain.channelCode().value()).isEqualTo("EMAIL");
        assertThat(notificationDomain.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notificationDomain.dedupKey()).isEqualTo("DEDUP-31");
        assertThat(notificationDomain.sourceEntityType()).isEqualTo("ASSIGNMENT");
        assertThat(notificationDomain.sourceEntityId()).isEqualTo("asg-41");
        assertThat(notificationDomain.scheduledAt()).isEqualTo(FIXED_INSTANT);
        assertThat(notificationDomain.sentAt()).isNull();
        assertThat(notificationDomain.deliveryAttemptCount()).isZero();
        assertThat(notificationDomain.errorCode()).isNull();
        assertThat(notificationDomain.errorMessage()).isNull();
        assertThat(notificationDomain.payloadSnapshot()).isEqualTo("{\"kind\":\"deadline\"}");
        assertThat(notificationDomain.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(notificationDomain.updatedAt()).isEqualTo(FIXED_INSTANT);

        assertThat(notificationRoundTrip.getId()).isEqualTo(31L);
        assertThat(notificationRoundTrip.getRecipientUserId()).isEqualTo(301L);
        assertThat(notificationRoundTrip.getNotificationType()).isEqualTo("DEADLINE_REMINDER_7D");
        assertThat(notificationRoundTrip.getChannelCode()).isEqualTo("EMAIL");
        assertThat(notificationRoundTrip.getStatus()).isEqualTo("PENDING");
        assertThat(notificationRoundTrip.getDedupKey()).isEqualTo("DEDUP-31");
        assertThat(notificationRoundTrip.getSourceEntityType()).isEqualTo("ASSIGNMENT");
        assertThat(notificationRoundTrip.getSourceEntityId()).isEqualTo("asg-41");
        assertThat(notificationRoundTrip.getScheduledAt()).isEqualTo(FIXED_INSTANT);
        assertThat(notificationRoundTrip.getDeliveryAttemptCount()).isZero();
        assertThat(notificationRoundTrip.getPayloadSnapshot()).isEqualTo("{\"kind\":\"deadline\"}");

        assertThat(notificationRuleDomain.id()).isEqualTo(41L);
        assertThat(notificationRuleDomain.ruleCode()).isEqualTo("ASSIGNMENT_CANCELLED");
        assertThat(notificationRuleDomain.name()).isEqualTo("Assignment cancelled");
        assertThat(notificationRuleDomain.notificationType()).isEqualTo("ASSIGNMENT_CANCELLED");
        assertThat(notificationRuleDomain.channelCode().value()).isEqualTo("EMAIL");
        assertThat(notificationRuleDomain.isEnabled()).isTrue();
        assertThat(notificationRuleDomain.daysBeforeDeadline()).isEqualTo(7);
        assertThat(notificationRuleDomain.repeatIntervalDays()).isEqualTo(3);
        assertThat(notificationRuleDomain.triggerMode()).isEqualTo("SCHEDULED");
        assertThat(notificationRuleDomain.recipientScopeCode()).isEqualTo("ASSIGNEE");
        assertThat(notificationRuleDomain.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(notificationRuleDomain.updatedAt()).isEqualTo(FIXED_INSTANT);

        assertThat(notificationRuleRoundTrip.getId()).isEqualTo(41L);
        assertThat(notificationRuleRoundTrip.getRuleCode()).isEqualTo("ASSIGNMENT_CANCELLED");
        assertThat(notificationRuleRoundTrip.getName()).isEqualTo("Assignment cancelled");
        assertThat(notificationRuleRoundTrip.getNotificationType()).isEqualTo("ASSIGNMENT_CANCELLED");
        assertThat(notificationRuleRoundTrip.getChannelCode()).isEqualTo("EMAIL");
        assertThat(notificationRuleRoundTrip.isEnabled()).isTrue();
        assertThat(notificationRuleRoundTrip.getDaysBeforeDeadline()).isEqualTo(7);
        assertThat(notificationRuleRoundTrip.getRepeatIntervalDays()).isEqualTo(3);
        assertThat(notificationRuleRoundTrip.getTriggerMode()).isEqualTo("SCHEDULED");
        assertThat(notificationRuleRoundTrip.getRecipientScopeCode()).isEqualTo("ASSIGNEE");
        assertThat(notificationRuleRoundTrip.getCreatedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(notificationRuleRoundTrip.getUpdatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void notificationPersistencePackageRemainsScalarAndControllerFree() throws Exception {
        String persistenceSource = Files.walk(NOTIFICATION_PERSISTENCE_PACKAGE)
            .filter(path -> path.toString().endsWith(".java"))
            .map(path -> {
                try {
                    return Files.readString(path);
                } catch (Exception exception) {
                    throw new IllegalStateException("Cannot read source: " + path, exception);
                }
            })
            .reduce("", (left, right) -> left + "\n" + right);

        assertThat(persistenceSource)
            .doesNotContain("AssignmentEntity")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ResultEntity")
            .doesNotContain("CourseEntity")
            .doesNotContain("UserEntity");
        assertThat(Files.exists(NOTIFICATION_CONTROLLER_PACKAGE)).isTrue();

        String controllerSource = Files.walk(NOTIFICATION_CONTROLLER_PACKAGE)
            .filter(path -> path.toString().endsWith(".java"))
            .map(path -> {
                try {
                    return Files.readString(path);
                } catch (Exception exception) {
                    throw new IllegalStateException("Cannot read source: " + path, exception);
                }
            })
            .reduce("", (left, right) -> left + "\n" + right);

        assertThat(controllerSource)
            .doesNotContain("SpringDataNotificationJpaRepository")
            .doesNotContain("SpringDataNotificationRuleJpaRepository")
            .doesNotContain("JpaNotificationRepositoryAdapter")
            .doesNotContain("JpaNotificationRuleRepositoryAdapter")
            .doesNotContain("AssignmentEntity")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ResultEntity")
            .doesNotContain("CourseEntity")
            .doesNotContain("@PutMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");
    }

    private NotificationEntity notificationEntity(Long id, Long recipientUserId, String dedupKey) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(id);
        entity.setRecipientUserId(recipientUserId);
        entity.setNotificationType("DEADLINE_REMINDER_7D");
        entity.setChannelCode("EMAIL");
        entity.setStatus(NotificationStatus.PENDING.name());
        entity.setDedupKey(dedupKey);
        entity.setSourceEntityType("ASSIGNMENT");
        entity.setSourceEntityId("asg-41");
        entity.setScheduledAt(FIXED_INSTANT);
        entity.setSentAt(null);
        entity.setDeliveryAttemptCount(0);
        entity.setErrorCode(null);
        entity.setErrorMessage(null);
        entity.setPayloadSnapshot("{\"kind\":\"deadline\"}");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private NotificationRuleEntity notificationRuleEntity(Long id, String ruleCode) {
        NotificationRuleEntity entity = new NotificationRuleEntity();
        entity.setId(id);
        entity.setRuleCode(ruleCode);
        entity.setName(ruleCode.equals("DEADLINE_REMINDER_7D") ? "Deadline reminder" : "Assignment cancelled");
        entity.setNotificationType(ruleCode);
        entity.setChannelCode("EMAIL");
        entity.setEnabled(true);
        entity.setDaysBeforeDeadline(7);
        entity.setRepeatIntervalDays(3);
        entity.setTriggerMode("SCHEDULED");
        entity.setRecipientScopeCode("ASSIGNEE");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }
}
