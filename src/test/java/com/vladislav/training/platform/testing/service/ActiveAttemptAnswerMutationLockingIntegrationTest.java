package com.vladislav.training.platform.testing.service;

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
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.infrastructure.persistence.JpaTestAttemptRepositoryAdapter;
import com.vladislav.training.platform.testing.infrastructure.persistence.JpaUserAnswerItemRepositoryAdapter;
import com.vladislav.training.platform.testing.infrastructure.persistence.JpaUserAnswerRepositoryAdapter;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataUserAnswerItemJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataUserAnswerJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestingPersistenceMapper;
import com.vladislav.training.platform.testing.infrastructure.persistence.UserAnswerEntity;
import com.vladislav.training.platform.testing.infrastructure.persistence.UserAnswerItemEntity;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
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
    classes = ActiveAttemptAnswerMutationLockingIntegrationTest.ActiveAttemptAnswerMutationLockingTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code ActiveAttemptAnswerMutationLocking} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class ActiveAttemptAnswerMutationLockingIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-22T10:00:00Z");

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
    private SpringDataTestAttemptJpaRepository testAttemptJpaRepository;
    @Autowired
    private SpringDataUserAnswerJpaRepository userAnswerJpaRepository;
    @Autowired
    private SpringDataUserAnswerItemJpaRepository userAnswerItemJpaRepository;
    @Autowired
    private TestAttemptRepository testAttemptRepository;
    @Autowired
    private ActiveAttemptAnswerMutationService activeAttemptAnswerMutationService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        userAnswerItemJpaRepository.deleteAllInBatch();
        userAnswerJpaRepository.deleteAllInBatch();
        testAttemptJpaRepository.deleteAllInBatch();
        assignmentTestRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        assignmentCampaignRepository.deleteAllInBatch();
        testRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
    }

    @Test
    void saveOrReplaceAnswerBlocksOnExistingAttemptLockAndTimesOutContender() throws Exception {
        AssignedAttemptFixture fixture = createAssignedAttemptFixture();

        Throwable failure = captureConcurrentMutationLockTimeout(
            () -> assertThat(testAttemptRepository.findAndLockTestAttemptById(fixture.attemptId()).id())
                .isEqualTo(fixture.attemptId()),
            () -> activeAttemptAnswerMutationService.saveOrReplaceAnswer(
                fixture.userId(),
                fixture.attemptId(),
                fixture.questionId(),
                List.of(new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null)),
                FIXED_INSTANT.plusSeconds(120)
            )
        );

        assertThat(failure).isNotNull();
        assertThat(failure).isInstanceOfAny(DataAccessException.class, RuntimeException.class);
        assertThat(failure.getMessage().toLowerCase()).contains("lock");
    }

    private Throwable captureConcurrentMutationLockTimeout(Runnable holderAction, Runnable contenderAction)
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
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-ANS-LOCK"));
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
        TestAttemptEntity attempt = testAttemptJpaRepository.saveAndFlush(
            testAttemptEntity(user.getId(), test.getId(), assignmentTest.getId())
        );
        return new AssignedAttemptFixture(user.getId(), test.getId(), assignmentTest.getId(), attempt.getId(), 501L);
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
        entity.setSourceRef("UNIT-ANSWER-LOCK");
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

    private TestAttemptEntity testAttemptEntity(Long userId, Long testId, Long assignmentTestId) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(AttemptMode.ASSIGNED);
        entity.setStatus(TestAttemptStatus.STARTED);
        entity.setStartedAt(FIXED_INSTANT);
        entity.setCompletedAt(null);
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(FIXED_INSTANT.plusSeconds(30));
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT.plusSeconds(30));
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

    private record AssignedAttemptFixture(Long userId, Long testId, Long assignmentTestId, Long attemptId, Long questionId) {
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
        TestAttemptEntity.class,
        UserAnswerEntity.class,
        UserAnswerItemEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAppUserJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataAssignmentCampaignJpaRepository.class,
        SpringDataAssignmentJpaRepository.class,
        SpringDataAssignmentTestJpaRepository.class,
        SpringDataTestAttemptJpaRepository.class,
        SpringDataUserAnswerJpaRepository.class,
        SpringDataUserAnswerItemJpaRepository.class
    })
    @Import({
        TestingPersistenceMapper.class,
        JpaTestAttemptRepositoryAdapter.class,
        JpaUserAnswerRepositoryAdapter.class,
        JpaUserAnswerItemRepositoryAdapter.class,
        ActiveAttemptAnswerMutationService.class
    })
    static class ActiveAttemptAnswerMutationLockingTestApplication {

        @Bean
        TestQuestionRepository testQuestionRepository() {
            return new TestQuestionRepository() {
                @Override
                public TestQuestion findTestQuestionById(Long testQuestionId) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public List<TestQuestion> findTestQuestionsByTestId(Long testId) {
                    return List.of(new TestQuestion(9001L, testId, 501L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT));
                }

                @Override
                public boolean existsPublishedTestUsingQuestion(Long questionId) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public TestQuestion saveTestQuestion(TestQuestion testQuestion) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public void deleteTestQuestion(Long testQuestionId) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }
            };
        }

        @Bean
        QuestionRepository questionRepository() {
            return new QuestionRepository() {
                @Override
                public Question findQuestionById(Long questionId) {
                    return new Question(
                        questionId,
                        8001L,
                        "Locking contract question",
                        QuestionType.SINGLE_CHOICE,
                        ContentStatus.PUBLISHED,
                        0,
                        FIXED_INSTANT,
                        FIXED_INSTANT
                    );
                }

                @Override
                public List<Question> findQuestionsByTopicId(Long topicId) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public List<Question> findQuestionsByTopicIdAndStatus(Long topicId, ContentStatus status) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public List<Question> findQuestionsByIds(java.util.Collection<Long> questionIds) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public boolean existsNonArchivedByTopicId(Long topicId) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public Question saveQuestion(Question question) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }
            };
        }

        @Bean
        AnswerOptionRepository answerOptionRepository() {
            return new AnswerOptionRepository() {
                @Override
                public AnswerOption findAnswerOptionById(Long answerOptionId) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public List<AnswerOption> findAnswerOptionsByQuestionId(Long questionId) {
                    return List.of(
                        new AnswerOption(
                            7001L,
                            questionId,
                            "Choice option",
                            AnswerOptionRole.CHOICE_OPTION,
                            Boolean.TRUE,
                            0,
                            null,
                            null,
                            FIXED_INSTANT,
                            FIXED_INSTANT
                        )
                    );
                }

                @Override
                public AnswerOption saveAnswerOption(AnswerOption answerOption) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }

                @Override
                public void deleteAnswerOption(Long answerOptionId) {
                    throw new UnsupportedOperationException("Not required for locking integration contract");
                }
            };
        }
    }
}
