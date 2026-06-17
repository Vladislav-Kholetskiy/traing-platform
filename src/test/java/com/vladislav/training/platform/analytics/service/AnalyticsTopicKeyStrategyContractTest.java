package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AnalyticsTopicKeyStrategy}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AnalyticsTopicKeyStrategyContractTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 topic-key strategy must fail closed / skip unsupported rows when topic_id_snapshot is absent; "
            + "live question.topic_id lookup is not allowed as historical topic-key fallback.";

    private static final String STRATEGY_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsTopicKeyStrategy";
    private static final String RESOLUTION_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsTopicKeyResolution";

    private static final List<String> FORBIDDEN_ANNOTATION_MARKERS = List.of(
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

    private static final List<String> FORBIDDEN_SOURCE_MARKERS = List.of(
        "QuestionEntity",
        "TopicEntity",
        "CourseEntity",
        "questionRepository",
        "topicRepository",
        "contentRepository",
        "topicIdSnapshot",
        "question.topic_id",
        "getTopicId",
        "isActiveFinalForTopic",
        "activeFinal",
        "currentFinal"
    );

    @Test
    void topicKeyStrategyContractMustStayFailClosedWithoutApprovedImmutableTopicAnchor() throws Exception {
        Class<?> strategyClass = loadClassOrFail(STRATEGY_CLASS_NAME);
        Class<?> resolutionClass = loadClassOrFail(RESOLUTION_CLASS_NAME);

        assertThat(strategyClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.service");
        assertThat(strategyClass.isInterface())
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Modifier.isPublic(strategyClass.getModifiers()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        List<Method> publicAbstractMethods = Arrays.stream(strategyClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> Modifier.isAbstract(method.getModifiers()))
            .toList();

        assertThat(publicAbstractMethods)
            .withFailMessage(CONTRACT_MESSAGE)
            .hasSize(1);

        Method resolutionMethod = publicAbstractMethods.get(0);
        assertThat(resolutionMethod.getName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("resolveTopicKey");
        assertThat(resolutionMethod.getReturnType().getName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(RESOLUTION_CLASS_NAME);
        assertThat(resolutionMethod.getParameterTypes())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(AnalyticsQuestionAggregateResultSourceRow.class);

        String strategySource = Files.readString(strategyProductionSourcePath());
        assertThat(FORBIDDEN_ANNOTATION_MARKERS.stream().noneMatch(strategySource::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        assertThat(resolutionClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.service");
        assertThat(resolutionClass.isRecord())
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Modifier.isPublic(resolutionClass.getModifiers()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        RecordComponent[] recordComponents = resolutionClass.getRecordComponents();
        assertThat(Arrays.stream(recordComponents).map(RecordComponent::getName).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly("topicId", "supported", "reason");
        assertThat(Arrays.stream(recordComponents).map(component -> component.getType().getName()).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(Long.class.getName(), boolean.class.getName(), String.class.getName());

        String resolutionSource = Files.readString(resolutionProductionSourcePath());
        assertThat(FORBIDDEN_SOURCE_MARKERS.stream().noneMatch(resolutionSource::contains))
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

    private static Path strategyProductionSourcePath() {
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
            "AnalyticsTopicKeyStrategy.java"
        );
    }

    private static Path resolutionProductionSourcePath() {
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
            "AnalyticsTopicKeyResolution.java"
        );
    }
}
