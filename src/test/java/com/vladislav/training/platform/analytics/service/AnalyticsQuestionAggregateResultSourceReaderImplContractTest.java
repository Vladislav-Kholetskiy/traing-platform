package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AnalyticsQuestionAggregateResultSourceReaderImpl}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AnalyticsQuestionAggregateResultSourceReaderImplContractTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 source reader implementation must be explicit and must read immutable result/result snapshot facts only; "
            + "live content, current org, assignment status, test attempt status and topic fallback are not allowed.";

    private static final String IMPLEMENTATION_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsQuestionAggregateResultSourceReaderImpl";

    private static final List<String> FORBIDDEN_ANNOTATION_MARKERS = List.of(
        "@Scheduled",
        "@EnableScheduling",
        "@RestController",
        "@Controller",
        "@RequestMapping",
        "@PostMapping",
        "@GetMapping"
    );

    private static final List<String> FORBIDDEN_LIVE_SOURCE_MARKERS = List.of(
        "QuestionEntity",
        "TopicEntity",
        "CourseEntity",
        "OrganizationalUnitEntity",
        "UserOrganizationAssignmentEntity",
        "UserRoleAssignmentEntity",
        "AssignmentEntity",
        "AssignmentStatus",
        "TestAttemptEntity",
        "TestAttemptStatus",
        "questionRepository",
        "topicRepository",
        "contentRepository",
        "organizationalUnitRepository",
        "userOrganizationAssignmentRepository",
        "assignmentRepository",
        "testAttemptRepository",
        "isActiveFinalForTopic",
        "activeFinal",
        "currentFinal",
        "currentOrg",
        "currentOrganization"
    );

    private static final List<String> FORBIDDEN_TOPIC_FALLBACK_MARKERS = List.of(
        "topicId",
        "topicIdSnapshot",
        "topicOriginalId",
        "topicRepository",
        "courseId",
        "course"
    );

    @Test
    void questionAggregateResultSourceReaderImplementationMustBeConcreteAndStayOffForbiddenRuntimeSurface() throws Exception {
        assertThat(Files.exists(productionSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Files.exists(testSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();

        Class<?> implementationClass = loadImplementationClassOrFail();

        assertThat(implementationClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.service");
        assertThat(AnalyticsQuestionAggregateResultSourceReader.class.isAssignableFrom(implementationClass))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(implementationClass.isInterface())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(java.lang.reflect.Modifier.isAbstract(implementationClass.getModifiers()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(implementationClass.isEnum())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(implementationClass.isRecord())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(implementationClass.getSimpleName())
            .withFailMessage(CONTRACT_MESSAGE)
            .doesNotContain("Test")
            .doesNotContain("Fixture");

        String source = Files.readString(productionSourcePath());

        assertThat(FORBIDDEN_ANNOTATION_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(FORBIDDEN_LIVE_SOURCE_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(FORBIDDEN_TOPIC_FALLBACK_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
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
            "AnalyticsQuestionAggregateResultSourceReaderImpl.java"
        );
    }

    private static Path testSourcePath() {
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
            "AnalyticsQuestionAggregateResultSourceReaderImpl.java"
        );
    }
}
