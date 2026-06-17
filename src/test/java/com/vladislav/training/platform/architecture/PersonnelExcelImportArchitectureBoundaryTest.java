package com.vladislav.training.platform.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code PersonnelExcelImportArchitecture}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class PersonnelExcelImportArchitectureBoundaryTest {

    @Test
    void directMvpPersonnelContourDoesNotUseGenericImportBridgeOrTargetRoute() throws Exception {
        String personnelSources = readSources(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel"
        ));
        String ownerExecutor = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportTypedOwnerCommandExecutor.java"
        ));
        String controller = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(personnelSources)
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("PersonnelImportJobBridge")
            .doesNotContain("import_job")
            .doesNotContain("import_job_item");

        assertThat(controller)
            .contains("/dry-run")
            .contains("/apply")
            .doesNotContain("{target}")
            .doesNotContain("@RequestBody")
            .doesNotContain("ImportCommandService");

        assertThat(ownerExecutor)
            .contains("AppUser updateAppUser(AppUser user)")
            .doesNotContain("Personnel")
            .doesNotContain("assignRole")
            .doesNotContain("replacePrimaryHomeUnit")
            .doesNotContain("assignUserAccessArea")
            .doesNotContain("assignManagementRelation")
            .doesNotContain("assignTemporary");
    }

    @Test
    void dryRunContourRemainsReadOnlyAndApplyContourRemainsOwnerServiceOrchestration() throws Exception {
        String dryRunService = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelDryRunService.java"
        ));
        String dryRunFacade = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelWorkbookDryRunFacade.java"
        ));
        String applyService = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelApplyService.java"
        ));

        assertThat(dryRunService + "\n" + dryRunFacade)
            .doesNotContain("UserAdministrationCommandService")
            .doesNotContain("AccessAdministrationCommandService")
            .doesNotContain("ImportCommandService")
            .doesNotContain("import_job")
            .doesNotContain("Audit")
            .doesNotContain("Notification")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(");

        assertThat(applyService)
            .contains("PersonnelOwnerMutationExecutor")
            .doesNotContain("ImportCommandService")
            .doesNotContain("import_job")
            .doesNotContain("Notification")
            .doesNotContain("AuditRead")
            .doesNotContain("answer_option.is_correct")
            .doesNotContain("test_id_snapshot")
            .doesNotContain("test_name_snapshot");
    }

    private String readSources(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(Files::isRegularFile).toList();
            StringBuilder builder = new StringBuilder();
            for (Path file : files) {
                builder.append(Files.readString(file)).append('\n');
            }
            return builder.toString();
        }
    }
}
