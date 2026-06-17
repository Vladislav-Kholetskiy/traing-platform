package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code UnsupportedAnalyticsTopicKeyStrategy}.
 * Сценарии описывают ожидаемую работу компонента.
 */
class UnsupportedAnalyticsTopicKeyStrategyBehaviorTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 topic-key strategy must fail closed / skip unsupported rows when no approved immutable topic anchor exists; "
            + "live question.topic_id lookup is not allowed as historical topic-key fallback.";

    private static final String IMPLEMENTATION_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.UnsupportedAnalyticsTopicKeyStrategy";

    private static final List<String> FORBIDDEN_MARKERS = List.of(
        "QuestionEntity",
        "TopicEntity",
        "CourseEntity",
        "questionRepository",
        "topicRepository",
        "contentRepository",
        "question.topic_id",
        "getTopicId",
        "topicIdSnapshot",
        "isActiveFinalForTopic",
        "activeFinal",
        "currentFinal",
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
    void unsupportedTopicKeyStrategyFailsClosedWhenImmutableTopicAnchorIsAbsent() throws Exception {
        Class<?> implementationClass = loadImplementationClassOrFail();

        assertThat(implementationClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.service");
        assertThat(AnalyticsTopicKeyStrategy.class.isAssignableFrom(implementationClass))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(implementationClass.isInterface())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(Modifier.isAbstract(implementationClass.getModifiers()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(implementationClass.isEnum())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(implementationClass.isRecord())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();

        AnalyticsTopicKeyStrategy strategy = instantiateStrategy(implementationClass);
        AnalyticsQuestionAggregateResultSourceRow sourceRow = new AnalyticsQuestionAggregateResultSourceRow(
            10L,
            20L,
            30L,
            "/company/unit",
            "ASSIGNED",
            new BigDecimal("100.0000"),
            true,
            true,
            40L,
            Boolean.TRUE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            Instant.parse("2026-05-01T10:15:30Z")
        );

        AnalyticsTopicKeyResolution resolution = strategy.resolveTopicKey(sourceRow);

        assertThat(resolution.topicId())
            .withFailMessage(CONTRACT_MESSAGE)
            .isNull();
        assertThat(resolution.supported())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(resolution.reason())
            .withFailMessage(CONTRACT_MESSAGE)
            .isNotBlank()
            .containsIgnoringCase("immutable")
            .containsIgnoringCase("topic")
            .containsAnyOf("anchor", "snapshot");

        assertThatThrownBy(() -> strategy.resolveTopicKey(null))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);

        String source = Files.readString(productionSourcePath());
        assertThat(FORBIDDEN_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
    }

    private static AnalyticsTopicKeyStrategy instantiateStrategy(Class<?> implementationClass) {
        try {
            Constructor<?> constructor = implementationClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (AnalyticsTopicKeyStrategy) constructor.newInstance();
        } catch (NoSuchMethodException exception) {
            fail(CONTRACT_MESSAGE, exception);
            throw new IllegalStateException("Unreachable");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }
    }

    private static Class<?> loadImplementationClassOrFail() {
        try {
            return Class.forName(IMPLEMENTATION_CLASS_NAME);
        } catch (ClassNotFoundException exception) {
            fail(CONTRACT_MESSAGE, exception);
            throw new IllegalStateException("Unreachable");
        }
    }

    private static Path productionSourcePath() {
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
            "UnsupportedAnalyticsTopicKeyStrategy.java"
        );
    }
}
