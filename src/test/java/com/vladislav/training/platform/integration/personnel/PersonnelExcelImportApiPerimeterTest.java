package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelExcelImportApiPerimeter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelExcelImportApiPerimeterTest {

    @Test
    void controllerSourceDoesNotReferenceImportOwnerInfrastructure() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(source)
            .doesNotContain("ImportCommandService")
            .doesNotContain("UserAdministrationCommandService")
            .doesNotContain("AccessAdministrationCommandService")
            .doesNotContain("Audit")
            .doesNotContain("Notification")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(");
    }

    @Test
    void controllerRoutesStayDedicatedAndDoNotUseGenericTargetRoute() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(source)
            .contains("/dry-run")
            .contains("/apply")
            .doesNotContain("/api/v1/admin/import/{target}")
            .doesNotContain("IMPORT_JOB_LAUNCH");
    }

    @Test
    void noGenericTargetRouteIsIntroduced() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(source)
            .doesNotContain("/api/v1/admin/import/{target}")
            .doesNotContain("@RequestMapping(\"/api/v1/admin/import/{target}\")")
            .contains("/api/v1/admin/import/personnel-excel");
    }
}
