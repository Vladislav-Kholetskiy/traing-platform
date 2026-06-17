package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code ImportLaunchAdmission}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportLaunchAdmissionTest {

    private static final Path IMPORT_COMMAND_SERVICE_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"
    );
    private static final Path IMPORT_ADMIN_COMMAND_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminCommandController.java"
    );

    @Test
    void actorFacingLaunchPathMustStayAdmittedBeforePersistenceAndNeverInvokeProcessing() throws Exception {
        String serviceSource = Files.readString(IMPORT_COMMAND_SERVICE_IMPL);

        assertThat(serviceSource)
            .contains("createImportJobLaunch()")
            .contains("capabilityAdmissionPolicy.check(request)")
            .contains("importJobRepository.saveImportJob(")
            .contains("importJobItemRepository.saveImportJobItem(")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("processImportJob(")
            .doesNotContain("processImportJobItem(");

        assertThat(serviceSource.indexOf("capabilityAdmissionPolicy.check(request)"))
            .isLessThan(serviceSource.indexOf("importJobRepository.saveImportJob("));
    }

    @Test
    void actorFacingControllerMustDelegateOnlyToImportCommandService() throws Exception {
        String controllerSource = Files.readString(IMPORT_ADMIN_COMMAND_CONTROLLER);

        assertThat(controllerSource)
            .contains("ImportCommandService")
            .contains("launchImportJob(")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportJobRepository")
            .doesNotContain("ImportJobItemRepository")
            .doesNotContain("UserCommandService")
            .doesNotContain("AppUserRepository");
    }
}
