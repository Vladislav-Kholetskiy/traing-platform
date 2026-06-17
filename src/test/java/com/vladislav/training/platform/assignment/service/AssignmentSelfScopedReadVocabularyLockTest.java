package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.controller.AssignmentSelfScopedReadController;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
/**
 * Фиксирует словарь и смысловые границы вокруг {@code AssignmentSelfScopedRead}.
 * Это помогает не расползтись именам и договорённостям.
 */
class AssignmentSelfScopedReadVocabularyLockTest {

    @Test
    void selfScopedReadVocabularyStaysReadOnlyRatherThanExecutionOrStartFlow() throws IOException {
        String servicePackageInfo = read("src/main/java/com/vladislav/training/platform/assignment/service/package-info.java");
        String controllerPackageInfo = read("src/main/java/com/vladislav/training/platform/assignment/controller/package-info.java");
        String serviceContract = read(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentSelfScopedQueryService.java"
        );
        String controller = read(
            "src/main/java/com/vladislav/training/platform/assignment/controller/AssignmentSelfScopedReadController.java"
        );

        assertThat(servicePackageInfo)
            .contains("SCN-20")
            .contains("pure query/read entitlement contour")
            .doesNotContain("future self-scoped assignment reads");
        assertThat(controllerPackageInfo)
            .contains("read-only entitlement delivery")
            .contains("not an execution entry")
            .contains("not a hidden pre-start command seam")
            .contains("not a generic testing/result facade");
        assertThat(serviceContract)
            .contains("self-scoped assignment read contour")
            .contains("does not authorize generic assignment read API semantics")
            .doesNotContain("start-flow")
            .doesNotContain("execution entry")
            .doesNotContain("test_attempt")
            .doesNotContain("findSelfVisibleTestById");
        assertThat(controller)
            .contains("Dedicated self-scoped REST adapter")
            .doesNotContain("startAttempt")
            .doesNotContain("resumeAttempt")
            .doesNotContain("submitAttempt")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("SelfVisibleTestingReadController");
    }

    @Test
    void selfScopedControllerSurfaceKeepsReadOnlyMethodVocabulary() {
        assertThat(getGetMappingMethodNames(AssignmentSelfScopedReadController.class))
            .containsExactlyInAnyOrder(
                "findSelfAssignments",
                "findSelfAssignmentById",
                "findAssignedLearningContext",
                "findAssignedMaterialContent",
                "findAssignedTestContext"
            )
            .doesNotContain(
                "startAttempt",
                "resumeAttempt",
                "launchAssignment",
                "executeAssignment",
                "recordResult"
            );
    }

    private Set<String> getGetMappingMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
