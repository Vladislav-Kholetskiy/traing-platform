package com.vladislav.training.platform.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImportApiPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportApiPerimeterRegressionTest {

    private static final Path INTEGRATION_CONTROLLER_ROOT = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/controller"
    );
    private static final Path IMPORT_ADMIN_COMMAND_CONTROLLER = INTEGRATION_CONTROLLER_ROOT.resolve(
        "ImportAdminCommandController.java"
    );
    private static final Path IMPORT_ADMIN_READ_CONTROLLER = INTEGRATION_CONTROLLER_ROOT.resolve(
        "ImportAdminReadController.java"
    );
    private static final Path IMPORT_ITEM_REVIEW_CONTROLLER = INTEGRATION_CONTROLLER_ROOT.resolve(
        "ImportItemReviewController.java"
    );

    @Test
    void stage61LaunchRouteRemainsAndStage62ReadRoutesRemainWhileStage63UsesOnlyTypedReviewRoutes() throws Exception {
        String commandControllerSource = Files.readString(IMPORT_ADMIN_COMMAND_CONTROLLER);
        String readControllerSource = Files.readString(IMPORT_ADMIN_READ_CONTROLLER);
        String reviewControllerSource = Files.readString(IMPORT_ITEM_REVIEW_CONTROLLER);

        assertThat(commandControllerSource)
            .contains("@RequestMapping(\"/api/v1/admin/import-jobs\")")
            .contains("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("/process")
            .doesNotContain("/retry")
            .doesNotContain("/reconcile")
            .doesNotContain("/backfill")
            .doesNotContain("/repair");

        assertThat(readControllerSource)
            .contains("@RequestMapping(\"/api/v1/admin\")")
            .contains("@GetMapping(\"/import-jobs\")")
            .contains("@GetMapping(\"/import-jobs/{importJobId}\")")
            .contains("@GetMapping(\"/import-jobs/{importJobId}/items\")")
            .contains("@GetMapping(\"/import-job-items/{itemId}\")")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("/apply-review")
            .doesNotContain("/reject-review")
            .doesNotContain("/resolve")
            .doesNotContain("/reject")
            .doesNotContain("/process")
            .doesNotContain("/retry")
            .doesNotContain("/reconcile")
            .doesNotContain("/backfill")
            .doesNotContain("/repair");

        assertThat(reviewControllerSource)
            .contains("@RequestMapping(\"/api/v1/admin/import-job-items\")")
            .contains("@PostMapping(\"/{itemId}/apply-review\")")
            .contains("@PostMapping(\"/{itemId}/reject-review\")")
            .doesNotContain("@PutMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("/process")
            .doesNotContain("/retry")
            .doesNotContain("/reconcile")
            .doesNotContain("/backfill")
            .doesNotContain("/repair");
    }

    @Test
    void integrationControllerContourDoesNotExposeStage62OrStage63Routes() throws Exception {
        try (java.util.stream.Stream<Path> stream = Files.walk(INTEGRATION_CONTROLLER_ROOT)) {
            List<Path> controllerFiles = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> path.getFileName().toString().contains("Controller"))
                .toList();

            assertThat(controllerFiles)
                .extracting(Path::getFileName)
                .extracting(Path::toString)
                .containsExactlyInAnyOrder(
                    "ImportAdminCommandController.java",
                    "ImportAdminReadController.java",
                    "ImportItemReviewController.java"
                );

            for (Path path : controllerFiles) {
                String source = Files.readString(path);
                assertThat(source)
                    .doesNotContain("/resolve")
                    .doesNotContain("/reject\"")
                    .doesNotContain("/process")
                    .doesNotContain("/retry")
                    .doesNotContain("/reconcile")
                    .doesNotContain("/backfill")
                    .doesNotContain("/repair")
                    .doesNotContain("@PatchMapping")
                    .doesNotContain("@PutMapping")
                    .doesNotContain("@DeleteMapping");
            }
        }
    }
}
