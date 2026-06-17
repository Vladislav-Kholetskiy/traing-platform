package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.content.service.PublishedCourseLearningContext;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.Assignment;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AssignmentSelfScopedQueryService}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AssignmentSelfScopedQueryServiceContractTest {

    @Test
    void selfScopedContractExposesSingleActorBoundReadPathForListDetailAndLearningContext() throws NoSuchMethodException {
        assertThat(methodNames(AssignmentSelfScopedQueryService.class))
            .containsExactlyInAnyOrder(
                "findSelfAssignments",
                "findSelfAssignmentById",
                "findAssignedLearningContext",
                "findAssignedMaterialContent",
                "findAssignedTestContext"
            );

        Method listMethod = AssignmentSelfScopedQueryService.class.getDeclaredMethod("findSelfAssignments", Long.class);
        assertThat(listMethod.getParameterTypes()).containsExactly(Long.class);
        assertThat(listMethod.getReturnType()).isEqualTo(java.util.List.class);
        assertThat(((ParameterizedType) listMethod.getGenericReturnType()).getActualTypeArguments())
            .containsExactly(Assignment.class);

        Method detailMethod = AssignmentSelfScopedQueryService.class.getDeclaredMethod(
            "findSelfAssignmentById",
            Long.class,
            Long.class
        );
        assertThat(detailMethod.getParameterTypes()).containsExactly(Long.class, Long.class);
        assertThat(detailMethod.getReturnType()).isEqualTo(Assignment.class);

        Method learningContextMethod = AssignmentSelfScopedQueryService.class.getDeclaredMethod(
            "findAssignedLearningContext",
            Long.class,
            Long.class
        );
        assertThat(learningContextMethod.getParameterTypes()).containsExactly(Long.class, Long.class);
        assertThat(learningContextMethod.getReturnType()).isEqualTo(AssignedLearningContext.class);

        Method assignedTestContextMethod = AssignmentSelfScopedQueryService.class.getDeclaredMethod(
            "findAssignedTestContext",
            Long.class,
            Long.class,
            Long.class
        );
        assertThat(assignedTestContextMethod.getParameterTypes()).containsExactly(Long.class, Long.class, Long.class);
        assertThat(assignedTestContextMethod.getReturnType()).isEqualTo(AssignedTestContext.class);

        Method assignedMaterialContentMethod = AssignmentSelfScopedQueryService.class.getDeclaredMethod(
            "findAssignedMaterialContent",
            Long.class,
            Long.class,
            Long.class
        );
        assertThat(assignedMaterialContentMethod.getParameterTypes()).containsExactly(Long.class, Long.class, Long.class);
        assertThat(assignedMaterialContentMethod.getReturnType()).isEqualTo(AssignedMaterialContent.class);
    }

    @Test
    void selfScopedContractDoesNotExposeArbitrarySubjectSelectorShape() throws IOException {
        String source = read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentSelfScopedQueryService.java");

        assertThat(source)
            .contains("trusted upstream actor identity")
            .contains("findSelfAssignments(Long actorUserId)")
            .contains("findSelfAssignmentById(Long actorUserId, Long assignmentId)")
            .contains("findAssignedLearningContext(Long actorUserId, Long assignmentId)")
            .contains("findAssignedMaterialContent(Long actorUserId, Long assignmentId, Long materialId)")
            .contains("findAssignedTestContext(Long actorUserId, Long assignmentId, Long assignmentTestId)")
            .doesNotContain("subjectUserId")
            .doesNotContain("targetUserId")
            .doesNotContain("assigneeUserId")
            .doesNotContain("findAssignmentsByUserId")
            .doesNotContain("findAssignmentsForUser")
            .doesNotContain("findUserAssignments")
            .doesNotContain("findAssignmentByUserId");
    }

    @Test
    void selfScopedContractDoesNotMixLaunchAdministrativeOrStatusVocabulary() throws IOException {
        assertThat(methodNames(AssignmentSelfScopedQueryService.class))
            .doesNotContain(
                "launchAssignmentCampaign",
                "cancelAssignment",
                "extendAssignmentDeadline",
                "replaceWithNewAssignment",
                "recalculateAssignmentStatus",
                "refreshAssignmentStatusCache",
                "findSelfVisibleTestById"
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentSelfScopedQueryService.java"))
            .contains("AssignmentSelfScopedQueryService")
            .contains("findSelfAssignments")
            .doesNotContain("campaign CRUD")
            .doesNotContain("manual status patch")
            .doesNotContain("audit trail");

        assertThat(fieldTypeNames(AssignedLearningContext.class))
            .containsExactlyInAnyOrder(
                Assignment.class.getName(),
                List.class.getName(),
                PublishedCourseLearningContext.class.getName()
            );
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private Set<String> fieldTypeNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(field -> field.getType().getName())
            .collect(Collectors.toUnmodifiableSet());
    }
}
