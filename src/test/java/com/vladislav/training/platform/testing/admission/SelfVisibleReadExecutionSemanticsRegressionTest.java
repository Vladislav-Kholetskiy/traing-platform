package com.vladislav.training.platform.testing.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.testing.service.SelfVisibleTestVisibilityFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfVisibleReadExecutionSemantics} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfVisibleReadExecutionSemanticsRegressionTest {

    @Test
    void selfExecutionSupportingReadMustNotReuseSelfVisibleReadFilterAsItsAuthoritativeBoundary() {
        assertThat(fieldTypes(SelfExecutionAdmissionFoundationStateReadServiceImpl.class))
            
            .doesNotContain(SelfVisibleTestVisibilityFilter.class);
    }

    @Test
    void selfExecutionSupportingReadSourceMustNotDescribeOrInvokeMaterializedSelfVisibleBaseline() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/admission/SelfExecutionAdmissionFoundationStateReadServiceImpl.java"))
            
            .doesNotContain("already materialized self-visible baseline predicate")
            .doesNotContain("requireSelfVisible(");
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
