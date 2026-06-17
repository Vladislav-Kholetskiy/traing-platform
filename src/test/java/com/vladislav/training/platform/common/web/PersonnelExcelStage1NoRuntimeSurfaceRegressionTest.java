package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code PersonnelExcelStage1NoRuntimeSurface} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class PersonnelExcelStage1NoRuntimeSurfaceRegressionTest {

    @Test
    void stageEightMayOpenDedicatedApplySurfaceButMustNotIntroduceGenericImportRouteOrOwnerSideEffects() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(controller)
            .contains("/api/v1/admin/import/personnel-excel")
            .contains("/dry-run")
            .contains("/apply")
            .doesNotContain("/api/v1/admin/import/{target}")
            .doesNotContain("IMPORT_JOB_LAUNCH")
            .doesNotContain("ImportCommandService")
            .doesNotContain("UserAdministrationCommandService")
            .doesNotContain("AccessAdministrationCommandService")
            .doesNotContain("Audit")
            .doesNotContain("Notification")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance");
    }

    @Test
    void stageSevenMayIntroduceApplyServiceButStillMustNotIntroduceImportJobBridge() throws Exception {
        String applyService = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelApplyService.java"
        ));
        assertThat(applyService)
            .contains("createPersonnelExcelApply")
            .doesNotContain("@PostMapping")
            .doesNotContain("@RequestMapping")
            .doesNotContain("ImportCommandService")
            .doesNotContain("import_job")
            .doesNotContain("import_job_item")
            .doesNotContain("Notification")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance");

        String dryRunFacade = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelWorkbookDryRunFacade.java"
        ));
        assertThat(dryRunFacade)
            .doesNotContain("import_job")
            .doesNotContain("import_job_item")
            .doesNotContain("ImportCommandService");
    }

    @Test
    void stageFiveMustNotWidenGenericPersonnelOwnerMutationSurface() throws Exception {
        String typedOwnerExecutor = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportTypedOwnerCommandExecutor.java"
        ));

        assertThat(typedOwnerExecutor)
            .doesNotContain("personnel")
            .doesNotContain("Personnel")
            .doesNotContain("temporary_management_delegation")
            .doesNotContain("temporary_access_area")
            .doesNotContain("user_organization_assignment");
    }
}
