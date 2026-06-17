package com.vladislav.training.platform.audit.service;

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
 * Фиксирует словарь и смысловые границы вокруг {@code AuditCommandSupport}.
 * Это помогает не расползтись именам и договорённостям.
 */
class AuditCommandSupportVocabularyLockTest {

    @Test
    void auditServicePackageKeepsScn14AsSynchronousCompanionRatherThanOwnerHistory() throws IOException {
        String packageInfo = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/audit/service/package-info.java"
        ));

        assertThat(packageInfo)
            .contains("package com.vladislav.training.platform.audit.service")
            .doesNotContain("owner history");
    }

    @Test
    void criticalCommandAuditSupportKeepsCompanionVocabulary() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/audit/service/CriticalCommandAuditSupport.java"
        ));

        assertThat(methodNames(CriticalCommandAuditSupport.class))
            .contains("resolveInteractiveActorUserId", "resolveSystemActorUserId", "recordAudit", "buildAuditContext")
            .doesNotContain("recordHistory", "appendOwnerHistory", "applyLifecycleTransition", "executeCommand");
        assertThat(source)
            .contains("class CriticalCommandAuditSupport")
            .contains("recordAudit");
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }
}
