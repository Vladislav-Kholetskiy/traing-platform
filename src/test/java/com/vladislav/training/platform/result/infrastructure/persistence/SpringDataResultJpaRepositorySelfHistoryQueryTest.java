package com.vladislav.training.platform.result.infrastructure.persistence;

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
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = SpringDataResultJpaRepositorySelfHistoryQueryTest.ResultSelfHistoryQueryTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет поведение {@code SpringDataResultJpaRepositorySelfHistoryQuery}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class SpringDataResultJpaRepositorySelfHistoryQueryTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T12:00:00Z");

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
    private SpringDataTestAttemptJpaRepository testAttemptRepository;
    @Autowired
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignRepository;
    @Autowired
    private SpringDataAssignmentJpaRepository assignmentRepository;
    @Autowired
    private SpringDataAssignmentTestJpaRepository assignmentTestRepository;
    @Autowired
    private SpringDataResultJpaRepository resultRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        resultRepository.deleteAllInBatch();
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
    void repositoryPublishesResultRootedSelfHistoryQueryByImmutableActorAnchorWithoutMandatoryAttemptModeFilter() {
        Method queryMethod = Arrays.stream(SpringDataResultJpaRepository.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc"))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "SpringDataResultJpaRepository must publish findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc"
            ));

        assertThat(queryMethod.getReturnType()).isEqualTo(List.class);
        assertThat(queryMethod.getParameterTypes()).containsExactly(Long.class);
        assertThat(Arrays.stream(queryMethod.getParameterTypes()).map(Class::getSimpleName).toList())
            
            .doesNotContain("AttemptMode");
    }

    @Test
    void repositorySummaryQueryReturnsAllActorOwnedHistoricalResultsSortedByCompletedAtDescThenIdDesc() {
        ActorFixture actor = createActorFixture("self-history-user");
        SelfResultFixture earlierSelf = createSelfResultFixture(actor.userId(), "self-earlier", FIXED_INSTANT.minusSeconds(300));
        SelfResultFixture assignedAtSharedTimestamp = createAssignedResultFixture(
            actor.userId(),
            "assigned-same-timestamp",
            FIXED_INSTANT
        );
        SelfResultFixture selfAtSharedTimestamp = createSelfResultFixture(
            actor.userId(),
            "self-same-timestamp",
            FIXED_INSTANT
        );
        ActorFixture otherActor = createActorFixture("other-history-user");
        createSelfResultFixture(otherActor.userId(), "other-user-result", FIXED_INSTANT.plusSeconds(60));

        List<SpringDataResultJpaRepository.ResultHistorySummaryRowView> results =
            resultRepository.findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(actor.userId());

        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::getResultId)
            .containsExactly(
                selfAtSharedTimestamp.resultId(),
                assignedAtSharedTimestamp.resultId(),
                earlierSelf.resultId()
            );
        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::getTestAttemptId)
            .containsExactly(
                selfAtSharedTimestamp.attemptId(),
                assignedAtSharedTimestamp.attemptId(),
                earlierSelf.attemptId()
            );
        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::getAttemptMode)
            .containsExactly(AttemptMode.SELF, AttemptMode.ASSIGNED, AttemptMode.SELF);
        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::getRecordedAt)
            .containsExactly(
                FIXED_INSTANT,
                FIXED_INSTANT,
                FIXED_INSTANT.minusSeconds(300)
            );
        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::getTestId)
            .doesNotContainNull();
        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::getTestName)
            .containsExactly("Test self-same-timestamp", "Test assigned-same-timestamp", "Test self-earlier");
        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::getAssignmentId)
            .containsExactly(null, assignedAtSharedTimestamp.assignmentId(), null);
        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::getScorePercent)
            .allSatisfy(value -> assertThat(value).isEqualByComparingTo("70.0000"));
        assertThat(results)
            .extracting(SpringDataResultJpaRepository.ResultHistorySummaryRowView::isPassed)
            .containsOnly(true);
        assertThat(selfAtSharedTimestamp.resultId()).isGreaterThan(assignedAtSharedTimestamp.resultId());
    }

    @Test
    void repositorySurfaceDoesNotExposeAttemptSideScn21HistoryFinderContract() {
        List<String> declaredMethodNames = Arrays.stream(SpringDataResultJpaRepository.class.getDeclaredMethods())
            .map(Method::getName)
            .toList();

        assertThat(declaredMethodNames)
            .doesNotContain("findAllByUserIdAndTestIdOrderByIdAsc")
            .doesNotContain("findByUserIdAndTestIdAndAttemptModeAndStatusIn")
            .doesNotContain("findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn");
    }

    private ActorFixture createActorFixture(String employeeNumber) {
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity(employeeNumber));
        return new ActorFixture(user.getId());
    }

    private SelfResultFixture createSelfResultFixture(Long userId, String suffix, Instant completedAt) {
        CourseEntity course = courseRepository.saveAndFlush(courseEntity(suffix));
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId(), suffix));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId(), suffix, TestType.TRAINING));
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(
            testAttemptEntity(userId, test.getId(), null, AttemptMode.SELF, completedAt)
        );
        Long resultId = insertResultRow(
            userId,
            attempt.getId(),
            AttemptMode.SELF,
            null,
            null,
            test.getId(),
            test.getName(),
            completedAt,
            "/company/self"
        );
        return new SelfResultFixture(userId, attempt.getId(), resultId, null);
    }

    private SelfResultFixture createAssignedResultFixture(Long userId, String suffix, Instant completedAt) {
        CourseEntity course = courseRepository.saveAndFlush(courseEntity(suffix));
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId(), suffix));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId(), suffix, TestType.CONTROL));
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(assignmentCampaignEntity(suffix));
        AssignmentEntity assignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), userId, course.getId(), completedAt)
        );
        AssignmentTestEntity assignmentTest = assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(assignment.getId(), test.getId(), completedAt)
        );
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(
            testAttemptEntity(userId, test.getId(), assignmentTest.getId(), AttemptMode.ASSIGNED, completedAt)
        );
        Long resultId = insertResultRow(
            userId,
            attempt.getId(),
            AttemptMode.ASSIGNED,
            assignment.getId(),
            assignmentTest.getId(),
            test.getId(),
            test.getName(),
            completedAt,
            "/company/assigned"
        );
        return new SelfResultFixture(userId, attempt.getId(), resultId, assignment.getId());
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

    private CourseEntity courseEntity(String suffix) {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Course " + suffix);
        entity.setDescription("Course");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId, String suffix) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName("Topic " + suffix);
        entity.setDescription("Topic");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestEntity testEntity(Long topicId, String suffix, TestType testType) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName("Test " + suffix);
        entity.setDescription("Test");
        entity.setTestType(testType);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setScoringPolicyCode("STANDARD");
        entity.setActiveFinalForTopic(false);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignEntity assignmentCampaignEntity(String suffix) {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Campaign " + suffix);
        entity.setDescription("Campaign");
        entity.setSourceType("MANUAL");
        entity.setSourceRef("SRC-" + suffix);
        entity.setSourceNameSnapshot("Campaign " + suffix);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentEntity assignmentEntity(Long campaignId, Long userId, Long courseId, Instant completedAt) {
        AssignmentEntity entity = instantiate(AssignmentEntity.class);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setStatus(AssignmentStatus.ASSIGNED);
        entity.setAssignedAt(completedAt.minusSeconds(900));
        entity.setDeadlineAt(completedAt.plusSeconds(86_400));
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
        entity.setCreatedAt(completedAt.minusSeconds(900));
        entity.setUpdatedAt(completedAt);
        return entity;
    }

    private AssignmentTestEntity assignmentTestEntity(Long assignmentId, Long testId, Instant completedAt) {
        AssignmentTestEntity entity = instantiate(AssignmentTestEntity.class);
        entity.setAssignmentId(assignmentId);
        entity.setTestId(testId);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(null);
        entity.setClosedAt(null);
        entity.setClosed(false);
        entity.setCreatedAt(completedAt.minusSeconds(900));
        entity.setUpdatedAt(completedAt);
        return entity;
    }

    private TestAttemptEntity testAttemptEntity(
        Long userId,
        Long testId,
        Long assignmentTestId,
        AttemptMode attemptMode,
        Instant completedAt
    ) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(attemptMode);
        entity.setStatus(TestAttemptStatus.COMPLETED);
        entity.setStartedAt(completedAt.minusSeconds(300));
        entity.setCompletedAt(completedAt);
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(completedAt.minusSeconds(30));
        entity.setCreatedAt(completedAt.minusSeconds(300));
        entity.setUpdatedAt(completedAt);
        return entity;
    }

    private Long insertResultRow(
        Long userIdSnapshot,
        Long testAttemptId,
        AttemptMode attemptMode,
        Long assignmentId,
        Long assignmentTestId,
        Long testIdSnapshot,
        String testNameSnapshot,
        Instant completedAt,
        String organizationalPathSnapshot
    ) {
        return jdbcTemplate.queryForObject(
            """
                insert into result (
                    test_attempt_id,
                    user_id_snapshot,
                    attempt_mode,
                    assignment_id,
                    assignment_test_id,
                    test_id_snapshot,
                    test_name_snapshot,
                    threshold_percent,
                    earned_score,
                    max_score,
                    score_percent,
                    passed,
                    within_deadline,
                    counted_in_assignment,
                    scoring_policy_code,
                    scoring_policy_snapshot,
                    completed_at,
                    organizational_unit_id_snapshot,
                    organizational_path_snapshot,
                    snapshot_final_topic_control_flag,
                    created_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?, ?, ?, ?, ?)
                returning id
                """,
            Long.class,
            testAttemptId,
            userIdSnapshot,
            attemptMode.name(),
            assignmentId,
            assignmentTestId,
            testIdSnapshot,
            testNameSnapshot,
            new BigDecimal("70.0000"),
            new BigDecimal("7.0000"),
            new BigDecimal("10.0000"),
            new BigDecimal("70.0000"),
            true,
            attemptMode == AttemptMode.ASSIGNED ? Boolean.TRUE : null,
            attemptMode == AttemptMode.ASSIGNED ? Boolean.TRUE : null,
            "STANDARD",
            "{\"policy\":\"self-history\"}",
            Timestamp.from(completedAt),
            501L,
            organizationalPathSnapshot,
            false,
            Timestamp.from(completedAt.plusSeconds(30))
        );
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

    private record SelfResultFixture(Long userId, Long attemptId, Long resultId, Long assignmentId) {
    }

    private record ActorFixture(Long userId) {
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AssignmentCampaignEntity.class,
        AssignmentEntity.class,
        AssignmentTestEntity.class,
        AppUserEntity.class,
        CourseEntity.class,
        TopicEntity.class,
        TestEntity.class,
        TestAttemptEntity.class,
        ResultEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAssignmentCampaignJpaRepository.class,
        SpringDataAssignmentJpaRepository.class,
        SpringDataAssignmentTestJpaRepository.class,
        SpringDataAppUserJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataTestAttemptJpaRepository.class,
        SpringDataResultJpaRepository.class
    })
    static class ResultSelfHistoryQueryTestApplication {
    }
}
