package com.vladislav.training.platform.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = NotificationPersistenceInvariantIntegrationTest.NotificationPersistenceInvariantTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code NotificationPersistenceInvariant} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class NotificationPersistenceInvariantIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T10:00:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private SpringDataAppUserJpaRepository appUserRepository;

    @Autowired
    private SpringDataNotificationJpaRepository notificationRepository;

    @Autowired
    private SpringDataNotificationRuleJpaRepository notificationRuleRepository;

    @AfterEach
    void cleanDatabase() {
        notificationRepository.deleteAllInBatch();
        notificationRuleRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
    }

    @Test
    void validNotificationPersistsAndJsonbPayloadRoundTrips() {
        AppUserEntity recipient = appUserRepository.saveAndFlush(appUserEntity("EMP-NOTIF-VALID"));

        NotificationEntity entity = validNotificationEntity(recipient.getId());
        entity.setPayloadSnapshot("{\"kind\":\"deadline\",\"days\":7,\"channels\":[\"EMAIL\"]}");
        entity.setSourceEntityType("ASSIGNMENT");
        entity.setSourceEntityId("ASG-100");
        entity.setScheduledAt(FIXED_INSTANT.plusSeconds(3_600));
        entity.setSentAt(FIXED_INSTANT.plusSeconds(7_200));
        entity.setStatus("SENT");

        NotificationEntity persisted = notificationRepository.saveAndFlush(entity);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getRecipientUserId()).isEqualTo(recipient.getId());
        assertThat(persisted.getDedupKey()).isEqualTo("notif-dedup-1");
        assertThat(persisted.getStatus()).isEqualTo("SENT");
        assertThat(persisted.getChannelCode()).isEqualTo("EMAIL");
        assertThat(persisted.getPayloadSnapshot()).contains("\"kind\":\"deadline\"");

        NotificationEntity reloaded = notificationRepository.findById(persisted.getId()).orElseThrow();
        assertThat(reloaded.getPayloadSnapshot()).contains("\"days\": 7");
        assertThat(reloaded.getPayloadSnapshot()).contains("\"channels\": [\"EMAIL\"]");
        assertThat(reloaded.getSourceEntityType()).isEqualTo("ASSIGNMENT");
        assertThat(reloaded.getSourceEntityId()).isEqualTo("ASG-100");
    }

    @Test
    void notificationConstraintsAreEnforcedByDatabase() {
        AppUserEntity recipient = appUserRepository.saveAndFlush(appUserEntity("EMP-NOTIF-CONSTRAINTS"));

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "   ",
            "EMAIL",
            "PENDING",
            "dedup-blank-type",
            null,
            null,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "REMINDER",
            "   ",
            "PENDING",
            "dedup-blank-channel",
            null,
            null,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "REMINDER",
            "EMAIL",
            "   ",
            "dedup-blank-status",
            null,
            null,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "REMINDER",
            "EMAIL",
            "PENDING",
            "   ",
            null,
            null,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "REMINDER",
            "EMAIL",
            "PENDING",
            "dedup-negative-attempts",
            null,
            null,
            -1,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "REMINDER",
            "EMAIL",
            "SENT",
            "dedup-sent-null-sent-at",
            null,
            null,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "REMINDER",
            "EMAIL",
            "PENDING",
            "dedup-source-pair-missing-id",
            "ASSIGNMENT",
            null,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "REMINDER",
            "EMAIL",
            "PENDING",
            "dedup-source-pair-missing-type",
            null,
            "ASG-101",
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        NotificationEntity sent = notificationWith(
            recipient.getId(),
            "REMINDER",
            "EMAIL",
            "SENT",
            "dedup-valid-sent",
            null,
            null,
            1,
            FIXED_INSTANT.plusSeconds(3_600)
        );
        NotificationEntity savedSent = notificationRepository.saveAndFlush(sent);
        assertThat(savedSent.getId()).isNotNull();
    }

    @Test
    void protectedDeadlineReminderUniquenessIsEnforcedWithoutBlockingNonConflictingNotifications() {
        AppUserEntity recipient = appUserRepository.saveAndFlush(appUserEntity("EMP-NOTIF-UNIQ"));

        NotificationEntity protectedReminder = notificationWith(
            recipient.getId(),
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            "PENDING",
            "dedup-protected-1",
            "ASSIGNMENT",
            "ASG-200",
            0,
            null
        );
        notificationRepository.saveAndFlush(protectedReminder);

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            "PENDING",
            "dedup-protected-2",
            "ASSIGNMENT",
            "ASG-200",
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        NotificationEntity differentType = notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "DEADLINE_REMINDER_1D",
            "EMAIL",
            "PENDING",
            "dedup-other-type",
            "ASSIGNMENT",
            "ASG-200",
            0,
            null
        ));
        NotificationEntity differentSourceType = notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            "PENDING",
            "dedup-other-source-type",
            "CAMPAIGN",
            "ASG-200",
            0,
            null
        ));
        NotificationEntity differentSourceId = notificationRepository.saveAndFlush(notificationWith(
            recipient.getId(),
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            "PENDING",
            "dedup-other-source-id",
            "ASSIGNMENT",
            "ASG-201",
            0,
            null
        ));

        assertThat(differentType.getId()).isNotNull();
        assertThat(differentSourceType.getId()).isNotNull();
        assertThat(differentSourceId.getId()).isNotNull();
    }

    @Test
    void validNotificationRulePersistsAndRuleConstraintsAreEnforced() {
        NotificationRuleEntity validRule = validNotificationRuleEntity("RULE-ALPHA");

        NotificationRuleEntity saved = notificationRuleRepository.saveAndFlush(validRule);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRuleCode()).isEqualTo("RULE-ALPHA");
        assertThat(saved.getChannelCode()).isEqualTo("EMAIL");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getRepeatIntervalDays()).isEqualTo(3);

        assertThatThrownBy(() -> notificationRuleRepository.saveAndFlush(validNotificationRuleEntity("RULE-ALPHA")))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRuleRepository.saveAndFlush(notificationRuleWith(
            "   ",
            "Rule Name",
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            true,
            7,
            3
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRuleRepository.saveAndFlush(notificationRuleWith(
            "RULE-BLANK-NAME",
            "   ",
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            true,
            7,
            3
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRuleRepository.saveAndFlush(notificationRuleWith(
            "RULE-BLANK-TYPE",
            "Rule Name",
            "   ",
            "EMAIL",
            true,
            7,
            3
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRuleRepository.saveAndFlush(notificationRuleWith(
            "RULE-BLANK-CHANNEL",
            "Rule Name",
            "DEADLINE_REMINDER_7D",
            "   ",
            true,
            7,
            3
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRuleRepository.saveAndFlush(notificationRuleWith(
            "RULE-NEGATIVE-DAYS",
            "Rule Name",
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            true,
            -1,
            3
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> notificationRuleRepository.saveAndFlush(notificationRuleWith(
            "RULE-NONPOSITIVE-REPEAT",
            "Rule Name",
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            true,
            7,
            0
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void notificationPersistenceBoundaryStaysScalarOnlyAndControllerFree() throws Exception {
        Path persistenceRoot = Path.of(
            "src/main/java/com/vladislav/training/platform/notification/infrastructure/persistence"
        );
        Path controllerRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");

        try (var files = Files.walk(persistenceRoot)) {
            List<Path> javaFiles = files
                .filter(path -> path.toString().endsWith(".java"))
                .toList();

            for (Path file : javaFiles) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("AssignmentEntity");
                assertThat(source).doesNotContain("TestAttemptEntity");
                assertThat(source).doesNotContain("ResultEntity");
                assertThat(source).doesNotContain("CourseEntity");
                assertThat(source).doesNotContain("UserEntity");
            }
        }

        assertThat(controllerRoot).exists();
        try (var files = Files.walk(controllerRoot)) {
            List<Path> javaFiles = files
                .filter(path -> path.toString().endsWith(".java"))
                .toList();

            for (Path file : javaFiles) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("SpringDataNotificationJpaRepository");
                assertThat(source).doesNotContain("SpringDataNotificationRuleJpaRepository");
                assertThat(source).doesNotContain("JpaNotificationRepositoryAdapter");
                assertThat(source).doesNotContain("JpaNotificationRuleRepositoryAdapter");
                assertThat(source).doesNotContain("AssignmentEntity");
                assertThat(source).doesNotContain("TestAttemptEntity");
                assertThat(source).doesNotContain("ResultEntity");
                assertThat(source).doesNotContain("CourseEntity");
                assertThat(source).doesNotContain("@PutMapping");
                assertThat(source).doesNotContain("@PatchMapping");
                assertThat(source).doesNotContain("@DeleteMapping");
            }
        }

        try (var migrations = Files.list(Path.of("src/main/resources/db/migration"))) {
            List<Path> postV100Migrations = migrations
                .filter(path -> path.getFileName().toString().matches("V\\d+__.*\\.sql"))
                .filter(path -> !path.getFileName().toString().startsWith("V100__"))
                .toList();

            for (Path migration : postV100Migrations) {
                String source = Files.readString(migration).toLowerCase(Locale.ROOT);
                assertThat(source).doesNotContain("create table notification");
                if (migration.getFileName().toString().startsWith("V109__")) {
                    assertThat(source).contains("add column read_at");
                } else {
                    assertThat(source).doesNotContain("alter table notification");
                }
                assertThat(source).doesNotContain("create table notification_rule");
                assertThat(source).doesNotContain("alter table notification_rule");
            }
        }
    }

    private NotificationEntity validNotificationEntity(Long recipientUserId) {
        return notificationWith(
            recipientUserId,
            "DEADLINE_REMINDER_7D",
            "EMAIL",
            "PENDING",
            "notif-dedup-1",
            null,
            null,
            0,
            null
        );
    }

    private NotificationEntity notificationWith(
        Long recipientUserId,
        String notificationType,
        String channelCode,
        String status,
        String dedupKey,
        String sourceEntityType,
        String sourceEntityId,
        int deliveryAttemptCount,
        Instant sentAt
    ) {
        NotificationEntity entity = instantiate(NotificationEntity.class);
        entity.setRecipientUserId(recipientUserId);
        entity.setNotificationType(notificationType);
        entity.setChannelCode(channelCode);
        entity.setStatus(status);
        entity.setDedupKey(dedupKey);
        entity.setSourceEntityType(sourceEntityType);
        entity.setSourceEntityId(sourceEntityId);
        entity.setScheduledAt(FIXED_INSTANT.plusSeconds(1_800));
        entity.setSentAt(sentAt);
        entity.setDeliveryAttemptCount(deliveryAttemptCount);
        entity.setErrorCode(null);
        entity.setErrorMessage(null);
        entity.setPayloadSnapshot("{\"kind\":\"deadline\"}");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private NotificationRuleEntity validNotificationRuleEntity(String ruleCode) {
        return notificationRuleWith(ruleCode, "Rule " + ruleCode, "DEADLINE_REMINDER_7D", "EMAIL", true, 7, 3);
    }

    private NotificationRuleEntity notificationRuleWith(
        String ruleCode,
        String name,
        String notificationType,
        String channelCode,
        boolean enabled,
        Integer daysBeforeDeadline,
        Integer repeatIntervalDays
    ) {
        NotificationRuleEntity entity = instantiate(NotificationRuleEntity.class);
        entity.setRuleCode(ruleCode);
        entity.setName(name);
        entity.setNotificationType(notificationType);
        entity.setChannelCode(channelCode);
        entity.setEnabled(enabled);
        entity.setDaysBeforeDeadline(daysBeforeDeadline);
        entity.setRepeatIntervalDays(repeatIntervalDays);
        entity.setTriggerMode("DEADLINE_OFFSET");
        entity.setRecipientScopeCode("ASSIGNEE");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AppUserEntity appUserEntity(String employeeNumber) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(null);
        entity.setLastName("User");
        entity.setFirstName(employeeNumber);
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity: " + type.getName(), exception);
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AppUserEntity.class,
        NotificationEntity.class,
        NotificationRuleEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAppUserJpaRepository.class,
        SpringDataNotificationJpaRepository.class,
        SpringDataNotificationRuleJpaRepository.class
    })
    static class NotificationPersistenceInvariantTestApplication {
    }
}
