package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет архитектурные ограничения вокруг {@code PersonnelApplyOwnerService}.
 * Такой тест не даёт соседним частям размыть границы решения.
 */
class PersonnelApplyOwnerServiceArchitectureTest {

    @Test
    void integrationPersonnelDoesNotImportOwnerPersistenceOrRepositories() throws Exception {
        String integrationPersonnelSource = readSources(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel"
        ));

        assertThat(integrationPersonnelSource)
            .doesNotContain("userorg.infrastructure.persistence")
            .doesNotContain("access.infrastructure.persistence")
            .doesNotContain("SpringData")
            .doesNotContain("Repository")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(");
    }

    @Test
    void importTypedOwnerCommandExecutorRemainsNarrowAndHasNoPersonnelGenericMutationMethods() throws Exception {
        String typedOwnerExecutor = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportTypedOwnerCommandExecutor.java"
        ));

        assertThat(typedOwnerExecutor)
            .contains("AppUser updateAppUser(AppUser user)")
            .doesNotContain("Personnel")
            .doesNotContain("personnel")
            .doesNotContain("replacePrimaryHomeUnit")
            .doesNotContain("deactivateUser")
            .doesNotContain("temporaryManagementDelegation")
            .doesNotContain("generic");
    }

    @Test
    void stageSevenApplyRuntimeMayExistButGenericOwnerPatchExecutorAndApplyWebSurfaceMustNotExist() throws Exception {
        Path applyService = Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelApplyService.java"
        );
        Path genericOwnerPatchExecutor = Path.of(
            "src/main/java/com/vladislav/training/platform/integration/service/GenericOwnerPatchExecutor.java"
        );
        String applyServiceSource = Files.readString(applyService);

        assertThat(applyService).exists();
        assertThat(applyServiceSource)
            .contains("createPersonnelExcelApply")
            .doesNotContain("@PostMapping")
            .doesNotContain("@RequestMapping")
            .doesNotContain("ImportCommandService")
            .doesNotContain("import_job")
            .doesNotContain("import_job_item");
        assertThat(genericOwnerPatchExecutor).doesNotExist();
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
