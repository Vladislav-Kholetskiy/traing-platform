package com.vladislav.training.platform.result.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentTestEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentTestJpaRepository;
import com.vladislav.training.platform.common.model.AttemptMode;
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
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
    classes = ResultPersistenceInvariantIntegrationTest.ResultPersistenceInvariantTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code ResultPersistenceInvariant} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class ResultPersistenceInvariantIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T14:00:00Z");

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
    private SpringDataResultJpaRepository resultRepository;
    @Autowired
    private SpringDataResultQuestionSnapshotJpaRepository resultQuestionSnapshotRepository;
    @Autowired
    private SpringDataResultAnswerOptionSnapshotJpaRepository resultAnswerOptionSnapshotRepository;

    @AfterEach
    void cleanDatabase() {
        resultAnswerOptionSnapshotRepository.deleteAllInBatch();
        resultQuestionSnapshotRepository.deleteAllInBatch();
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
    void resultRejectsDuplicateByTestAttemptId() {
        AssignedResultFixture fixture = createAssignedResultFixture();
        resultRepository.saveAndFlush(resultEntity(
            fixture.attemptId(),
            fixture.userId(),
            AttemptMode.ASSIGNED,
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            true,
            true
        ));

        assertThatThrownBy(() -> resultRepository.saveAndFlush(resultEntity(
            fixture.attemptId(),
            fixture.userId(),
            AttemptMode.ASSIGNED,
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            true,
            true
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void resultAttemptModeNullabilityAndDeadlineConstraintsAreEnforced() {
        AssignedResultFixture assignedFixture = createAssignedResultFixture();
        SelfResultFixture selfFixture = createSelfResultFixture();

        assertThatThrownBy(() -> resultRepository.saveAndFlush(resultEntity(
            assignedFixture.attemptId(),
            assignedFixture.userId(),
            AttemptMode.ASSIGNED,
            assignedFixture.assignmentId(),
            null,
            true,
            true
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultRepository.saveAndFlush(resultEntity(
            assignedFixture.attemptId(),
            assignedFixture.userId(),
            AttemptMode.ASSIGNED,
            assignedFixture.assignmentId(),
            assignedFixture.assignmentTestId(),
            null,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultRepository.saveAndFlush(resultEntity(
            selfFixture.attemptId(),
            selfFixture.userId(),
            AttemptMode.SELF,
            null,
            null,
            true,
            true
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void resultForeignKeysAreEnforced() {
        AssignedResultFixture fixture = createAssignedResultFixture();

        assertThatThrownBy(() -> resultRepository.saveAndFlush(resultEntity(
            999_999L,
            fixture.userId(),
            AttemptMode.ASSIGNED,
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            true,
            true
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultRepository.saveAndFlush(resultEntity(
            fixture.attemptId(),
            fixture.userId(),
            AttemptMode.ASSIGNED,
            999_999L,
            fixture.assignmentTestId(),
            true,
            true
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultRepository.saveAndFlush(resultEntity(
            fixture.attemptId(),
            fixture.userId(),
            AttemptMode.ASSIGNED,
            fixture.assignmentId(),
            999_999L,
            true,
            true
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void resultQuestionSnapshotRejectsDuplicateOriginalQuestionAndDisplayOrder() {
        AssignedResultFixture fixture = createAssignedResultFixture();
        ResultEntity result = resultRepository.saveAndFlush(resultEntity(
            fixture.attemptId(),
            fixture.userId(),
            AttemptMode.ASSIGNED,
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            true,
            true
        ));
        resultQuestionSnapshotRepository.saveAndFlush(
            resultQuestionSnapshotEntity(result.getId(), fixture.questionId(), 0, "Question body", new BigDecimal("1.0000"))
        );

        assertThatThrownBy(() -> resultQuestionSnapshotRepository.saveAndFlush(
            resultQuestionSnapshotEntity(result.getId(), fixture.questionId(), 1, "Other body", new BigDecimal("1.0000"))
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultQuestionSnapshotRepository.saveAndFlush(
            resultQuestionSnapshotEntity(result.getId(), fixture.secondQuestionId(), 0, "Third body", new BigDecimal("1.0000"))
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void resultQuestionSnapshotCheckConstraintsAreEnforced() {
        AssignedResultFixture fixture = createAssignedResultFixture();
        ResultEntity result = resultRepository.saveAndFlush(resultEntity(
            fixture.attemptId(),
            fixture.userId(),
            AttemptMode.ASSIGNED,
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            true,
            true
        ));

        assertThatThrownBy(() -> resultQuestionSnapshotRepository.saveAndFlush(
            resultQuestionSnapshotEntity(result.getId(), fixture.questionId(), -1, "Question body", new BigDecimal("1.0000"))
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultQuestionSnapshotRepository.saveAndFlush(
            resultQuestionSnapshotEntity(result.getId(), fixture.questionId(), 1, "   ", new BigDecimal("1.0000"))
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultQuestionSnapshotRepository.saveAndFlush(
            resultQuestionSnapshotEntity(result.getId(), fixture.questionId(), 1, "Question body", BigDecimal.ZERO)
        )).isInstanceOf(DataIntegrityViolationException.class);

        ResultQuestionSnapshotEntity negativeEarned = resultQuestionSnapshotEntity(
            result.getId(),
            fixture.questionId(),
            1,
            "Question body",
            new BigDecimal("1.0000")
        );
        negativeEarned.setEarnedScore(new BigDecimal("-0.1000"));
        assertThatThrownBy(() -> resultQuestionSnapshotRepository.saveAndFlush(negativeEarned))
            .isInstanceOf(DataIntegrityViolationException.class);

        ResultQuestionSnapshotEntity zeroMax = resultQuestionSnapshotEntity(
            result.getId(),
            fixture.questionId(),
            1,
            "Question body",
            new BigDecimal("1.0000")
        );
        zeroMax.setMaxScore(BigDecimal.ZERO);
        assertThatThrownBy(() -> resultQuestionSnapshotRepository.saveAndFlush(zeroMax))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void resultAnswerOptionSnapshotRejectsDuplicateDisplayOrderAndOriginalOption() {
        AssignedResultFixture fixture = createAssignedResultFixture();
        ResultEntity result = resultRepository.saveAndFlush(resultEntity(
            fixture.attemptId(),
            fixture.userId(),
            AttemptMode.ASSIGNED,
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            true,
            true
        ));
        ResultQuestionSnapshotEntity questionSnapshot = resultQuestionSnapshotRepository.saveAndFlush(
            resultQuestionSnapshotEntity(result.getId(), fixture.questionId(), 0, "Question body", new BigDecimal("1.0000"))
        );
        resultAnswerOptionSnapshotRepository.saveAndFlush(
            resultAnswerOptionSnapshotEntity(questionSnapshot.getId(), fixture.answerOptionId(), 0, "Option A")
        );

        assertThatThrownBy(() -> resultAnswerOptionSnapshotRepository.saveAndFlush(
            resultAnswerOptionSnapshotEntity(questionSnapshot.getId(), fixture.secondAnswerOptionId(), 0, "Option B")
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultAnswerOptionSnapshotRepository.saveAndFlush(
            resultAnswerOptionSnapshotEntity(questionSnapshot.getId(), fixture.answerOptionId(), 1, "Option C")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void resultAnswerOptionSnapshotCheckAndForeignKeyConstraintsAreEnforced() {
        AssignedResultFixture fixture = createAssignedResultFixture();
        ResultEntity result = resultRepository.saveAndFlush(resultEntity(
            fixture.attemptId(),
            fixture.userId(),
            AttemptMode.ASSIGNED,
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            true,
            true
        ));
        ResultQuestionSnapshotEntity questionSnapshot = resultQuestionSnapshotRepository.saveAndFlush(
            resultQuestionSnapshotEntity(result.getId(), fixture.questionId(), 0, "Question body", new BigDecimal("1.0000"))
        );

        assertThatThrownBy(() -> resultAnswerOptionSnapshotRepository.saveAndFlush(
            resultAnswerOptionSnapshotEntity(questionSnapshot.getId(), fixture.answerOptionId(), -1, "Option A")
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultAnswerOptionSnapshotRepository.saveAndFlush(
            resultAnswerOptionSnapshotEntity(questionSnapshot.getId(), fixture.answerOptionId(), 1, "   ")
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultAnswerOptionSnapshotRepository.saveAndFlush(
            resultAnswerOptionSnapshotEntity(999_999L, fixture.answerOptionId(), 1, "Option A")
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> resultAnswerOptionSnapshotRepository.saveAndFlush(
            resultAnswerOptionSnapshotEntity(questionSnapshot.getId(), 999_999L, 1, "Option A")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private AssignedResultFixture createAssignedResultFixture() {
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-RESULT-ASG"));
        CourseEntity course = courseRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId()));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId()));
        QuestionEntity question = questionRepository.saveAndFlush(questionEntity(topic.getId(), "Question A", 0));
        QuestionEntity secondQuestion = questionRepository.saveAndFlush(questionEntity(topic.getId(), "Question B", 1));
        AnswerOptionEntity answerOption = answerOptionRepository.saveAndFlush(
            answerOptionEntity(question.getId(), "Option A", 0)
        );
        AnswerOptionEntity secondAnswerOption = answerOptionRepository.saveAndFlush(
            answerOptionEntity(question.getId(), "Option B", 1)
        );
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
        return new AssignedResultFixture(
            user.getId(),
            assignment.getId(),
            assignmentTest.getId(),
            attempt.getId(),
            question.getId(),
            secondQuestion.getId(),
            answerOption.getId(),
            secondAnswerOption.getId()
        );
    }

    private SelfResultFixture createSelfResultFixture() {
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-RESULT-SELF"));
        CourseEntity course = courseRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId()));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId()));
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(
            testAttemptEntity(user.getId(), test.getId(), null, AttemptMode.SELF)
        );
        return new SelfResultFixture(user.getId(), attempt.getId());
    }

    private ResultEntity resultEntity(
        Long testAttemptId,
        Long userIdSnapshot,
        AttemptMode attemptMode,
        Long assignmentId,
        Long assignmentTestId,
        Boolean withinDeadline,
        Boolean countedInAssignment
    ) {
        ResultEntity entity = instantiate(ResultEntity.class);
        entity.setTestAttemptId(testAttemptId);
        entity.setAttemptMode(attemptMode);
        entity.setAssignmentId(assignmentId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setTestIdSnapshot(601L);
        entity.setTestNameSnapshot("Persisted Test");
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setEarnedScore(new BigDecimal("8.0000"));
        entity.setMaxScore(new BigDecimal("10.0000"));
        entity.setScorePercent(new BigDecimal("80.0000"));
        entity.setPassed(true);
        entity.setUserIdSnapshot(userIdSnapshot);
        entity.setWithinDeadline(withinDeadline);
        entity.setCountedInAssignment(countedInAssignment);
        entity.setScoringPolicyCode("DEFAULT_POLICY");
        entity.setScoringPolicySnapshot("{\"policy\":\"v1\"}");
        entity.setCompletedAt(FIXED_INSTANT.plusSeconds(120));
        entity.setOrganizationalUnitIdSnapshot(501L);
        entity.setOrganizationalPathSnapshot("/company/ops");
        entity.setSnapshotFinalTopicControlFlag(true);
        entity.setCreatedAt(FIXED_INSTANT.plusSeconds(180));
        return entity;
    }

    private ResultQuestionSnapshotEntity resultQuestionSnapshotEntity(
        Long resultId,
        Long questionOriginalId,
        int displayOrder,
        String body,
        BigDecimal weight
    ) {
        ResultQuestionSnapshotEntity entity = instantiate(ResultQuestionSnapshotEntity.class);
        entity.setResultId(resultId);
        entity.setQuestionOriginalId(questionOriginalId);
        entity.setBody(body);
        entity.setQuestionType(ResultQuestionType.SINGLE_CHOICE);
        entity.setDisplayOrder(displayOrder);
        entity.setWeight(weight);
        entity.setCorrectAnswerSnapshot("{\"correct\":1}");
        entity.setUserAnswerSnapshot("{\"selected\":1}");
        entity.setEarnedScore(new BigDecimal("1.0000"));
        entity.setMaxScore(new BigDecimal("1.0000"));
        entity.setCorrect(true);
        entity.setEvaluationNote(null);
        entity.setCreatedAt(FIXED_INSTANT.plusSeconds(240));
        return entity;
    }

    private ResultAnswerOptionSnapshotEntity resultAnswerOptionSnapshotEntity(
        Long resultQuestionSnapshotId,
        Long answerOptionOriginalId,
        int displayOrder,
        String body
    ) {
        ResultAnswerOptionSnapshotEntity entity = instantiate(ResultAnswerOptionSnapshotEntity.class);
        entity.setResultQuestionSnapshotId(resultQuestionSnapshotId);
        entity.setAnswerOptionOriginalId(answerOptionOriginalId);
        entity.setBody(body);
        entity.setDisplayOrder(displayOrder);
        entity.setCorrectAtSnapshot(displayOrder == 0);
        entity.setSelectedByUser(displayOrder == 0);
        entity.setCreatedAt(FIXED_INSTANT.plusSeconds(300));
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

    private QuestionEntity questionEntity(Long topicId, String body, int sortOrder) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody(body);
        entity.setQuestionType(QuestionType.SINGLE_CHOICE);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AnswerOptionEntity answerOptionEntity(Long questionId, String body, int displayOrder) {
        AnswerOptionEntity entity = instantiate(AnswerOptionEntity.class);
        entity.setQuestionId(questionId);
        entity.setBody(body);
        entity.setAnswerOptionRole(AnswerOptionRole.CHOICE_OPTION);
        entity.setIsCorrect(displayOrder == 0);
        entity.setDisplayOrder(displayOrder);
        entity.setPairingKey(null);
        entity.setCanonicalOrderPosition(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignEntity assignmentCampaignEntity() {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Campaign");
        entity.setDescription("Campaign");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("UNIT-RESULT");
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
        entity.setStatus(TestAttemptStatus.COMPLETED);
        entity.setStartedAt(FIXED_INSTANT);
        entity.setCompletedAt(FIXED_INSTANT.plusSeconds(120));
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

    private record AssignedResultFixture(
        Long userId,
        Long assignmentId,
        Long assignmentTestId,
        Long attemptId,
        Long questionId,
        Long secondQuestionId,
        Long answerOptionId,
        Long secondAnswerOptionId
    ) {
    }

    private record SelfResultFixture(Long userId, Long attemptId) {
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
        TestAttemptEntity.class,
        ResultEntity.class,
        ResultQuestionSnapshotEntity.class,
        ResultAnswerOptionSnapshotEntity.class
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
        SpringDataTestAttemptJpaRepository.class,
        SpringDataResultJpaRepository.class,
        SpringDataResultQuestionSnapshotJpaRepository.class,
        SpringDataResultAnswerOptionSnapshotJpaRepository.class
    })
    static class ResultPersistenceInvariantTestApplication {
    }
}
