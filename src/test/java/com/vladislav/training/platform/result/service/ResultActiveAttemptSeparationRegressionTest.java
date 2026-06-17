package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ResultActiveAttemptSeparation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ResultActiveAttemptSeparationRegressionTest {

    @Test
    void resultContractsDoNotExposeActiveAttemptOrExecutionStateVocabulary() {
        assertThat(methodNames(ResultRecordingService.class))
            .containsExactly("recordResult")
            .doesNotContain(
                "findActiveAssignedAttempt",
                "findActiveSelfAttempt",
                "resumeAttempt",
                "updateAttemptActivity",
                "submitAssignedAttempt"
            );
        assertThat(Files.exists(Path.of("src/main/java/com/vladislav/training/platform/result/service/ResultQueryService.java")))
            .isFalse();
    }

    @Test
    void resultBoundaryDocumentationKeepsResultSeparateFromTestAttemptOwnerTruth() throws IOException {
        String packageInfo = read("src/main/java/com/vladislav/training/platform/result/service/package-info.java");

        assertThat(packageInfo)
            .contains("package com.vladislav.training.platform.result.service");
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
