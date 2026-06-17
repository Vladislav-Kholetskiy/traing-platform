package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelCurrentStateReaderNoMutation}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelCurrentStateReaderNoMutationTest {

    @Test
    void readerImplementationIsReadOnlyAndAvoidsForbiddenRuntimeDependencies() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/OwnerReadPersonnelCurrentStateReader.java"
        ));

        assertThat(source)
            .contains("@Transactional(readOnly = true)")
            .doesNotContain("UserAdministrationCommandService")
            .doesNotContain("AccessAdministrationCommandService")
            .doesNotContain("ManagementRelationCommandService")
            .doesNotContain("UserAccessAreaCommandService")
            .doesNotContain("TemporaryRoleAssignmentCommandService")
            .doesNotContain("TemporaryAccessAreaCommandService")
            .doesNotContain("TemporaryManagementDelegationCommandService")
            .doesNotContain("ImportCommandService")
            .doesNotContain("import_job")
            .doesNotContain("import_job_item")
            .doesNotContain("AuditRead")
            .doesNotContain("Notification")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(");
    }
}
