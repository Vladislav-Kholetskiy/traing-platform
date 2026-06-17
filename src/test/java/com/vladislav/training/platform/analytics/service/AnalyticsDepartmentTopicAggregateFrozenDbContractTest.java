package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AnalyticsDepartmentTopicAggregateFrozenDb}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AnalyticsDepartmentTopicAggregateFrozenDbContractTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 department-topic aggregate row contract must match frozen analytics_department_topic_aggregate V100 schema before Step 7 writer semantics can be opened.";

    private static final String ROW_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsDepartmentTopicAggregateRow";

    private static final List<String> FORBIDDEN_MARKERS = List.of(
        "attemptModeSnapshot",
        "totalAnsweredQuestions",
        "correctAnsweredQuestions",
        "QuestionEntity",
        "TopicEntity",
        "CourseEntity",
        "OrganizationalUnitEntity",
        "UserOrganizationAssignmentEntity",
        "AssignmentEntity",
        "AssignmentStatus",
        "TestAttemptEntity",
        "TestAttemptStatus",
        "questionRepository",
        "topicRepository",
        "contentRepository",
        "organizationalUnitRepository",
        "assignmentRepository",
        "testAttemptRepository",
        "question.topic_id",
        "getTopicId",
        "isActiveFinalForTopic",
        "activeFinal",
        "currentFinal",
        "currentOrg",
        "currentOrganization",
        "JdbcTemplate",
        "EntityManager",
        "Repository",
        "@Service",
        "@Component",
        "@Scheduled",
        "@EnableScheduling",
        "@RestController",
        "@Controller",
        "@RequestMapping",
        "@PostMapping",
        "@GetMapping"
    );

    @Test
    void departmentTopicAggregateRowMustMatchFrozenAnalyticsDepartmentTopicAggregateSchema() throws Exception {
        String ddl = Files.readString(
            Path.of("src", "main", "resources", "db", "migration", "V100__full_schema_stack.sql")
        );
        String tableBlock = extractAnalyticsDepartmentTopicAggregateBlock(ddl);

        assertThat(tableBlock)
            .withFailMessage(CONTRACT_MESSAGE)
            .contains("organizational_unit_id_snapshot")
            .contains("organizational_path_snapshot")
            .contains("topic_id")
            .contains("period_start")
            .contains("period_end")
            .contains("average_score_percent")
            .contains("pass_rate_percent")
            .contains("attempt_count")
            .contains("error_count")
            .contains("calculated_at")
            .contains("refreshed_at")
            .contains("reconciled_at")
            .contains("unique (organizational_unit_id_snapshot, topic_id, period_start, period_end)");

        Class<?> rowClass = loadClassOrFail(ROW_CLASS_NAME);

        assertThat(rowClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.service");
        assertThat(rowClass.isRecord())
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Modifier.isPublic(rowClass.getModifiers()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        RecordComponent[] recordComponents = rowClass.getRecordComponents();
        assertThat(Arrays.stream(recordComponents).map(RecordComponent::getName).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(
                "organizationalUnitIdSnapshot",
                "organizationalPathSnapshot",
                "topicId",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "periodStartInclusive",
                "periodEndExclusive"
            );
        assertThat(Arrays.stream(recordComponents).map(component -> component.getType().getName()).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(
                Long.class.getName(),
                String.class.getName(),
                Long.class.getName(),
                BigDecimal.class.getName(),
                BigDecimal.class.getName(),
                long.class.getName(),
                long.class.getName(),
                Instant.class.getName(),
                Instant.class.getName()
            );

        String rowSource = Files.readString(rowProductionSourcePath());
        assertThat(FORBIDDEN_MARKERS.stream().noneMatch(rowSource::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
    }

    private static Class<?> loadClassOrFail(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            fail(CONTRACT_MESSAGE, exception);
            throw new IllegalStateException("Unreachable");
        }
    }

    private static String extractAnalyticsDepartmentTopicAggregateBlock(String ddl) {
        String marker = "create table analytics_department_topic_aggregate";
        int start = ddl.indexOf(marker);
        assertThat(start)
            .withFailMessage(CONTRACT_MESSAGE)
            .isGreaterThanOrEqualTo(0);

        int nextSection = ddl.indexOf("-- 3. analytics_question_aggregate", start);
        assertThat(nextSection)
            .withFailMessage(CONTRACT_MESSAGE)
            .isGreaterThan(start);

        return ddl.substring(start, nextSection);
    }

    private static Path rowProductionSourcePath() {
        return Path.of(
            "src",
            "main",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "analytics",
            "service",
            "AnalyticsDepartmentTopicAggregateRow.java"
        );
    }
}
