package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentPersistenceMapper;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentTestEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.JpaAssignmentRepositoryAdapter;
import com.vladislav.training.platform.assignment.infrastructure.persistence.JpaAssignmentTestRepositoryAdapter;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentTestJpaRepository;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.infrastructure.persistence.AnswerOptionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.QuestionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataAnswerOptionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataQuestionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.infrastructure.persistence.JpaTestAttemptRepositoryAdapter;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataUserAnswerItemJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataUserAnswerJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestingPersistenceMapper;
import com.vladislav.training.platform.testing.infrastructure.persistence.UserAnswerEntity;
import com.vladislav.training.platform.testing.infrastructure.persistence.UserAnswerItemEntity;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = AssignedAttemptLateSubmitAnswerPreservationIntegrationTest.AssignedAttemptLateSubmitTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code AssignedAttemptLateSubmitAnswerPreservation} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class AssignedAttemptLateSubmitAnswerPreservationIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-22T10:00:00Z");
    private static final Instant DEADLINE = FIXED_INSTANT.minusSeconds(600);
    private static final Instant NOW = FIXED_INSTANT;

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
    private SpringDataQuestionJpaRepository questionRepository;
    @Autowired
    private SpringDataAnswerOptionJpaRepository answerOptionRepository;
    @Autowired
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignRepository;
    @Autowired
    private SpringDataAssignmentJpaRepository assignmentRepository;
    @Autowired
    private SpringDataAssignmentTestJpaRepository assignmentTestRepository;
    @Autowired
    private SpringDataTestAttemptJpaRepository testAttemptRepository;
    @Autowired
    private SpringDataUserAnswerJpaRepository userAnswerRepository;
    @Autowired
    private SpringDataUserAnswerItemJpaRepository userAnswerItemRepository;
    @Autowired
    private SpringDataResultJpaRepository resultRepository;
    @Autowired
    private AssignedAttemptSubmitSequencingService assignedAttemptSubmitSequencingService;
    @Autowired
    private UtcClock utcClock;
    @Autowired
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Autowired
    private ResultRecordingService resultRecordingService;

    @AfterEach
    void cleanDatabase() {
        userAnswerItemRepository.deleteAllInBatch();
        userAnswerRepository.deleteAllInBatch();
        resultRepository.deleteAllInBatch();
        testAttemptRepository.deleteAllInBatch();
        assignmentTestRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        assignmentCampaignRepository.deleteAllInBatch();
        answerOptionRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();
        testRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
    }

    @Test
    void lateAssignedSubmitTerminalizesExpiredAndPreservesAnswerRows() {
        Fixture fixture = createFixture();

        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            fixture.userId(),
            fixture.testId(),
            fixture.assignmentTestId(),
            TestAttemptStatus.IN_PROGRESS
        ));
        UserAnswerEntity persistedAnswer = userAnswerRepository.saveAndFlush(
            userAnswerEntity(attempt.getId(), fixture.questionId())
        );
        UserAnswerItemEntity persistedItem = userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(persistedAnswer.getId(), fixture.answerOptionId())
        );

        List<UserAnswerEntity> answersBefore = userAnswerRepository.findAllByTestAttemptIdOrderByIdAsc(attempt.getId());
        List<UserAnswerItemEntity> itemsBefore = userAnswerItemRepository.findAllByUserAnswerIdOrderByIdAsc(persistedAnswer.getId());

        when(utcClock.now()).thenReturn(NOW);

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            assignedAttemptSubmitSequencingService.submitAssignedAttempt(
                fixture.userId(),
                fixture.assignmentTestId(),
                attempt.getId()
            );

        TestAttemptEntity attemptAfter = testAttemptRepository.findById(attempt.getId()).orElseThrow();
        List<UserAnswerEntity> answersAfter = userAnswerRepository.findAllByTestAttemptIdOrderByIdAsc(attempt.getId());
        List<UserAnswerItemEntity> itemsAfter = userAnswerItemRepository.findAllByUserAnswerIdOrderByIdAsc(persistedAnswer.getId());

        assertThat(outcome.attemptId()).isEqualTo(attempt.getId());
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(outcome.recordedResult()).isNull();

        assertThat(attemptAfter.getStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(attemptAfter.getExpiredAt()).isEqualTo(NOW);
        assertThat(attemptAfter.getCompletedAt()).isNull();

        assertThat(answersAfter)
            .hasSize(1)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyElementsOf(answersBefore);
        assertThat(itemsAfter)
            .hasSize(1)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyElementsOf(itemsBefore);

        assertThat(answersAfter.get(0).getId()).isEqualTo(persistedAnswer.getId());
        assertThat(answersAfter.get(0).getQuestionId()).isEqualTo(fixture.questionId());
        assertThat(itemsAfter.get(0).getId()).isEqualTo(persistedItem.getId());
        assertThat(itemsAfter.get(0).getAnswerOptionId()).isEqualTo(fixture.answerOptionId());
        assertThat(itemsAfter.get(0).getLeftAnswerOptionId()).isNull();
        assertThat(itemsAfter.get(0).getRightAnswerOptionId()).isNull();
        assertThat(itemsAfter.get(0).getUserOrderPosition()).isNull();
    }

    @Test
    void expiredAssignedSubmitDoesNotCloseAssignmentTestAsCountedSuccess() {
        Fixture fixture = createFixture();

        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            fixture.userId(),
            fixture.testId(),
            fixture.assignmentTestId(),
            TestAttemptStatus.IN_PROGRESS
        ));
        AssignmentTestEntity assignmentTestBefore = assignmentTestRepository.findById(fixture.assignmentTestId()).orElseThrow();
        AssignmentEntity assignmentBefore = assignmentRepository.findById(fixture.assignmentId()).orElseThrow();

        assertThat(assignmentTestBefore.isClosed()).isFalse();
        assertThat(assignmentTestBefore.getClosedAt()).isNull();
        assertThat(assignmentTestBefore.getCountedResultId()).isNull();
        assertThat(assignmentBefore.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(assignmentBefore.getClosedAt()).isNull();

        when(utcClock.now()).thenReturn(NOW);

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            assignedAttemptSubmitSequencingService.submitAssignedAttempt(
                fixture.userId(),
                fixture.assignmentTestId(),
                attempt.getId()
            );

        AssignmentTestEntity assignmentTestAfter = assignmentTestRepository.findById(fixture.assignmentTestId()).orElseThrow();
        AssignmentEntity assignmentAfter = assignmentRepository.findById(fixture.assignmentId()).orElseThrow();

        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(outcome.recordedResult()).isNull();
        assertThat(assignmentTestAfter.isClosed()).isFalse();
        assertThat(assignmentTestAfter.getClosedAt()).isNull();
        assertThat(assignmentTestAfter.getCountedResultId()).isNull();
        assertThat(assignmentAfter.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(assignmentAfter.getClosedAt()).isNull();
    }

    @Test
    void expiredAssignedSubmitLeavesResultTableUnchanged() {
        Fixture fixture = createFixture();

        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            fixture.userId(),
            fixture.testId(),
            fixture.assignmentTestId(),
            TestAttemptStatus.IN_PROGRESS
        ));

        long resultCountBefore = resultRepository.count();

        when(utcClock.now()).thenReturn(NOW);

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            assignedAttemptSubmitSequencingService.submitAssignedAttempt(
                fixture.userId(),
                fixture.assignmentTestId(),
                attempt.getId()
            );

        long resultCountAfter = resultRepository.count();

        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(outcome.recordedResult()).isNull();
        assertThat(resultCountBefore).isZero();
        assertThat(resultCountAfter).isZero();
        assertThat(resultRepository.findByTestAttemptId(attempt.getId())).isEmpty();
    }

    private Fixture createFixture() {
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-LATE-ASG"));
        CourseEntity course = courseRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId()));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId()));
        QuestionEntity question = questionRepository.saveAndFlush(
            questionEntity(topic.getId(), "Single choice question", QuestionType.SINGLE_CHOICE, 0)
        );
        AnswerOptionEntity answerOption = answerOptionRepository.saveAndFlush(
            answerOptionEntity(question.getId(), "Choice A", AnswerOptionRole.CHOICE_OPTION, true, 0)
        );
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(assignmentCampaignEntity());
        AssignmentEntity assignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), user.getId(), course.getId())
        );
        AssignmentTestEntity assignmentTest = assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(assignment.getId(), test.getId())
        );
        return new Fixture(
            user.getId(),
            test.getId(),
            assignment.getId(),
            assignmentTest.getId(),
            question.getId(),
            answerOption.getId()
        );
    }

    private AppUserEntity appUserEntity(String employeeNumber) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(null);
        entity.setLastName("User");
        entity.setFirstName(employeeNumber);
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(3_600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(3_600));
        return entity;
    }

    private CourseEntity courseEntity() {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Course");
        entity.setDescription("Course description");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(3_600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(3_600));
        return entity;
    }

    private TopicEntity topicEntity(Long courseId) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName("Topic");
        entity.setDescription("Topic description");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(3_600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(3_600));
        return entity;
    }

    private TestEntity testEntity(Long topicId) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName("Test");
        entity.setDescription("Test description");
        entity.setTestType(TestType.CONTROL);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setScoringPolicyCode("DEFAULT_POLICY");
        entity.setActiveFinalForTopic(true);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(3_600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(3_600));
        return entity;
    }

    private QuestionEntity questionEntity(Long topicId, String body, QuestionType questionType, int sortOrder) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody(body);
        entity.setQuestionType(questionType);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(3_600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(3_600));
        return entity;
    }

    private AnswerOptionEntity answerOptionEntity(
        Long questionId,
        String body,
        AnswerOptionRole role,
        Boolean isCorrect,
        int displayOrder
    ) {
        AnswerOptionEntity entity = instantiate(AnswerOptionEntity.class);
        entity.setQuestionId(questionId);
        entity.setBody(body);
        entity.setAnswerOptionRole(role);
        entity.setIsCorrect(isCorrect);
        entity.setDisplayOrder(displayOrder);
        entity.setPairingKey(null);
        entity.setCanonicalOrderPosition(null);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(3_600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(3_600));
        return entity;
    }

    private AssignmentCampaignEntity assignmentCampaignEntity() {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Campaign");
        entity.setDescription("Campaign");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("UNIT-1");
        entity.setSourceNameSnapshot("Operations");
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(3_600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(3_600));
        return entity;
    }

    private AssignmentEntity assignmentEntity(Long campaignId, Long userId, Long courseId) {
        AssignmentEntity entity = instantiate(AssignmentEntity.class);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setStatus(AssignmentStatus.ASSIGNED);
        entity.setAssignedAt(FIXED_INSTANT.minusSeconds(1_800));
        entity.setDeadlineAt(DEADLINE);
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(1_800));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(1_800));
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
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(1_800));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(1_800));
        return entity;
    }

    private TestAttemptEntity testAttemptEntity(
        Long userId,
        Long testId,
        Long assignmentTestId,
        TestAttemptStatus status
    ) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(AttemptMode.ASSIGNED);
        entity.setStatus(status);
        entity.setStartedAt(FIXED_INSTANT.minusSeconds(1_200));
        entity.setCompletedAt(null);
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(FIXED_INSTANT.minusSeconds(900));
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(1_200));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(900));
        return entity;
    }

    private UserAnswerEntity userAnswerEntity(Long testAttemptId, Long questionId) {
        UserAnswerEntity entity = instantiate(UserAnswerEntity.class);
        entity.setTestAttemptId(testAttemptId);
        entity.setQuestionId(questionId);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(800));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(800));
        return entity;
    }

    private UserAnswerItemEntity userAnswerItemEntity(Long userAnswerId, Long answerOptionId) {
        UserAnswerItemEntity entity = instantiate(UserAnswerItemEntity.class);
        entity.setUserAnswerId(userAnswerId);
        entity.setAnswerOptionId(answerOptionId);
        entity.setLeftAnswerOptionId(null);
        entity.setRightAnswerOptionId(null);
        entity.setUserOrderPosition(null);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(700));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(700));
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

    private record Fixture(
        Long userId,
        Long testId,
        Long assignmentId,
        Long assignmentTestId,
        Long questionId,
        Long answerOptionId
    ) {
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AppUserEntity.class,
        CourseEntity.class,
        TopicEntity.class,
        TestEntity.class,
        QuestionEntity.class,
        AnswerOptionEntity.class,
        AssignmentCampaignEntity.class,
        AssignmentEntity.class,
        AssignmentTestEntity.class,
        ResultEntity.class,
        TestAttemptEntity.class,
        UserAnswerEntity.class,
        UserAnswerItemEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAppUserJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataQuestionJpaRepository.class,
        SpringDataAnswerOptionJpaRepository.class,
        SpringDataAssignmentCampaignJpaRepository.class,
        SpringDataAssignmentJpaRepository.class,
        SpringDataAssignmentTestJpaRepository.class,
        SpringDataResultJpaRepository.class,
        SpringDataTestAttemptJpaRepository.class,
        SpringDataUserAnswerJpaRepository.class,
        SpringDataUserAnswerItemJpaRepository.class
    })
    @Import({
        TestingPersistenceMapper.class,
        AssignmentPersistenceMapper.class,
        JpaTestAttemptRepositoryAdapter.class,
        JpaAssignmentRepositoryAdapter.class,
        JpaAssignmentTestRepositoryAdapter.class,
        AttemptStatusRecalculationServiceImpl.class,
        AssignedAttemptSubmitTerminalService.class,
        AssignedAttemptSubmitSequencingService.class
    })
    static class AssignedAttemptLateSubmitTestApplication {

        @Bean
        UtcClock utcClock() {
            return Mockito.mock(UtcClock.class);
        }

        @Bean
        CriticalCommandAuditSupport criticalCommandAuditSupport() {
            return Mockito.mock(CriticalCommandAuditSupport.class);
        }

        @Bean
        ResultRecordingService resultRecordingService() {
            return Mockito.mock(ResultRecordingService.class);
        }
    }
}
