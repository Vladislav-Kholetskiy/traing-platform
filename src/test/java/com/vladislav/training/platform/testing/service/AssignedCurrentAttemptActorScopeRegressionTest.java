package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.testing.infrastructure.persistence.JpaTestAttemptRepositoryAdapter;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AssignedCurrentAttemptActorScope} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AssignedCurrentAttemptActorScopeRegressionTest {

    @Test
    void assignedCurrentAttemptServiceBoundaryMustNotExposeActorlessAssignedLookupSignature() {
        assertThat(publicMethodSignatures(ActiveAttemptOwnerLocalReadService.class))
            
            .doesNotContain("findActiveAssignedAttempt(java.lang.Long)");
        assertThat(publicMethodSignatures(ActiveAttemptOwnerLocalReadService.class))
            .contains("findActiveAssignedAttemptForActor(java.lang.Long, java.lang.Long)");
        assertThat(readUnchecked("src/main/java/com/vladislav/training/platform/testing/service/ActiveAttemptOwnerLocalReadService.java"))
            .contains("accessSpecificationPolicy.canRead(")
            .contains("AccessReadArea.ASSIGNED_CURRENT_ATTEMPT");
    }

    @Test
    void assignedCurrentAttemptRepositoryBoundaryMustNotExposeActorlessAssignedLookupSignature() {
        assertThat(publicMethodSignatures(TestAttemptRepository.class))
            
            .doesNotContain("findActiveAssignedAttempt(java.lang.Long)");
        assertThat(publicMethodSignatures(TestAttemptRepository.class))
            .contains("findActiveAssignedAttemptForActor(java.lang.Long, java.lang.Long)");
    }

    @Test
    void assignedCurrentAttemptJpaAdapterMustNotRetainActorlessAssignmentTestOnlyLookup() throws Exception {
        assertThat(publicMethodSignatures(JpaTestAttemptRepositoryAdapter.class))
            
            .doesNotContain("findActiveAssignedAttempt(java.lang.Long)");
        assertThat(publicMethodSignatures(JpaTestAttemptRepositoryAdapter.class))
            .contains("findActiveAssignedAttemptForActor(java.lang.Long, java.lang.Long)");

        assertThat(read("src/main/java/com/vladislav/training/platform/testing/infrastructure/persistence/JpaTestAttemptRepositoryAdapter.java"))
            
            .doesNotContain("findByAssignmentTestIdAndStatusIn(assignmentTestId, ACTIVE_STATUSES)")
            .contains("findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn(");
    }

    private Set<String> publicMethodSignatures(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(this::signature)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String signature(Method method) {
        return method.getName() + "(" + Arrays.stream(method.getParameterTypes())
            .map(Class::getName)
            .collect(Collectors.joining(", ")) + ")";
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private String readUnchecked(String relativePath) {
        try {
            return read(relativePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source: " + relativePath, exception);
        }
    }
}
