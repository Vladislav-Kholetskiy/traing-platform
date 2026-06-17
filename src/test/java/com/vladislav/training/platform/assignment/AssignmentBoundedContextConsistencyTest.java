package com.vladislav.training.platform.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.controller.AssignmentAdministrativeActionController;
import com.vladislav.training.platform.assignment.controller.AssignmentCampaignLaunchController;
import com.vladislav.training.platform.assignment.controller.AssignmentSelfScopedReadController;
import com.vladislav.training.platform.assignment.controller.dto.AssignmentCampaignResponse;
import com.vladislav.training.platform.assignment.controller.dto.LaunchAssignmentCampaignRequest;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentPersistenceMapper;
import com.vladislav.training.platform.assignment.infrastructure.scheduler.AssignmentStatusRecalculationScheduler;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AssignmentBoundedContextConsistency}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentBoundedContextConsistencyTest {

    private static final Path ASSIGNMENT_ROOT = Path.of(
        "src/main/java/com/vladislav/training/platform/assignment"
    );
    private static final Pattern FOREIGN_IMPORT_PATTERN = Pattern.compile(
        "^import\\s+com\\.vladislav\\.training\\.platform\\.(access|audit|content|userorg|result|analytics|integration|notification|testing)\\..+;$"
    );

    @Test
    void assignmentRetainsOwnBoundedContextTopologyAndLayerSplit() {
        assertThat(ASSIGNMENT_ROOT).isDirectory();
        assertThat(ASSIGNMENT_ROOT.resolve("controller")).isDirectory();
        assertThat(ASSIGNMENT_ROOT.resolve("controller/dto")).isDirectory();
        assertThat(ASSIGNMENT_ROOT.resolve("domain")).isDirectory();
        assertThat(ASSIGNMENT_ROOT.resolve("service")).isDirectory();
        assertThat(ASSIGNMENT_ROOT.resolve("repository")).isDirectory();
        assertThat(ASSIGNMENT_ROOT.resolve("infrastructure/persistence")).isDirectory();
        assertThat(ASSIGNMENT_ROOT.resolve("infrastructure/scheduler")).isDirectory();

        assertThat(Assignment.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.domain");
        assertThat(AssignmentCampaignLaunchController.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.controller");
        assertThat(AssignmentAdministrativeActionController.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.controller");
        assertThat(AssignmentSelfScopedReadController.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.controller");
        assertThat(LaunchAssignmentCampaignRequest.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.controller.dto");
        assertThat(AssignmentCampaignResponse.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.controller.dto");
        assertThat(loadClass("com.vladislav.training.platform.assignment.service.AssignmentCampaignCommandServiceImpl").getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.service");
        assertThat(loadClass("com.vladislav.training.platform.assignment.service.AssignmentAdministrativeActionServiceImpl").getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.service");
        assertThat(AssignmentRepository.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.repository");
        assertThat(AssignmentCampaignReadRepository.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.repository");
        assertThat(AssignmentPersistenceMapper.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.infrastructure.persistence");
        assertThat(AssignmentStatusRecalculationScheduler.class.getPackageName())
            .isEqualTo("com.vladislav.training.platform.assignment.infrastructure.scheduler");
        assertThat(Path.of("src/main/java/com/vladislav/training/platform/assignment/controller/AssignmentReadController.java"))
            .doesNotExist();
    }

    @Test
    void assignmentAndNeighborPackageDocsKeepCanonicalBoundedContextReading() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.service");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.controller");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/dto/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.controller.dto");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/repository/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.repository");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.infrastructure.persistence");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/infrastructure/scheduler/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.infrastructure.scheduler");

        assertThat(read("src/main/java/com/vladislav/training/platform/audit/package-info.java"))
            .contains("package com.vladislav.training.platform.audit");

        assertThat(read("src/main/java/com/vladislav/training/platform/audit/service/package-info.java"))
            .contains("package com.vladislav.training.platform.audit.service");

        assertThat(read("src/main/java/com/vladislav/training/platform/application/policy/package-info.java"))
            .contains("package com.vladislav.training.platform.application.policy");
    }

    @Test
    void assignmentUsesForeignModulesOnlyAsAllowedSupportContours() throws IOException {
        Map<String, List<String>> importsByModule = foreignImportsByModule();

        assertThat(importsByModule.getOrDefault("result", List.of()))
            .allSatisfy(path -> assertThat(path)
                .contains("/assignment/infrastructure/persistence/ResultBackedAssignmentStatusDefiningCountedResultFactsReader.java"));
        assertThat(importsByModule.getOrDefault("analytics", List.of())).isEmpty();
        assertThat(importsByModule.getOrDefault("integration", List.of())).isEmpty();
        assertThat(importsByModule.getOrDefault("notification", List.of()))
            .allSatisfy(path -> assertThat(path).contains("/assignment/service/"));
        assertThat(importsByModule.getOrDefault("testing", List.of())).isEmpty();

        assertThat(importsByModule.getOrDefault("access", List.of()))
            .allSatisfy(path -> assertThat(path)
                .containsAnyOf("/assignment/service/", "/assignment/repository/"));
        assertThat(importsByModule.getOrDefault("audit", List.of()))
            .allSatisfy(path -> assertThat(path).contains("/assignment/service/"));
        assertThat(importsByModule.getOrDefault("content", List.of()))
            .allSatisfy(path -> assertThat(path)
                .containsAnyOf(
                    "/assignment/service/",
                    "/assignment/controller/",
                    "/assignment/controller/dto/",
                    "/assignment/infrastructure/persistence/JpaManagerialCurrentSupervisionReadRepositoryAdapter.java"
                ));
        assertThat(importsByModule.getOrDefault("userorg", List.of()))
            .allSatisfy(path -> assertThat(path)
                .containsAnyOf(
                    "/assignment/service/",
                    "/assignment/controller/",
                    "/assignment/controller/dto/",
                    "/assignment/infrastructure/persistence/JpaManagerialCurrentSupervisionReadRepositoryAdapter.java"
                ));
    }

    private Map<String, List<String>> foreignImportsByModule() throws IOException {
        Map<String, List<String>> importsByModule = new LinkedHashMap<>();

        try (Stream<Path> files = Files.walk(ASSIGNMENT_ROOT)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    Matcher matcher = FOREIGN_IMPORT_PATTERN.matcher(line.trim());
                    if (matcher.matches()) {
                        importsByModule.computeIfAbsent(matcher.group(1), ignored -> new ArrayList<>())
                            .add(normalize(file));
                    }
                }
            }
        }

        return importsByModule;
    }

    private Class<?> loadClass(String fqcn) {
        try {
            return Class.forName(fqcn);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Expected assignment class is missing: " + fqcn, exception);
        }
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }
}

