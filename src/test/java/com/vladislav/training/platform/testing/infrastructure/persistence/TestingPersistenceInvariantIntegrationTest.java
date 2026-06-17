package com.vladislav.training.platform.testing.infrastructure.persistence;

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
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
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
    classes = TestingPersistenceInvariantIntegrationTest.TestingPersistenceInvariantTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code TestingPersistenceInvariant} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class TestingPersistenceInvariantIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T10:00:00Z");

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

    @AfterEach
    void cleanDatabase() {
        userAnswerItemRepository.deleteAllInBatch();
        userAnswerRepository.deleteAllInBatch();
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
    void assignedContourRejectsSecondActiveAttemptForSameAssignmentTest() {
        AssignedFixture fixture = createAssignedFixture();
        testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                fixture.assignmentTestId(),
                AttemptMode.ASSIGNED,
                TestAttemptStatus.STARTED
            )
        );

        assertThatThrownBy(() -> testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                fixture.assignmentTestId(),
                AttemptMode.ASSIGNED,
                TestAttemptStatus.IN_PROGRESS
            )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void terminalAssignedAttemptDoesNotBlockNewActiveAttempt() {
        AssignedFixture fixture = createAssignedFixture();
        testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                fixture.assignmentTestId(),
                AttemptMode.ASSIGNED,
                TestAttemptStatus.COMPLETED
            )
        );

        TestAttemptEntity active = testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                fixture.assignmentTestId(),
                AttemptMode.ASSIGNED,
                TestAttemptStatus.STARTED
            )
        );

        assertThat(active.getId()).isNotNull();
    }

    @Test
    void selfContourRejectsSecondActiveAttemptForSameUserAndTest() {
        ContentFixture fixture = createContentFixture();
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-SELF"));
        testAttemptRepository.saveAndFlush(
            testAttemptEntity(user.getId(), fixture.testId(), null, AttemptMode.SELF, TestAttemptStatus.STARTED)
        );

        assertThatThrownBy(() -> testAttemptRepository.saveAndFlush(
            testAttemptEntity(user.getId(), fixture.testId(), null, AttemptMode.SELF, TestAttemptStatus.IN_PROGRESS)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void terminalSelfAttemptDoesNotBlockNewActiveAttempt() {
        ContentFixture fixture = createContentFixture();
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-SELF-2"));
        testAttemptRepository.saveAndFlush(
            testAttemptEntity(user.getId(), fixture.testId(), null, AttemptMode.SELF, TestAttemptStatus.COMPLETED)
        );

        TestAttemptEntity active = testAttemptRepository.saveAndFlush(
            testAttemptEntity(user.getId(), fixture.testId(), null, AttemptMode.SELF, TestAttemptStatus.STARTED)
        );

        assertThat(active.getId()).isNotNull();
    }

    @Test
    void attemptModeAssignmentTestNullabilityConstraintIsEnforced() {
        AssignedFixture fixture = createAssignedFixture();

        assertThatThrownBy(() -> testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                null,
                AttemptMode.ASSIGNED,
                TestAttemptStatus.STARTED
            )
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                fixture.assignmentTestId(),
                AttemptMode.SELF,
                TestAttemptStatus.STARTED
            )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userAnswerRejectsDuplicateQuestionWithinSameAttempt() {
        AssignedFixture fixture = createAssignedFixture();
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                fixture.assignmentTestId(),
                AttemptMode.ASSIGNED,
                TestAttemptStatus.STARTED
            )
        );
        userAnswerRepository.saveAndFlush(userAnswerEntity(attempt.getId(), fixture.choiceQuestionId()));

        assertThatThrownBy(() -> userAnswerRepository.saveAndFlush(
            userAnswerEntity(attempt.getId(), fixture.choiceQuestionId())
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userAnswerForeignKeysAreEnforced() {
        AssignedFixture fixture = createAssignedFixture();
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                fixture.assignmentTestId(),
                AttemptMode.ASSIGNED,
                TestAttemptStatus.STARTED
            )
        );

        assertThatThrownBy(() -> userAnswerRepository.saveAndFlush(userAnswerEntity(999_999L, fixture.choiceQuestionId())))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> userAnswerRepository.saveAndFlush(userAnswerEntity(attempt.getId(), 999_999L)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userAnswerItemRejectsDuplicateChoiceSelection() {
        AssignedFixture fixture = createAssignedFixture();
        TestAttemptEntity attempt = persistAssignedActiveAttempt(fixture);
        Long userAnswerId = persistUserAnswerForQuestion(attempt.getId(), fixture.choiceQuestionId());
        userAnswerItemRepository.saveAndFlush(userAnswerItemEntity(userAnswerId, fixture.choiceOptionId(), null, null, null));

        assertThatThrownBy(() -> userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(userAnswerId, fixture.choiceOptionId(), null, null, null)
        ))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uix_usr_answer_item__choice_option_unique");
    }

    @Test
    void userAnswerItemRejectsDuplicateMatchingLeftAndOrderingPosition() {
        AssignedFixture fixture = createAssignedFixture();
        TestAttemptEntity attempt = persistAssignedActiveAttempt(fixture);
        Long matchingAnswerId = persistUserAnswerForQuestion(attempt.getId(), fixture.matchingQuestionId());
        userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(matchingAnswerId, null, fixture.matchLeftOptionId(), fixture.matchRightOptionId(), null)
        );

        assertThatThrownBy(() -> userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(matchingAnswerId, null, fixture.matchLeftOptionId(), fixture.matchRightOptionId(), null)
        ))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uix_usr_answer_item__matching_left_unique");

        Long orderingAnswerId = persistUserAnswerForQuestion(attempt.getId(), fixture.orderingQuestionId());
        userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(orderingAnswerId, fixture.orderFirstOptionId(), null, null, 0)
        );

        assertThatThrownBy(() -> userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(orderingAnswerId, fixture.orderSecondOptionId(), null, null, 0)
        ))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uix_usr_answer_item__ordering_position_unique");
    }

    @Test
    void userAnswerItemCheckAndForeignKeyConstraintsAreEnforced() {
        AssignedFixture fixture = createAssignedFixture();
        TestAttemptEntity attempt = persistAssignedActiveAttempt(fixture);
        Long userAnswerId = persistUserAnswerForQuestion(attempt.getId(), fixture.orderingQuestionId());

        assertThatThrownBy(() -> userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(userAnswerId, fixture.orderFirstOptionId(), null, null, -1)
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(999_999L, fixture.orderFirstOptionId(), null, null, 0)
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> userAnswerItemRepository.saveAndFlush(
            userAnswerItemEntity(userAnswerId, 999_999L, null, null, 0)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private TestAttemptEntity persistAssignedActiveAttempt(AssignedFixture fixture) {
        return testAttemptRepository.saveAndFlush(
            testAttemptEntity(
                fixture.userId(),
                fixture.testId(),
                fixture.assignmentTestId(),
                AttemptMode.ASSIGNED,
                TestAttemptStatus.STARTED
            )
        );
    }

    private Long persistUserAnswerForQuestion(Long testAttemptId, Long questionId) {
        return userAnswerRepository.saveAndFlush(userAnswerEntity(testAttemptId, questionId)).getId();
    }

    private AssignedFixture createAssignedFixture() {
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-ASG"));
        ContentFixture content = createContentFixture();
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(assignmentCampaignEntity());
        AssignmentEntity assignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), user.getId(), content.courseId())
        );
        AssignmentTestEntity assignmentTest = assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(assignment.getId(), content.testId())
        );
        return new AssignedFixture(
            user.getId(),
            content.courseId(),
            content.testId(),
            assignment.getId(),
            assignmentTest.getId(),
            content.choiceQuestionId(),
            content.choiceOptionId(),
            content.matchingQuestionId(),
            content.matchLeftOptionId(),
            content.matchRightOptionId(),
            content.orderingQuestionId(),
            content.orderFirstOptionId(),
            content.orderSecondOptionId()
        );
    }

    private ContentFixture createContentFixture() {
        CourseEntity course = courseRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId()));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId()));

        QuestionEntity choiceQuestion = questionRepository.saveAndFlush(
            questionEntity(topic.getId(), "Choice question", QuestionType.SINGLE_CHOICE, 0)
        );
        AnswerOptionEntity choiceOption = answerOptionRepository.saveAndFlush(
            answerOptionEntity(choiceQuestion.getId(), "Choice A", AnswerOptionRole.CHOICE_OPTION, true, 0, null, null)
        );

        QuestionEntity matchingQuestion = questionRepository.saveAndFlush(
            questionEntity(topic.getId(), "Matching question", QuestionType.MATCHING, 1)
        );
        AnswerOptionEntity matchLeft = answerOptionRepository.saveAndFlush(
            answerOptionEntity(matchingQuestion.getId(), "Left A", AnswerOptionRole.MATCH_LEFT, null, 0, "A", null)
        );
        AnswerOptionEntity matchRight = answerOptionRepository.saveAndFlush(
            answerOptionEntity(matchingQuestion.getId(), "Right A", AnswerOptionRole.MATCH_RIGHT, null, 1, "A", null)
        );

        QuestionEntity orderingQuestion = questionRepository.saveAndFlush(
            questionEntity(topic.getId(), "Ordering question", QuestionType.ORDERING, 2)
        );
        AnswerOptionEntity orderFirst = answerOptionRepository.saveAndFlush(
            answerOptionEntity(orderingQuestion.getId(), "Order 1", AnswerOptionRole.ORDER_ITEM, null, 0, null, 0)
        );
        AnswerOptionEntity orderSecond = answerOptionRepository.saveAndFlush(
            answerOptionEntity(orderingQuestion.getId(), "Order 2", AnswerOptionRole.ORDER_ITEM, null, 1, null, 1)
        );

        return new ContentFixture(
            course.getId(),
            topic.getId(),
            test.getId(),
            choiceQuestion.getId(),
            choiceOption.getId(),
            matchingQuestion.getId(),
            matchLeft.getId(),
            matchRight.getId(),
            orderingQuestion.getId(),
            orderFirst.getId(),
            orderSecond.getId()
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
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private CourseEntity courseEntity() {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Course");
        entity.setDescription("Course description");
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
        entity.setDescription("Topic description");
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
        entity.setDescription("Test description");
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

    private QuestionEntity questionEntity(Long topicId, String body, QuestionType questionType, int sortOrder) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody(body);
        entity.setQuestionType(questionType);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AnswerOptionEntity answerOptionEntity(
        Long questionId,
        String body,
        AnswerOptionRole role,
        Boolean isCorrect,
        int displayOrder,
        String pairingKey,
        Integer canonicalOrderPosition
    ) {
        AnswerOptionEntity entity = instantiate(AnswerOptionEntity.class);
        entity.setQuestionId(questionId);
        entity.setBody(body);
        entity.setAnswerOptionRole(role);
        entity.setIsCorrect(isCorrect);
        entity.setDisplayOrder(displayOrder);
        entity.setPairingKey(pairingKey);
        entity.setCanonicalOrderPosition(canonicalOrderPosition);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignEntity assignmentCampaignEntity() {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Кампания тестового контура");
        entity.setDescription("Campaign");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("UNIT-1");
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

    private TestAttemptEntity testAttemptEntity(
        Long userId,
        Long testId,
        Long assignmentTestId,
        AttemptMode attemptMode,
        TestAttemptStatus status
    ) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(attemptMode);
        entity.setStatus(status);
        entity.setStartedAt(FIXED_INSTANT);
        entity.setCompletedAt(status == TestAttemptStatus.COMPLETED ? FIXED_INSTANT.plusSeconds(600) : null);
        entity.setExpiredAt(status == TestAttemptStatus.EXPIRED ? FIXED_INSTANT.plusSeconds(600) : null);
        entity.setAbandonedAt(status == TestAttemptStatus.ABANDONED ? FIXED_INSTANT.plusSeconds(600) : null);
        entity.setLastActivityAt(FIXED_INSTANT.plusSeconds(60));
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private UserAnswerEntity userAnswerEntity(Long testAttemptId, Long questionId) {
        UserAnswerEntity entity = instantiate(UserAnswerEntity.class);
        entity.setTestAttemptId(testAttemptId);
        entity.setQuestionId(questionId);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private UserAnswerItemEntity userAnswerItemEntity(
        Long userAnswerId,
        Long answerOptionId,
        Long leftAnswerOptionId,
        Long rightAnswerOptionId,
        Integer userOrderPosition
    ) {
        UserAnswerItemEntity entity = instantiate(UserAnswerItemEntity.class);
        entity.setUserAnswerId(userAnswerId);
        entity.setAnswerOptionId(answerOptionId);
        entity.setLeftAnswerOptionId(leftAnswerOptionId);
        entity.setRightAnswerOptionId(rightAnswerOptionId);
        entity.setUserOrderPosition(userOrderPosition);
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

    private record ContentFixture(
        Long courseId,
        Long topicId,
        Long testId,
        Long choiceQuestionId,
        Long choiceOptionId,
        Long matchingQuestionId,
        Long matchLeftOptionId,
        Long matchRightOptionId,
        Long orderingQuestionId,
        Long orderFirstOptionId,
        Long orderSecondOptionId
    ) {
    }

    private record AssignedFixture(
        Long userId,
        Long courseId,
        Long testId,
        Long assignmentId,
        Long assignmentTestId,
        Long choiceQuestionId,
        Long choiceOptionId,
        Long matchingQuestionId,
        Long matchLeftOptionId,
        Long matchRightOptionId,
        Long orderingQuestionId,
        Long orderFirstOptionId,
        Long orderSecondOptionId
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
        SpringDataTestAttemptJpaRepository.class,
        SpringDataUserAnswerJpaRepository.class,
        SpringDataUserAnswerItemJpaRepository.class
    })
    static class TestingPersistenceInvariantTestApplication {
    }
}
