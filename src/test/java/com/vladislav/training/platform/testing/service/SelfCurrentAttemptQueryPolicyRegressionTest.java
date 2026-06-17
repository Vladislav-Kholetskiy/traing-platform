package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.access.service.AccessReadArea;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfCurrentAttemptQueryPolicy} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfCurrentAttemptQueryPolicyRegressionTest {

    @Test
    void selfCurrentAttemptReadMustHaveDedicatedReadContourVocabulary() {
        assertThat(Arrays.stream(AccessReadArea.values()).map(Enum::name).toList())
            
            .contains("SELF_CURRENT_ATTEMPT");
    }

    @Test
    void selfCurrentAttemptSupportingReadMustApplyExplicitPolicyVerdictBeforeOwnerLocalMaterialization() throws IOException {
        assertThat(fieldTypes(SelfCurrentAttemptReadService.class))
            
            .contains(
                com.vladislav.training.platform.access.service.AccessSpecificationPolicy.class,
                com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver.class
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/SelfCurrentAttemptReadService.java"))
            
            .contains("accessSpecificationPolicy.canRead(")
            .contains("contextResolver.resolveSelfCurrentAttemptContext(");
    }

    @Test
    void selfCurrentAttemptReadNeverFallsBackToAssignedCurrentAttemptContour() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/SelfCurrentAttemptReadService.java"))
            
            .doesNotContain("ASSIGNED_CURRENT_ATTEMPT")
            .contains("resolveSelfCurrentAttemptContext(");
    }

    @Test
    void selfCurrentAttemptSupportingReadDoesNotDependOnMutationRecordingHandoffOrAuditCollaborators() {
        assertThat(fieldTypes(SelfCurrentAttemptReadService.class))
            
            .doesNotContain(
                SelfAttemptEntryService.class,
                ActiveAttemptAnswerMutationService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                CriticalCommandAuditSupport.class
            );
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
