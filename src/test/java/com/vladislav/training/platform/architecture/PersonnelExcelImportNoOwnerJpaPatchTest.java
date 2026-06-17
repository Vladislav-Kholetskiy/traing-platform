package com.vladislav.training.platform.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelExcelImportNoOwnerJpaPatch}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelExcelImportNoOwnerJpaPatchTest {

    @Test
    void personnelContourHasNoOwnerJpaPatchOrRepositoryMutation() throws Exception {
        String source = readSources(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel"
        ));

        assertThat(source)
            .doesNotContain("userorg.infrastructure.persistence")
            .doesNotContain("access.infrastructure.persistence")
            .doesNotContain("SpringData")
            .doesNotContain("Jpa")
            .doesNotContain("@Entity")
            .doesNotContain("UserOrganizationAssignmentEntity")
            .doesNotContain("UserRoleAssignmentEntity")
            .doesNotContain("UserAccessAreaEntity")
            .doesNotContain("ManagementRelationEntity")
            .doesNotContain("TemporaryRoleAssignmentEntity")
            .doesNotContain("TemporaryAccessAreaEntity")
            .doesNotContain("TemporaryManagementDelegationEntity")
            .doesNotContain("Repository")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(")
            .doesNotContain("@Query")
            .doesNotContain("nativeQuery")
            .doesNotContain("update user_")
            .doesNotContain("update access_")
            .doesNotContain("insert into")
            .doesNotContain("delete from");
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
