package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Проверяет, что {@code ActiveAttemptAnswerMutationActorScope} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ActiveAttemptAnswerMutationActorScopeRegressionTest {

    @Test
    void answerMutationServiceMustNotExposeActorlessMutableSignatures() {
        assertThat(publicMethodSignatures(ActiveAttemptAnswerMutationService.class))
            
            .doesNotContain(
                "saveOrReplaceAnswer(java.lang.Long, java.lang.Long, java.util.List, java.time.Instant)",
                "clearAnswer(java.lang.Long, java.lang.Long, java.time.Instant)"
            );
    }

    @Test
    void answerMutationCoreMustNotLockAttemptOnlyByActorlessPrimaryKey() throws Exception {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/ActiveAttemptAnswerMutationService.java"))
            
            .doesNotContain("findAndLockTestAttemptById(testAttemptId)");
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
}
