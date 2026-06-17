package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfAnswerMutationCapabilityAdmission} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfAnswerMutationCapabilityAdmissionRegressionTest {

    @Test
    void selfAnswerMutationEntryBoundaryMustExposeExplicitCapabilityAdmissionWiring() {
        assertThat(fieldTypes(SelfAttemptAnswerMutationEntryService.class))
            
            .contains(CapabilityAdmissionPolicy.class, CapabilityAdmissionRequestFactory.class);
    }

    @Test
    void selfAnswerMutationEntryBoundaryMustBuildAndCheckCanonicalCapabilityAdmissionRequest() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAnswerMutationEntryService.java"))
            
            .contains("capabilityAdmissionRequestFactory.createSelfAnswerMutation(")
            .contains("capabilityAdmissionPolicy.check(")
            .doesNotContain("selfAttemptAnswerMutationAdmissionSupport.checkSelfAnswerMutation(");
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
