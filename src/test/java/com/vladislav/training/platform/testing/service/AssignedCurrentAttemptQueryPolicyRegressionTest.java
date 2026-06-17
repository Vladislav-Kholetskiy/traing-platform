package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AssignedCurrentAttemptQueryPolicy} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AssignedCurrentAttemptQueryPolicyRegressionTest {

    @Test
    void assignedCurrentAttemptSupportingReadMustApplyExplicitPolicyVerdictBeforeOwnerLocalMaterialization() throws IOException {
        assertThat(fieldTypes(AssignedCurrentAttemptReadService.class))
            
            .contains(
                com.vladislav.training.platform.access.service.AccessSpecificationPolicy.class,
                com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver.class
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/AssignedCurrentAttemptReadService.java"))
            
            .contains("accessSpecificationPolicy.canRead(")
            .contains("AccessReadArea.ASSIGNED_CURRENT_ATTEMPT")
            .contains("contextResolver.resolveActorSelfScope(");
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .toList();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
