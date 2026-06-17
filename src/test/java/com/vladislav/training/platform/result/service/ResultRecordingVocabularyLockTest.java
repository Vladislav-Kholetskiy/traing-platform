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
 * Фиксирует словарь и смысловые границы вокруг {@code ResultRecording}.
 * Это помогает не расползтись именам и договорённостям.
 */
class ResultRecordingVocabularyLockTest {

    @Test
    void resultServicePackageKeepsRecordingSeparateFromMutableLifecycleManagement() throws IOException {
        String packageInfo = read("src/main/java/com/vladislav/training/platform/result/service/package-info.java");

        assertThat(packageInfo)
            .contains("package com.vladislav.training.platform.result.service");
    }

    @Test
    void recordingContractDoesNotDriftIntoMutableResultManagementVocabulary() throws IOException {
        String recordingContract = read("src/main/java/com/vladislav/training/platform/result/service/ResultRecordingService.java");

        assertThat(methodNames(ResultRecordingService.class))
            .containsExactly("recordResult")
            .doesNotContain("ensureRecorded", "recalculateResult", "refreshResult", "deleteResult", "updateResult");
        assertThat(recordingContract)
            .contains("interface ResultRecordingService")
            .contains("recordResult");
        assertThat(Files.exists(Path.of("src/main/java/com/vladislav/training/platform/result/service/ResultQueryService.java")))
            .isFalse();
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
