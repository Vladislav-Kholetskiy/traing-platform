package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.testing.admission.SelfExecutionAdmissionFoundationStateReadService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Не даёт {@code SelfVisibleReadStartMaterializationBoundary} незаметно уйти от текущего замысла.
 * Тест страхует важные ограничения и исходную идею решения.
 */
class SelfVisibleReadStartMaterializationBoundaryAntiDriftTest {

    @Test
    void selfStartMaterializationBoundaryMustNotTreatSelfVisibleSupportingReadAsItsCommandGate() {
        assertThat(fieldTypes(SelfAttemptEntryService.class))
            
            .doesNotContain(SelfExecutionAdmissionFoundationStateReadService.class);
    }

    @Test
    void selfStartMaterializationSourceMustNotCallSelfVisibleSupportingReadBeforeContinuationOrCreate() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptEntryService.java"))
            
            .doesNotContain("findSelfExecutionAdmissionFoundationState(");
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
