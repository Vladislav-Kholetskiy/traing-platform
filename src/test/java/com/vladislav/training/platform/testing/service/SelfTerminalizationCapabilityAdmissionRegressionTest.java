package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfTerminalizationCapabilityAdmission} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfTerminalizationCapabilityAdmissionRegressionTest {

    @Test
    void selfSubmitBoundaryMustExposeExplicitCapabilityAdmissionWiring() {
        assertThat(fieldTypes(SelfAttemptSubmitSequencingService.class))
            .contains(
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                SelfAttemptTerminalAdmissionFoundationStateReadService.class
            );
    }

    @Test
    void selfAbandonBoundaryMustExposeExplicitCapabilityAdmissionWiring() {
        assertThat(fieldTypes(SelfAttemptAbandonSequencingService.class))
            .contains(
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                SelfAttemptTerminalAdmissionFoundationStateReadService.class
            );
    }

    @Test
    void selfTerminalizationBoundariesMustBuildAndCheckCanonicalCapabilityAdmissionRequestsBeforeTerminalCore() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingService.java"))
            .contains("foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(")
            .contains("capabilityAdmissionRequestFactory.createSelfAttemptSubmit(")
            .contains("capabilityAdmissionPolicy.check(");

        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonSequencingService.java"))
            .contains("foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(")
            .contains("capabilityAdmissionRequestFactory.createSelfAttemptAbandon(")
            .contains("capabilityAdmissionPolicy.check(");
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
