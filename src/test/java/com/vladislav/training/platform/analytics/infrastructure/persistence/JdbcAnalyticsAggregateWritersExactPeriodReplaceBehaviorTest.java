package com.vladislav.training.platform.analytics.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.vladislav.training.platform.analytics.service.AnalyticsDepartmentTopicAggregateRow;
import com.vladislav.training.platform.analytics.service.AnalyticsQuestionAggregateRow;
import com.vladislav.training.platform.analytics.service.AnalyticsQuestionAggregateWriter;
import com.vladislav.training.platform.analytics.service.AnalyticsDepartmentTopicAggregateWriter;
import com.vladislav.training.platform.analytics.service.AnalyticsUserTopicAggregateRow;
import com.vladislav.training.platform.analytics.service.AnalyticsUserTopicAggregateWriter;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
/**
 * Проверяет поведение {@code JdbcAnalyticsAggregateWritersExactPeriodReplace}.
 * Сценарии описывают ожидаемую работу компонента.
 */
@JdbcTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ContextConfiguration(classes = JdbcAnalyticsAggregateWritersExactPeriodReplaceBehaviorTest.TestApplication.class)
class JdbcAnalyticsAggregateWritersExactPeriodReplaceBehaviorTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 JDBC aggregate writers must replace only the exact target period in frozen analytics aggregate tables, "
            + "must preserve neighboring periods, must clear only scoped stale rows on empty input, and must fail fast on "
            + "null arguments or row period mismatch without opening scheduler or public API runtime.";

    private static final String USER_WRITER_CLASS_NAME =
        "com.vladislav.training.platform.analytics.infrastructure.persistence.JdbcAnalyticsUserTopicAggregateWriter";
    private static final String DEPARTMENT_WRITER_CLASS_NAME =
        "com.vladislav.training.platform.analytics.infrastructure.persistence.JdbcAnalyticsDepartmentTopicAggregateWriter";
    private static final String QUESTION_WRITER_CLASS_NAME =
        "com.vladislav.training.platform.analytics.infrastructure.persistence.JdbcAnalyticsQuestionAggregateWriter";

    private static final Instant PERIOD_START = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-05-02T00:00:00Z");
    private static final Instant NEIGHBOR_START = Instant.parse("2026-05-02T00:00:00Z");
    private static final Instant NEIGHBOR_END = Instant.parse("2026-05-03T00:00:00Z");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-01T12:00:00Z");

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
        jdbcTemplate.execute("delete from analytics_question_aggregate");
        jdbcTemplate.execute("delete from analytics_department_topic_aggregate");
        jdbcTemplate.execute("delete from analytics_user_topic_aggregate");
        jdbcTemplate.execute("delete from result");
        jdbcTemplate.execute("delete from test_attempt");
        jdbcTemplate.execute("delete from assignment_test");
        jdbcTemplate.execute("delete from assignment");
        jdbcTemplate.execute("delete from assignment_campaign");
        jdbcTemplate.execute("delete from test");
        jdbcTemplate.execute("delete from question");
        jdbcTemplate.execute("delete from topic");
        jdbcTemplate.execute("delete from course");
        jdbcTemplate.execute("delete from app_user");
    }

    @Test
    void jdbcWriterBehaviorSliceDoesNotStartQuartzSchedulerRuntime() {
        assertThat(applicationContext.getBeanNamesForType(SchedulerFactoryBean.class))
            
            .isEmpty();
    }

    @Test
    void userTopicWriterMustFailFastOnNullArgsAndRowPeriodMismatch() {
        AnalyticsUserTopicAggregateWriter writer = createUserWriter();
        AnalyticsUserTopicAggregateRow mismatchedRow = new AnalyticsUserTopicAggregateRow(
            101L,
            501L,
            null,
            null,
            null,
            null,
            new BigDecimal("85.5000"),
            new BigDecimal("75.0000"),
            12,
            3,
            NEIGHBOR_START,
            NEIGHBOR_END
        );

        assertThatThrownBy(() -> writer.replaceUserTopicAggregates(null, PERIOD_END, List.of()))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceUserTopicAggregates(PERIOD_START, null, List.of()))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceUserTopicAggregates(PERIOD_START, PERIOD_END, null))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceUserTopicAggregates(PERIOD_START, PERIOD_END, List.of(mismatchedRow)))
            
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userTopicWriterMustReplaceOnlyExactPeriodAndClearOnlyTargetPeriodOnEmptyInput() {
        AnalyticsUserTopicAggregateWriter writer = createUserWriter();
        long userId = insertUser("EMP-UT");
        long courseId = insertCourse("Course UT");
        long targetTopicId = insertTopic(courseId, "Target Topic UT", 0);
        long neighborTopicId = insertTopic(courseId, "Neighbor Topic UT", 1);
        long assignmentCampaignId = insertAssignmentCampaign("UNIT-UT");
        long assignmentId = insertAssignment(assignmentCampaignId, userId, courseId);
        long targetTestId = insertTest(targetTopicId, "Target Final Test");
        long assignmentTestId = insertAssignmentTest(assignmentId, targetTestId);
        long targetAttemptId = insertTestAttempt(
            userId,
            targetTestId,
            assignmentTestId,
            "ASSIGNED",
            FIXED_INSTANT.plusSeconds(60)
        );
        long lastAssignedFinalResultId = insertResult(
            targetAttemptId,
            userId,
            targetTestId,
            assignmentId,
            assignmentTestId,
            "ASSIGNED",
            FIXED_INSTANT.plusSeconds(120)
        );

        insertUserTopicAggregateRow(userId, targetTopicId, PERIOD_START, PERIOD_END, "10.0000", "20.0000", 1, 1);
        insertUserTopicAggregateRow(userId, neighborTopicId, NEIGHBOR_START, NEIGHBOR_END, "30.0000", "40.0000", 2, 0);

        AnalyticsUserTopicAggregateRow replacementRow = new AnalyticsUserTopicAggregateRow(
            userId,
            targetTopicId,
            lastAssignedFinalResultId,
            FIXED_INSTANT.plusSeconds(120),
            new BigDecimal("88.5000"),
            true,
            new BigDecimal("81.2500"),
            new BigDecimal("66.6700"),
            12,
            4,
            PERIOD_START,
            PERIOD_END
        );

        writer.replaceUserTopicAggregates(PERIOD_START, PERIOD_END, List.of(replacementRow));

        assertThat(selectUserTopicRows())
            
            .hasSize(2);
        assertThat(selectUserTopicRowsForPeriod(PERIOD_START, PERIOD_END))
            
            .containsExactly(Map.ofEntries(
                Map.entry("user_id", userId),
                Map.entry("topic_id", targetTopicId),
                Map.entry("period_start", Timestamp.from(PERIOD_START)),
                Map.entry("period_end", Timestamp.from(PERIOD_END)),
                Map.entry("last_assigned_final_result_id", lastAssignedFinalResultId),
                Map.entry("last_assigned_final_completed_at", Timestamp.from(FIXED_INSTANT.plusSeconds(120))),
                Map.entry("last_assigned_final_score_percent", new BigDecimal("88.5000")),
                Map.entry("last_assigned_final_passed", true),
                Map.entry("average_score_percent", new BigDecimal("81.2500")),
                Map.entry("pass_rate_percent", new BigDecimal("66.6700")),
                Map.entry("attempt_count", 12),
                Map.entry("error_count", 4)
            ));
        assertThat(selectUserTopicRowsForPeriod(NEIGHBOR_START, NEIGHBOR_END))
            
            .hasSize(1);

        writer.replaceUserTopicAggregates(PERIOD_START, PERIOD_END, List.of());

        assertThat(selectUserTopicRowsForPeriod(PERIOD_START, PERIOD_END))
            
            .isEmpty();
        assertThat(selectUserTopicRowsForPeriod(NEIGHBOR_START, NEIGHBOR_END))
            
            .hasSize(1);
    }

    @Test
    void departmentTopicWriterMustFailFastOnNullArgsAndRowPeriodMismatch() {
        AnalyticsDepartmentTopicAggregateWriter writer = createDepartmentWriter();
        AnalyticsDepartmentTopicAggregateRow mismatchedRow = new AnalyticsDepartmentTopicAggregateRow(
            301L,
            "/company/division/team-b",
            501L,
            new BigDecimal("91.5000"),
            new BigDecimal("88.0000"),
            7,
            1,
            NEIGHBOR_START,
            NEIGHBOR_END
        );

        assertThatThrownBy(() -> writer.replaceDepartmentTopicAggregates(null, PERIOD_END, List.of()))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceDepartmentTopicAggregates(PERIOD_START, null, List.of()))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceDepartmentTopicAggregates(PERIOD_START, PERIOD_END, null))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceDepartmentTopicAggregates(PERIOD_START, PERIOD_END, List.of(mismatchedRow)))
            
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void departmentTopicWriterMustReplaceOnlyExactPeriodAndClearOnlyTargetPeriodOnEmptyInput() {
        AnalyticsDepartmentTopicAggregateWriter writer = createDepartmentWriter();
        long courseId = insertCourse("Course DT");
        long targetTopicId = insertTopic(courseId, "Target Topic DT", 0);
        long neighborTopicId = insertTopic(courseId, "Neighbor Topic DT", 1);

        insertDepartmentTopicAggregateRow(301L, "/company/division/team-a", targetTopicId, PERIOD_START, PERIOD_END, "50.0000", "60.0000", 2, 1);
        insertDepartmentTopicAggregateRow(302L, "/company/division/team-b", neighborTopicId, NEIGHBOR_START, NEIGHBOR_END, "70.0000", "80.0000", 3, 0);

        AnalyticsDepartmentTopicAggregateRow replacementRow = new AnalyticsDepartmentTopicAggregateRow(
            301L,
            "/company/division/team-a",
            targetTopicId,
            new BigDecimal("77.2500"),
            new BigDecimal("61.5000"),
            25,
            4,
            PERIOD_START,
            PERIOD_END
        );

        writer.replaceDepartmentTopicAggregates(PERIOD_START, PERIOD_END, List.of(replacementRow));

        assertThat(selectDepartmentTopicRows())
            
            .hasSize(2);
        assertThat(selectDepartmentTopicRowsForPeriod(PERIOD_START, PERIOD_END))
            
            .containsExactly(Map.of(
                "organizational_unit_id_snapshot", 301L,
                "organizational_path_snapshot", "/company/division/team-a",
                "topic_id", targetTopicId,
                "period_start", Timestamp.from(PERIOD_START),
                "period_end", Timestamp.from(PERIOD_END),
                "average_score_percent", new BigDecimal("77.2500"),
                "pass_rate_percent", new BigDecimal("61.5000"),
                "attempt_count", 25,
                "error_count", 4
            ));
        assertThat(selectDepartmentTopicRowsForPeriod(NEIGHBOR_START, NEIGHBOR_END))
            
            .hasSize(1);

        writer.replaceDepartmentTopicAggregates(PERIOD_START, PERIOD_END, List.of());

        assertThat(selectDepartmentTopicRowsForPeriod(PERIOD_START, PERIOD_END))
            
            .isEmpty();
        assertThat(selectDepartmentTopicRowsForPeriod(NEIGHBOR_START, NEIGHBOR_END))
            
            .hasSize(1);
    }

    @Test
    void questionWriterMustFailFastOnNullArgsAndRowPeriodMismatch() {
        AnalyticsQuestionAggregateWriter writer = createQuestionWriter();
        AnalyticsQuestionAggregateRow mismatchedRow = new AnalyticsQuestionAggregateRow(
            701L,
            10,
            6,
            4,
            new BigDecimal("4.2500"),
            NEIGHBOR_START,
            NEIGHBOR_END
        );

        assertThatThrownBy(() -> writer.replaceQuestionAggregates(null, PERIOD_END, List.of()))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceQuestionAggregates(PERIOD_START, null, List.of()))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceQuestionAggregates(PERIOD_START, PERIOD_END, null))
            
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.replaceQuestionAggregates(PERIOD_START, PERIOD_END, List.of(mismatchedRow)))
            
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void questionWriterMustReplaceOnlyExactPeriodAndClearOnlyTargetPeriodOnEmptyInput() {
        AnalyticsQuestionAggregateWriter writer = createQuestionWriter();
        long courseId = insertCourse("Course Q");
        long topicId = insertTopic(courseId, "Topic Q", 0);
        long targetQuestionId = insertQuestion(topicId, "Target Question", 0);
        long neighborQuestionId = insertQuestion(topicId, "Neighbor Question", 1);

        insertQuestionAggregateRow(targetQuestionId, PERIOD_START, PERIOD_END, 1, 1, 0, "1.0000");
        insertQuestionAggregateRow(neighborQuestionId, NEIGHBOR_START, NEIGHBOR_END, 2, 1, 1, "2.0000");

        AnalyticsQuestionAggregateRow replacementRow = new AnalyticsQuestionAggregateRow(
            targetQuestionId,
            15,
            8,
            3,
            new BigDecimal("4.7500"),
            PERIOD_START,
            PERIOD_END
        );

        writer.replaceQuestionAggregates(PERIOD_START, PERIOD_END, List.of(replacementRow));

        assertThat(selectQuestionRows())
            
            .hasSize(2);
        assertThat(selectQuestionRowsForPeriod(PERIOD_START, PERIOD_END))
            
            .containsExactly(Map.of(
                "question_id", targetQuestionId,
                "period_start", Timestamp.from(PERIOD_START),
                "period_end", Timestamp.from(PERIOD_END),
                "attempt_count", 15,
                "correct_count", 8,
                "incorrect_count", 3,
                "average_earned_score", new BigDecimal("4.7500")
            ));
        assertThat(selectQuestionRowsForPeriod(NEIGHBOR_START, NEIGHBOR_END))
            
            .hasSize(1);

        writer.replaceQuestionAggregates(PERIOD_START, PERIOD_END, List.of());

        assertThat(selectQuestionRowsForPeriod(PERIOD_START, PERIOD_END))
            
            .isEmpty();
        assertThat(selectQuestionRowsForPeriod(NEIGHBOR_START, NEIGHBOR_END))
            
            .hasSize(1);
    }

    private AnalyticsUserTopicAggregateWriter createUserWriter() {
        return (AnalyticsUserTopicAggregateWriter) instantiateWriter(USER_WRITER_CLASS_NAME);
    }

    private AnalyticsDepartmentTopicAggregateWriter createDepartmentWriter() {
        return (AnalyticsDepartmentTopicAggregateWriter) instantiateWriter(DEPARTMENT_WRITER_CLASS_NAME);
    }

    private AnalyticsQuestionAggregateWriter createQuestionWriter() {
        return (AnalyticsQuestionAggregateWriter) instantiateWriter(QUESTION_WRITER_CLASS_NAME);
    }

    private Object instantiateWriter(String className) {
        try {
            Class<?> writerClass = Class.forName(className);
            Constructor<?> constructor = writerClass.getDeclaredConstructor(JdbcTemplate.class);
            constructor.setAccessible(true);
            return constructor.newInstance(jdbcTemplate);
        } catch (ClassNotFoundException exception) {
            fail(CONTRACT_MESSAGE + " Missing writer class: " + className, exception);
            throw new IllegalStateException("Unreachable");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }
    }

    private long insertUser(String employeeNumber) {
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

    private long insertCourse(String name) {
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

    private long insertTopic(long courseId, String name, int sortOrder) {
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
            sortOrder,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private long insertQuestion(long topicId, String body, int sortOrder) {
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

    private long insertTest(long topicId, String name) {
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
            new BigDecimal("70.0000"),
            "DEFAULT_POLICY",
            true,
            0,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private long insertAssignmentCampaign(String sourceRef) {
        return insertAndReturnId(
            """
                insert into assignment_campaign (
                    name, description, source_type, source_ref, source_name_snapshot, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                returning id
                """,
            "Campaign UT",
            "Campaign UT",
            "ORG_UNIT",
            sourceRef,
            "Operations",
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private long insertAssignment(long campaignId, long userId, long courseId) {
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

    private long insertAssignmentTest(long assignmentId, long testId) {
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

    private long insertTestAttempt(
        long userId,
        long testId,
        Long assignmentTestId,
        String attemptMode,
        Instant completedAt
    ) {
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
            attemptMode,
            "COMPLETED",
            timestamp(completedAt.minusSeconds(120)),
            timestamp(completedAt),
            null,
            null,
            timestamp(completedAt.minusSeconds(30)),
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT)
        );
    }

    private long insertResult(
        long testAttemptId,
        long userIdSnapshot,
        long testIdSnapshot,
        Long assignmentId,
        Long assignmentTestId,
        String attemptMode,
        Instant completedAt
    ) {
        return insertAndReturnId(
            """
                insert into result (
                    test_attempt_id, user_id_snapshot, attempt_mode, assignment_id, assignment_test_id,
                    test_id_snapshot, test_name_snapshot, threshold_percent, earned_score, max_score, score_percent,
                    passed, within_deadline, counted_in_assignment, scoring_policy_code, scoring_policy_snapshot,
                    completed_at, organizational_unit_id_snapshot, organizational_path_snapshot,
                    snapshot_final_topic_control_flag, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?, ?, ?, ?, ?)
                returning id
                """,
            testAttemptId,
            userIdSnapshot,
            attemptMode,
            assignmentId,
            assignmentTestId,
            testIdSnapshot,
            "Target Final Test",
            new BigDecimal("70.0000"),
            new BigDecimal("8.8500"),
            new BigDecimal("10.0000"),
            new BigDecimal("88.5000"),
            true,
            "ASSIGNED".equals(attemptMode) ? true : null,
            "ASSIGNED".equals(attemptMode) ? true : null,
            "DEFAULT_POLICY",
            "{\"policy\":\"v1\"}",
            timestamp(completedAt),
            301L,
            "/company/division/team-a",
            true,
            timestamp(completedAt.plusSeconds(5))
        );
    }

    private void insertUserTopicAggregateRow(
        long userId,
        long topicId,
        Instant periodStart,
        Instant periodEnd,
        String averageScorePercent,
        String passRatePercent,
        int attemptCount,
        int errorCount
    ) {
        jdbcTemplate.update(
            """
                insert into analytics_user_topic_aggregate (
                    user_id, topic_id, period_start, period_end, last_assigned_final_result_id,
                    last_assigned_final_completed_at, last_assigned_final_score_percent, last_assigned_final_passed,
                    average_score_percent, pass_rate_percent, attempt_count, error_count,
                    calculated_at, refreshed_at, reconciled_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            userId,
            topicId,
            timestamp(periodStart),
            timestamp(periodEnd),
            null,
            null,
            null,
            null,
            new BigDecimal(averageScorePercent),
            new BigDecimal(passRatePercent),
            attemptCount,
            errorCount,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT.plusSeconds(10)),
            null
        );
    }

    private void insertDepartmentTopicAggregateRow(
        long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot,
        long topicId,
        Instant periodStart,
        Instant periodEnd,
        String averageScorePercent,
        String passRatePercent,
        int attemptCount,
        int errorCount
    ) {
        jdbcTemplate.update(
            """
                insert into analytics_department_topic_aggregate (
                    organizational_unit_id_snapshot, organizational_path_snapshot, topic_id,
                    period_start, period_end, average_score_percent, pass_rate_percent,
                    attempt_count, error_count, calculated_at, refreshed_at, reconciled_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            organizationalUnitIdSnapshot,
            organizationalPathSnapshot,
            topicId,
            timestamp(periodStart),
            timestamp(periodEnd),
            new BigDecimal(averageScorePercent),
            new BigDecimal(passRatePercent),
            attemptCount,
            errorCount,
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT.plusSeconds(10)),
            null
        );
    }

    private void insertQuestionAggregateRow(
        long questionId,
        Instant periodStart,
        Instant periodEnd,
        int attemptCount,
        int correctCount,
        int incorrectCount,
        String averageEarnedScore
    ) {
        jdbcTemplate.update(
            """
                insert into analytics_question_aggregate (
                    question_id, period_start, period_end, attempt_count, correct_count,
                    incorrect_count, average_earned_score, calculated_at, refreshed_at, reconciled_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            questionId,
            timestamp(periodStart),
            timestamp(periodEnd),
            attemptCount,
            correctCount,
            incorrectCount,
            new BigDecimal(averageEarnedScore),
            timestamp(FIXED_INSTANT),
            timestamp(FIXED_INSTANT.plusSeconds(10)),
            null
        );
    }

    private List<Map<String, Object>> selectUserTopicRows() {
        return jdbcTemplate.queryForList(
            """
                select user_id, topic_id, period_start, period_end, average_score_percent,
                       last_assigned_final_result_id, last_assigned_final_completed_at,
                       last_assigned_final_score_percent, last_assigned_final_passed,
                       average_score_percent, pass_rate_percent, attempt_count, error_count
                from analytics_user_topic_aggregate
                order by period_start, user_id, topic_id
                """
        );
    }

    private List<Map<String, Object>> selectUserTopicRowsForPeriod(Instant periodStart, Instant periodEnd) {
        return jdbcTemplate.queryForList(
            """
                select user_id, topic_id, period_start, period_end, average_score_percent,
                       last_assigned_final_result_id, last_assigned_final_completed_at,
                       last_assigned_final_score_percent, last_assigned_final_passed,
                       average_score_percent, pass_rate_percent, attempt_count, error_count
                from analytics_user_topic_aggregate
                where period_start = ? and period_end = ?
                order by user_id, topic_id
                """,
            timestamp(periodStart),
            timestamp(periodEnd)
        );
    }

    private List<Map<String, Object>> selectDepartmentTopicRows() {
        return jdbcTemplate.queryForList(
            """
                select organizational_unit_id_snapshot, organizational_path_snapshot, topic_id, period_start, period_end,
                       average_score_percent, pass_rate_percent, attempt_count, error_count
                from analytics_department_topic_aggregate
                order by period_start, organizational_unit_id_snapshot, topic_id
                """
        );
    }

    private List<Map<String, Object>> selectDepartmentTopicRowsForPeriod(Instant periodStart, Instant periodEnd) {
        return jdbcTemplate.queryForList(
            """
                select organizational_unit_id_snapshot, organizational_path_snapshot, topic_id, period_start, period_end,
                       average_score_percent, pass_rate_percent, attempt_count, error_count
                from analytics_department_topic_aggregate
                where period_start = ? and period_end = ?
                order by organizational_unit_id_snapshot, topic_id
                """,
            timestamp(periodStart),
            timestamp(periodEnd)
        );
    }

    private List<Map<String, Object>> selectQuestionRows() {
        return jdbcTemplate.queryForList(
            """
                select question_id, period_start, period_end, attempt_count, correct_count,
                       incorrect_count, average_earned_score
                from analytics_question_aggregate
                order by period_start, question_id
                """
        );
    }

    private List<Map<String, Object>> selectQuestionRowsForPeriod(Instant periodStart, Instant periodEnd) {
        return jdbcTemplate.queryForList(
            """
                select question_id, period_start, period_end, attempt_count, correct_count,
                       incorrect_count, average_earned_score
                from analytics_question_aggregate
                where period_start = ? and period_end = ?
                order by question_id
                """,
            timestamp(periodStart),
            timestamp(periodEnd)
        );
    }

    private long insertAndReturnId(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
