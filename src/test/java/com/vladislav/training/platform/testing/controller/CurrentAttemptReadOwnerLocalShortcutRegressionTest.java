package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.testing.service.ActiveAttemptOwnerLocalReadService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code CurrentAttemptReadOwnerLocalShortcut} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class CurrentAttemptReadOwnerLocalShortcutRegressionTest {

    @Test
    void currentAttemptReadControllerMustNotOwnOwnerLocalAttemptMaterialization() throws IOException {
        assertThat(fieldTypes(CurrentAttemptReadController.class))
            
            .doesNotContain(ActiveAttemptOwnerLocalReadService.class);

        assertThat(read("src/main/java/com/vladislav/training/platform/testing/controller/CurrentAttemptReadController.java"))
            
            .doesNotContain("findActiveAssignedAttemptForActor(")
            .doesNotContain("findActiveSelfAttempt(");
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
