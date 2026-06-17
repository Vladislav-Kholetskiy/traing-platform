package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
/**
 * Проверяет поведение {@code AnalyticsQuestionAggregateResultSourceReaderImpl}.
 * Сценарии описывают ожидаемую работу компонента.
 */
@JdbcTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class AnalyticsQuestionAggregateResultSourceReaderImplBehaviorTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 question aggregate source reader must read bounded immutable result/result snapshot facts and must not depend on live content, current organization, assignment status, test attempt status or topic fallback.";

    private static final Instant PERIOD_START = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-05-02T00:00:00Z");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-01T10:00:00Z");

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("delete from result_question_snapshot");
        jdbcTemplate.execute("delete from result");
        jdbcTemplate.execute("delete from test_attempt");
        jdbcTemplate.execute("delete from assignment_test");
        jdbcTemplate.execute("delete from assignment");
        jdbcTemplate.execute("delete from assignment_campaign");
        jdbcTemplate.execute("delete from question");
        jdbcTemplate.execute("delete from test");
        jdbcTemplate.execute("delete from topic");
        jdbcTemplate.execute("delete from course");
        jdbcTemplate.execute("delete from app_user");
    }

    @Test
    void readsBoundedImmutableResultQuestionRowsAndExcludesRightBoundary() {
        AnalyticsQuestionAggregateResultSourceReaderImpl reader = createReader();
        AssignedResultFixture fixture = createAssignedResultFixture();

        assertThat(applicationContext.getBeanNamesForType(SchedulerFactoryBean.class))
            
            .isEmpty();

        Long insideResultId = insertResult(
            fixture.firstAttemptId(),
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            fixture.userId(),
            501L,
            "/company/ops",
            "ASSIGNED",
            PERIOD_START.plusSeconds(600)
        );
        insertResultQuestionSnapshot(insideResultId, fixture.questionOriginalId(), true, 0);

        Long boundaryResultId = insertResult(
            fixture.secondAttemptId(),
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            fixture.userId(),
            501L,
            "/company/ops",
            "ASSIGNED",
            PERIOD_END
        );
        insertResultQuestionSnapshot(boundaryResultId, fixture.questionOriginalId(), false, 0);

        assertThatCode(() -> {
            List<AnalyticsQuestionAggregateResultSourceRow> rows = reader.readQuestionAggregateRows(PERIOD_START, PERIOD_END);

            assertThat(rows)
                
                .containsExactly(
                    new AnalyticsQuestionAggregateResultSourceRow(
                        insideResultId,
                        fixture.userId(),
                        501L,
                        "/company/ops",
                        "ASSIGNED",
                        new BigDecimal("100.0000"),
                        true,
                        true,
                        fixture.questionOriginalId(),
                        true,
                        new BigDecimal("1.0000"),
                        new BigDecimal("1.0000"),
                        PERIOD_START.plusSeconds(600)
                    )
                );
        })
            
            .doesNotThrowAnyException();
    }

    private AnalyticsQuestionAggregateResultSourceReaderImpl createReader() {
        try {
            Constructor<AnalyticsQuestionAggregateResultSourceReaderImpl> jdbcTemplateConstructor =
                AnalyticsQuestionAggregateResultSourceReaderImpl.class.getDeclaredConstructor(JdbcTemplate.class);
            jdbcTemplateConstructor.setAccessible(true);
            return jdbcTemplateConstructor.newInstance(jdbcTemplate);
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }

        try {
            Constructor<AnalyticsQuestionAggregateResultSourceReaderImpl> noArgsConstructor =
                AnalyticsQuestionAggregateResultSourceReaderImpl.class.getDeclaredConstructor();
            noArgsConstructor.setAccessible(true);
            return noArgsConstructor.newInstance();
        } catch (NoSuchMethodException exception) {
            fail(
                CONTRACT_MESSAGE
                    + " SCN-11 source reader must be ready for bounded read-only result-facts implementation.",
                exception
            );
            throw new IllegalStateException("Unreachable");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }
    }

    private AssignedResultFixture createAssignedResultFixture() {
        long userId = insertUser("EMP-ANL-READ");
        long courseId = insertCourse("Course");
        long topicId = insertTopic(courseId, "Topic");
        long testId = insertTest(topicId, "Test 1", 0, true);
        long questionId = insertQuestion(topicId, "Question snapshot anchor", 0);
        long campaignId = insertAssignmentCampaign("UNIT-ANL");
        long assignmentId = insertAssignment(campaignId, userId, courseId);
        long assignmentTestId = insertAssignmentTest(assignmentId, testId);
        long firstAttemptId = insertTestAttempt(userId, testId, assignmentTestId, FIXED_INSTANT.plusSeconds(120));
        long secondAttemptId = insertTestAttempt(userId, testId, assignmentTestId, FIXED_INSTANT.plusSeconds(240));

        return new AssignedResultFixture(userId, assignmentId, assignmentTestId, firstAttemptId, secondAttemptId, questionId);
    }

    private Long insertUser(String employeeNumber) {
        return insertAndReturnId(
            """
                insert into app_user (
                    employee_number, external_id, last_name, first_name, middle_name, status, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            employeeNumber,
            null,
            "User",
            employeeNumber,
            null,
            "ACTIVE",
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertCourse(String name) {
        return insertAndReturnId(
            """
                insert into course (
                    name, description, status, sort_order, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?)
                returning id
                """,
            name,
            name,
            "PUBLISHED",
            0,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertTopic(Long courseId, String name) {
        return insertAndReturnId(
            """
                insert into topic (
                    course_id, name, description, status, sort_order, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            courseId,
            name,
            name,
            "PUBLISHED",
            0,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertTest(Long topicId, String name, int sortOrder, boolean activeFinalForTopic) {
        return insertAndReturnId(
            """
                insert into test (
                    topic_id, name, description, test_type, status, threshold_percent, scoring_policy_code,
                    is_active_final_for_topic, sort_order, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            topicId,
            name,
            name,
            "CONTROL",
            "PUBLISHED",
            BigDecimal.valueOf(70.0000),
            "DEFAULT_POLICY",
            activeFinalForTopic,
            sortOrder,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertQuestion(Long topicId, String body, int sortOrder) {
        return insertAndReturnId(
            """
                insert into question (
                    topic_id, body, question_type, status, sort_order, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            topicId,
            body,
            "SINGLE_CHOICE",
            "PUBLISHED",
            sortOrder,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertAssignmentCampaign(String sourceRef) {
        return insertAndReturnId(
            """
                insert into assignment_campaign (
                    name, description, source_type, source_ref, source_name_snapshot, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            "Campaign",
            "Campaign",
            "ORG_UNIT",
            sourceRef,
            "Operations",
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertAssignment(Long campaignId, Long userId, Long courseId) {
        return insertAndReturnId(
            """
                insert into assignment (
                    campaign_id, user_id, course_id, status, assigned_at, deadline_at, cancelled_at, closed_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            campaignId,
            userId,
            courseId,
            "ASSIGNED",
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT.plusSeconds(86_400)),
            null,
            null,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertAssignmentTest(Long assignmentId, Long testId) {
        return insertAndReturnId(
            """
                insert into assignment_test (
                    assignment_id, test_id, assignment_test_role, counted_result_id, closed_at, is_closed, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            assignmentId,
            testId,
            "FINAL_TOPIC_CONTROL",
            null,
            null,
            false,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertTestAttempt(Long userId, Long testId, Long assignmentTestId, Instant completedAt) {
        return insertAndReturnId(
            """
                insert into test_attempt (
                    user_id, test_id, assignment_test_id, attempt_mode, status, started_at, completed_at,
                    expired_at, abandoned_at, last_activity_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            userId,
            testId,
            assignmentTestId,
            "ASSIGNED",
            "COMPLETED",
            timestamp(completedAt.minusSeconds(120)),
            timestamp(completedAt),
            null,
            null,
            timestamp(completedAt.minusSeconds(60)),
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private Long insertResult(
        Long testAttemptId,
        Long assignmentId,
        Long assignmentTestId,
        Long userIdSnapshot,
        Long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot,
        String attemptMode,
        Instant completedAt
    ) {
        return insertAndReturnId(
            """
                insert into result (
                    test_attempt_id, user_id_snapshot, attempt_mode, assignment_id, assignment_test_id,
                    test_id_snapshot, test_name_snapshot,
                    threshold_percent, earned_score, max_score, score_percent, passed, within_deadline,
                    counted_in_assignment, scoring_policy_code, scoring_policy_snapshot, completed_at,
                    organizational_unit_id_snapshot, organizational_path_snapshot, snapshot_final_topic_control_flag, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?, ?, ?, ?, ?)
                returning id
                """,
            testAttemptId,
            userIdSnapshot,
            attemptMode,
            assignmentId,
            assignmentTestId,
            4001L,
            "Analytics Test Snapshot",
            BigDecimal.valueOf(70.0000),
            BigDecimal.valueOf(1.0000),
            BigDecimal.valueOf(1.0000),
            BigDecimal.valueOf(100.0000),
            true,
            true,
            true,
            "DEFAULT_POLICY",
            "{\"policy\":\"v1\"}",
            timestamp(completedAt),
            organizationalUnitIdSnapshot,
            organizationalPathSnapshot,
            true,
            timestamp(completedAt.plusSeconds(1))
        );
    }

    private Long insertResultQuestionSnapshot(Long resultId, Long questionOriginalId, boolean isCorrect, int displayOrder) {
        return insertAndReturnId(
            """
                insert into result_question_snapshot (
                    result_id, question_original_id, body, question_type, display_order, weight,
                    correct_answer_snapshot, user_answer_snapshot, earned_score, max_score, is_correct,
                    evaluation_note, created_at
                ) values (?, ?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), ?, ?, ?, ?, ?)
                returning id
                """,
            resultId,
            questionOriginalId,
            "Question snapshot body",
            "SINGLE_CHOICE",
            displayOrder,
            BigDecimal.valueOf(1.0000),
            "{\"correct\":1}",
            "{\"selected\":1}",
            isCorrect ? BigDecimal.valueOf(1.0000) : BigDecimal.ZERO,
            BigDecimal.valueOf(1.0000),
            isCorrect,
            null,
            timestamp(FIXED_INSTANT.plusSeconds(30))
        );
    }

    private Long insertAndReturnId(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private record AssignedResultFixture(
        Long userId,
        Long assignmentId,
        Long assignmentTestId,
        Long firstAttemptId,
        Long secondAttemptId,
        Long questionOriginalId
    ) {
    }

    static final class SourceReaderBehaviorTestApplication {
    }
}
