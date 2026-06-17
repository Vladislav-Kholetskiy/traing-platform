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
 * Проверяет договорённости вокруг {@code AnalyticsQuestionAggregateResultSourceReader}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AnalyticsQuestionAggregateResultSourceReaderContractTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 question aggregate source reader must read only result/result snapshot facts; "
            + "question_original_id is allowed for question aggregate; "
            + "topic aggregate materialization must use approved immutable topic snapshot anchor when present; "
            + "live content lookup must not be used as topic-key fallback.";

    private static final String SOURCE_READER_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsQuestionAggregateResultSourceReader";
    private static final String SOURCE_ROW_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsQuestionAggregateResultSourceRow";

    private static final List<String> FORBIDDEN_PACKAGE_SEGMENTS = List.of(
        ".content.",
        ".assignment.",
        ".testing.",
        ".userorg.",
        ".result."
    );

    private static final List<String> FORBIDDEN_TOPIC_NAMES = List.of(
        "topicId",
        "topicOriginalId",
        "topic",
        "courseId",
        "course"
    );

    @Test
    void questionAggregateResultSourceReaderContractIsReservedForSnapshotOnlyScn11Facts() throws Exception {
        assertThat(Files.exists(sourceReaderProductionSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Files.exists(sourceReaderTestSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(Files.exists(sourceRowProductionSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Files.exists(sourceRowTestSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();

        Class<?> sourceReaderClass = loadClassOrFail(SOURCE_READER_CLASS_NAME);
        Class<?> sourceRowClass = loadClassOrFail(SOURCE_ROW_CLASS_NAME);

        assertThat(sourceReaderClass.isInterface())
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(sourceReaderClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.service");
        assertThat(FORBIDDEN_PACKAGE_SEGMENTS.stream().noneMatch(sourceReaderClass.getName()::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        List<Method> publicAbstractMethods = Arrays.stream(sourceReaderClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> Modifier.isAbstract(method.getModifiers()))
            .toList();

        assertThat(publicAbstractMethods)
            .withFailMessage(CONTRACT_MESSAGE)
            .hasSize(1);

        Method sourceMethod = publicAbstractMethods.get(0);
        assertThat(sourceMethod.getName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("readQuestionAggregateRows");
        assertThat(sourceMethod.getReturnType())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(List.class);
        assertThat(sourceMethod.getGenericReturnType().getTypeName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("java.util.List<" + SOURCE_ROW_CLASS_NAME + ">");
        assertThat(sourceMethod.getParameterTypes())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(Instant.class, Instant.class);
        assertThat(Arrays.stream(sourceMethod.getParameters()).map(parameter -> parameter.getName()).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly("periodStartInclusive", "periodEndExclusive");

        assertThat(sourceRowClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.service");
        assertThat(sourceRowClass.isRecord())
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        RecordComponent[] recordComponents = sourceRowClass.getRecordComponents();
        assertThat(Arrays.stream(recordComponents).map(RecordComponent::getName).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(
                "resultId",
                "userIdSnapshot",
                "organizationalUnitIdSnapshot",
                "organizationalPathSnapshot",
                "attemptModeSnapshot",
                "scorePercent",
                "passed",
                "finalTopicControlSnapshot",
                "questionOriginalId",
                "topicIdSnapshot",
                "answeredCorrectly",
                "earnedScore",
                "maxScore",
                "completedAt"
            );
        assertThat(Arrays.stream(recordComponents).map(component -> component.getType().getName()).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(
                Long.class.getName(),
                Long.class.getName(),
                Long.class.getName(),
                String.class.getName(),
                String.class.getName(),
                BigDecimal.class.getName(),
                Boolean.class.getName(),
                Boolean.class.getName(),
                Long.class.getName(),
                Long.class.getName(),
                Boolean.class.getName(),
                BigDecimal.class.getName(),
                BigDecimal.class.getName(),
                Instant.class.getName()
            );

        assertThat(Arrays.stream(recordComponents).map(RecordComponent::getName).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .doesNotContainAnyElementsOf(FORBIDDEN_TOPIC_NAMES);
        assertThat(Arrays.stream(sourceRowClass.getDeclaredFields()).map(field -> field.getName()).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .doesNotContainAnyElementsOf(FORBIDDEN_TOPIC_NAMES);
        assertThat(Arrays.stream(sourceRowClass.getDeclaredMethods()).map(Method::getName).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .doesNotContainAnyElementsOf(FORBIDDEN_TOPIC_NAMES);
    }

    private static Class<?> loadClassOrFail(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            fail(CONTRACT_MESSAGE, exception);
            throw new IllegalStateException("Unreachable");
        }
    }

    private static Path sourceReaderProductionSourcePath() {
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
            "AnalyticsQuestionAggregateResultSourceReader.java"
        );
    }

    private static Path sourceReaderTestSourcePath() {
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
            "AnalyticsQuestionAggregateResultSourceReader.java"
        );
    }

    private static Path sourceRowProductionSourcePath() {
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
            "AnalyticsQuestionAggregateResultSourceRow.java"
        );
    }

    private static Path sourceRowTestSourcePath() {
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
            "AnalyticsQuestionAggregateResultSourceRow.java"
        );
    }
}
