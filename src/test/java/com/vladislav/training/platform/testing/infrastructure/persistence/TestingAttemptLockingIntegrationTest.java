package com.vladislav.training.platform.testing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentTestEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentTestJpaRepository;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = TestingAttemptLockingIntegrationTest.TestingAttemptLockingTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code TestingAttemptLocking} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class TestingAttemptLockingIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T12:00:00Z");

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
    private SpringDataCourseJpaRepository courseRepository;
    @Autowired
    private SpringDataTopicJpaRepository topicRepository;
    @Autowired
    private SpringDataTestJpaRepository testRepository;
    @Autowired
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignRepository;
    @Autowired
    private SpringDataAssignmentJpaRepository assignmentRepository;
    @Autowired
    private SpringDataAssignmentTestJpaRepository assignmentTestRepository;
    @Autowired
    private SpringDataTestAttemptJpaRepository testAttemptRepository;
    @Autowired
    private TestAttemptRepository testAttemptLockingRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        testAttemptRepository.deleteAllInBatch();
        assignmentTestRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        assignmentCampaignRepository.deleteAllInBatch();
        testRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
    }

    @Test
    void lockByIdBlocksConcurrentLockOnSameAttempt() throws Exception {
        AssignedAttemptFixture fixture = createAssignedAttemptFixture();

        Throwable failure = captureConcurrentLockTimeout(
            () -> assertThat(testAttemptLockingRepository.findAndLockTestAttemptById(fixture.attemptId()).id())
                .isEqualTo(fixture.attemptId()),
            () -> assertThat(testAttemptLockingRepository.findAndLockTestAttemptById(fixture.attemptId()).id())
                .isEqualTo(fixture.attemptId())
        );

        assertLockTimeout(failure);
    }

    @Test
    void lockActiveAssignedAttemptForActorBlocksConcurrentAssignedLock() throws Exception {
        AssignedAttemptFixture fixture = createAssignedAttemptFixture();

        Throwable failure = captureConcurrentLockTimeout(
            () -> assertThat(testAttemptLockingRepository.findAndLockActiveAssignedAttemptForActor(
                fixture.userId(),
                fixture.assignmentTestId()
            ).id())
                .isEqualTo(fixture.attemptId()),
            () -> assertThat(testAttemptLockingRepository.findAndLockActiveAssignedAttemptForActor(
                fixture.userId(),
                fixture.assignmentTestId()
            ).id())
                .isEqualTo(fixture.attemptId())
        );

        assertLockTimeout(failure);
    }

    @Test
    void actorScopedAssignedLockDoesNotReturnForeignUsersActiveAttempt() {
        AssignedAttemptFixture fixture = createAssignedAttemptFixture();

        assertThat(testAttemptLockingRepository.findAndLockActiveAssignedAttemptForActor(
            fixture.userId() + 1,
            fixture.assignmentTestId()
        )).isNull();
    }

    @Test
    void lockActiveSelfAttemptBlocksConcurrentSelfLock() throws Exception {
        SelfAttemptFixture fixture = createSelfAttemptFixture();

        Throwable failure = captureConcurrentLockTimeout(
            () -> assertThat(testAttemptLockingRepository.findAndLockActiveSelfAttempt(fixture.userId(), fixture.testId()).id())
                .isEqualTo(fixture.attemptId()),
            () -> assertThat(testAttemptLockingRepository.findAndLockActiveSelfAttempt(fixture.userId(), fixture.testId()).id())
                .isEqualTo(fixture.attemptId())
        );

        assertLockTimeout(failure);
    }

    private Throwable captureConcurrentLockTimeout(Runnable holderAction, Runnable contenderAction)
        throws InterruptedException, ExecutionException {
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> holder = executor.submit(() -> newTransactionTemplate().executeWithoutResult(status -> {
                holderAction.run();
                lockAcquired.countDown();
                awaitLatch(releaseLock);
            }));

            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Throwable> contender = executor.submit(() -> {
                try {
                    newTransactionTemplate().executeWithoutResult(status -> {
                        jdbcTemplate.execute("set local lock_timeout = '500ms'");
                        contenderAction.run();
                    });
                    return null;
                } catch (Throwable throwable) {
                    return throwable;
                }
            });

            Throwable failure = contender.get();
            releaseLock.countDown();
            holder.get();
            return failure;
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertLockTimeout(Throwable failure) {
        assertThat(failure).isNotNull();
        assertThat(failure).isInstanceOfAny(DataAccessException.class, RuntimeException.class);
        assertThat(failure.getMessage().toLowerCase()).contains("lock");
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for lock release", exception);
        }
    }

    private TransactionTemplate newTransactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private AssignedAttemptFixture createAssignedAttemptFixture() {
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-LOCK-ASG"));
        CourseEntity course = courseRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId()));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId()));
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(assignmentCampaignEntity());
        AssignmentEntity assignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), user.getId(), course.getId())
        );
        AssignmentTestEntity assignmentTest = assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(assignment.getId(), test.getId())
        );
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(
            testAttemptEntity(user.getId(), test.getId(), assignmentTest.getId(), AttemptMode.ASSIGNED)
        );
        return new AssignedAttemptFixture(user.getId(), test.getId(), assignmentTest.getId(), attempt.getId());
    }

    private SelfAttemptFixture createSelfAttemptFixture() {
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-LOCK-SELF"));
        CourseEntity course = courseRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId()));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId()));
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(
            testAttemptEntity(user.getId(), test.getId(), null, AttemptMode.SELF)
        );
        return new SelfAttemptFixture(user.getId(), test.getId(), attempt.getId());
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

    private CourseEntity courseEntity() {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Course");
        entity.setDescription("Course");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName("Topic");
        entity.setDescription("Topic");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestEntity testEntity(Long topicId) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName("Test");
        entity.setDescription("Test");
        entity.setTestType(TestType.CONTROL);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setScoringPolicyCode("DEFAULT_POLICY");
        entity.setActiveFinalForTopic(true);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignEntity assignmentCampaignEntity() {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Campaign");
        entity.setDescription("Campaign");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("UNIT-LOCK");
        entity.setSourceNameSnapshot("Operations");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentEntity assignmentEntity(Long campaignId, Long userId, Long courseId) {
        AssignmentEntity entity = instantiate(AssignmentEntity.class);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setStatus(AssignmentStatus.ASSIGNED);
        entity.setAssignedAt(FIXED_INSTANT);
        entity.setDeadlineAt(FIXED_INSTANT.plusSeconds(86_400));
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentTestEntity assignmentTestEntity(Long assignmentId, Long testId) {
        AssignmentTestEntity entity = instantiate(AssignmentTestEntity.class);
        entity.setAssignmentId(assignmentId);
        entity.setTestId(testId);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(null);
        entity.setClosedAt(null);
        entity.setClosed(false);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestAttemptEntity testAttemptEntity(Long userId, Long testId, Long assignmentTestId, AttemptMode attemptMode) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(attemptMode);
        entity.setStatus(TestAttemptStatus.STARTED);
        entity.setStartedAt(FIXED_INSTANT);
        entity.setCompletedAt(null);
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(FIXED_INSTANT.plusSeconds(60));
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

    private record AssignedAttemptFixture(Long userId, Long testId, Long assignmentTestId, Long attemptId) {
    }

    private record SelfAttemptFixture(Long userId, Long testId, Long attemptId) {
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AppUserEntity.class,
        CourseEntity.class,
        TopicEntity.class,
        TestEntity.class,
        AssignmentCampaignEntity.class,
        AssignmentEntity.class,
        AssignmentTestEntity.class,
        TestAttemptEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAppUserJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataAssignmentCampaignJpaRepository.class,
        SpringDataAssignmentJpaRepository.class,
        SpringDataAssignmentTestJpaRepository.class,
        SpringDataTestAttemptJpaRepository.class
    })
    @Import({TestingPersistenceMapper.class, JpaTestAttemptRepositoryAdapter.class})
    static class TestingAttemptLockingTestApplication {
    }
}
