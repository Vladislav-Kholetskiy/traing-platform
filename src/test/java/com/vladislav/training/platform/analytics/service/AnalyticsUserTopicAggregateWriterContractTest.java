package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Method;
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
 * Проверяет договорённости вокруг {@code AnalyticsUserTopicAggregateWriter}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AnalyticsUserTopicAggregateWriterContractTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 user-topic aggregate writer contract must stay bounded to analytics materialization; "
            + "user-topic rebuild may use immutable result facts and explicit topic-key strategy only; "
            + "live content, current organization, assignment status, test attempt status and public/scheduler surfaces are not allowed.";

    private static final String WRITER_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsUserTopicAggregateWriter";
    private static final String ROW_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsUserTopicAggregateRow";

    private static final List<String> FORBIDDEN_MARKERS = List.of(
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
    void userTopicAggregateWriterContractMustStayBoundedToAnalyticsMaterializationOnly() throws Exception {
        assertThat(Files.exists(writerProductionSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Files.exists(writerTestSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(Files.exists(rowProductionSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Files.exists(rowTestSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();

        Class<?> writerClass = loadClassOrFail(WRITER_CLASS_NAME);
        Class<?> rowClass = loadClassOrFail(ROW_CLASS_NAME);

        assertThat(writerClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.service");
        assertThat(writerClass.isInterface())
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Modifier.isPublic(writerClass.getModifiers()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        List<Method> publicAbstractMethods = Arrays.stream(writerClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> Modifier.isAbstract(method.getModifiers()))
            .toList();

        assertThat(publicAbstractMethods)
            .withFailMessage(CONTRACT_MESSAGE)
            .hasSize(1);

        Method writerMethod = publicAbstractMethods.get(0);
        assertThat(writerMethod.getName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("replaceUserTopicAggregates");
        assertThat(writerMethod.getReturnType())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(void.class);
        assertThat(writerMethod.getParameterTypes())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(Instant.class, Instant.class, List.class);
        assertThat(writerMethod.getGenericParameterTypes()[2].getTypeName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("java.util.List<" + ROW_CLASS_NAME + ">");

        String writerSource = Files.readString(writerProductionSourcePath());
        assertThat(FORBIDDEN_MARKERS.stream().noneMatch(writerSource::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

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
                "userId",
                "topicId",
                "lastAssignedFinalResultId",
                "lastAssignedFinalCompletedAt",
                "lastAssignedFinalScorePercent",
                "lastAssignedFinalPassed",
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
                Long.class.getName(),
                Long.class.getName(),
                Instant.class.getName(),
                BigDecimal.class.getName(),
                Boolean.class.getName(),
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

    private static Path writerProductionSourcePath() {
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
            "AnalyticsUserTopicAggregateWriter.java"
        );
    }

    private static Path writerTestSourcePath() {
        return Path.of(
            "src",
            "test",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "analytics",
            "service",
            "AnalyticsUserTopicAggregateWriter.java"
        );
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
            "AnalyticsUserTopicAggregateRow.java"
        );
    }

    private static Path rowTestSourcePath() {
        return Path.of(
            "src",
            "test",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "analytics",
            "service",
            "AnalyticsUserTopicAggregateRow.java"
        );
    }
}
