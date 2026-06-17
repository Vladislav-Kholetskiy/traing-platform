package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SchedulerNotOwner} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SchedulerNotOwnerRegressionTest {

    private static final Path PRODUCTION_SOURCE_ROOT =
        Path.of("src/main/java/com/vladislav/training/platform");
    private static final List<String> PRIMARY_WAVE_SEVEN_SCOPE_MARKERS = List.of(
        "scheduler",
        "reconciliation",
        "recovery",
        "recover",
        "repair",
        "maintenance",
        "backfill",
        "replay"
    );

    private static final List<String> SECONDARY_WAVE_SEVEN_SCOPE_MARKERS = List.of("rebuild");

    private static final List<String> ALLOWED_CONTRACT_REFERENCES = List.of(
        "AssignmentStatusRecalculationService",
        "AttemptStatusRecalculationService",
        "AnalyticsRebuildService",
        "AnalyticsRefreshService",
        "ResultRecordingService"
    );

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
        "\\b(class|interface|record|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+([\\w\\.]+);");
    private static final Pattern SCHEDULING_RUNTIME_PATTERN = Pattern.compile("(?m)^\\s*@(?:Scheduled|EnableScheduling)\\b");

    @Test
    void maintenanceSchedulerRecoveryCandidatesDoNotTakeOwnerLifecycleOver() throws IOException {
        List<Path> productionJavaSources = productionJavaSources();

        assertThat(productionJavaSources).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Path sourceFile : productionJavaSources) {
            String source = Files.readString(sourceFile);
            String sanitizedSource = stripComments(source);

            if (!isMaintenanceCandidate(sourceFile, sanitizedSource)) {
                continue;
            }

            List<String> importedTypes = importedTypes(sanitizedSource);

            for (String importedType : importedTypes) {
                if (isForbiddenTestingImport(importedType)) {
                    violations.add(sourceFile + " -> forbidden Testing owner import " + importedType);
                }
                if (isForbiddenAssignmentImport(importedType)) {
                    violations.add(sourceFile + " -> forbidden Assignment owner import " + importedType);
                }
                if (isForbiddenResultImport(importedType)) {
                    violations.add(sourceFile + " -> forbidden Result owner import " + importedType);
                }
            }

            for (String forbiddenReference : List.of(
                "TestAttemptEntity",
                "TestAttemptRepository",
                "SpringDataTestAttempt",
                "SelfAttemptSubmit",
                "AssignedAttemptSubmit",
                "AttemptSubmit",
                "AttemptAbandon",
                "Terminalization",
                "terminalize",
                "submit",
                "abandon",
                "setStatus(",
                "TestAttemptStatus.COMPLETED",
                "TestAttemptStatus.ABANDONED",
                "TestAttemptStatus.EXPIRED",
                "TestAttemptStatus.IN_PROGRESS"
            )) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden Testing lifecycle takeover " + forbiddenReference);
                }
            }

            for (String forbiddenReference : List.of(
                "AssignmentEntity",
                "AssignmentRepository",
                "SpringDataAssignment",
                "AssignmentStatus.",
                "assignment.status",
                ".status(",
                "closeAssignment",
                "cancelAssignment",
                "completeAssignment"
            )) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden Assignment lifecycle takeover " + forbiddenReference);
                }
            }

            for (String forbiddenReference : List.of(
                "ResultEntity",
                "ResultQuestionSnapshotEntity",
                "ResultAnswerOptionSnapshotEntity",
                "ResultRepository",
                "ResultQuestionSnapshotRepository",
                "ResultAnswerOptionSnapshotRepository"
            )) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden Result lifecycle takeover " + forbiddenReference);
                }
            }

            for (Pattern forbiddenSqlPattern : List.of(
                Pattern.compile("(?i)\\bupdate\\s+test_attempt\\b"),
                Pattern.compile("(?i)\\binsert\\s+into\\s+result\\b"),
                Pattern.compile("(?i)\\bupdate\\s+assignment\\b"),
                Pattern.compile("(?i)\\bupdate\\s+result\\b"),
                Pattern.compile("(?i)\\binsert\\s+into\\s+result_question_snapshot\\b"),
                Pattern.compile("(?i)\\binsert\\s+into\\s+result_answer_option_snapshot\\b")
            )) {
                if (forbiddenSqlPattern.matcher(sanitizedSource).find()) {
                    violations.add(sourceFile + " -> forbidden direct SQL owner mutation " + forbiddenSqlPattern.pattern());
                }
            }

            if (importsTestingOwnerRepository(importedTypes)
                || importsAssignmentOwnerPersistence(importedTypes)
                || importsResultOwnerPersistence(importedTypes)) {
                for (String forbiddenMutation : List.of(".save(", ".saveAll(", ".saveAndFlush(")) {
                    if (sanitizedSource.contains(forbiddenMutation)) {
                        violations.add(sourceFile + " -> forbidden owner repository mutation shortcut " + forbiddenMutation);
                    }
                }
            }
        }

        assertThat(violations)
            
            .isEmpty();
    }

    private List<Path> productionJavaSources() throws IOException {
        try (Stream<Path> paths = Files.walk(PRODUCTION_SOURCE_ROOT)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }
    }

    private boolean isMaintenanceCandidate(Path sourceFile, String sanitizedSource) {
        String normalizedPath = normalize(sourceFile);
        String className = declaredClassName(sanitizedSource).toLowerCase();

        if (containsPrimaryMarker(normalizedPath) || containsPrimaryMarker(className)) {
            return true;
        }

        return containsSecondaryMarker(normalizedPath)
            && (normalizedPath.contains("/infrastructure/") || normalizedPath.contains("/scheduler/"));
    }

    private boolean containsPrimaryMarker(String value) {
        return PRIMARY_WAVE_SEVEN_SCOPE_MARKERS.stream().anyMatch(value::contains);
    }

    private boolean containsSecondaryMarker(String value) {
        return SECONDARY_WAVE_SEVEN_SCOPE_MARKERS.stream().anyMatch(value::contains);
    }

    private String declaredClassName(String sanitizedSource) {
        Matcher matcher = CLASS_DECLARATION_PATTERN.matcher(sanitizedSource);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    private List<String> importedTypes(String sanitizedSource) {
        List<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(sanitizedSource);
        while (matcher.find()) {
            String importedType = matcher.group(1);
            if (ALLOWED_CONTRACT_REFERENCES.stream().anyMatch(importedType::endsWith)) {
                continue;
            }
            imports.add(importedType);
        }
        return imports;
    }

    private boolean isForbiddenTestingImport(String importedType) {
        return importedType.startsWith("com.vladislav.training.platform.testing.infrastructure.persistence.")
            || importedType.startsWith("com.vladislav.training.platform.testing.repository.")
            || importedType.endsWith("AttemptSubmitTerminalService")
            || importedType.endsWith("AttemptSubmitSequencingService")
            || importedType.endsWith("AttemptSubmissionService")
            || importedType.endsWith("AttemptAbandonTerminalService")
            || importedType.endsWith("AttemptTerminalizationOutcome")
            || importedType.endsWith("AttemptTerminalCriticalAuditPayloadFactory");
    }

    private boolean isForbiddenAssignmentImport(String importedType) {
        return importedType.startsWith("com.vladislav.training.platform.assignment.infrastructure.persistence.")
            || importedType.startsWith("com.vladislav.training.platform.assignment.repository.SpringData");
    }

    private boolean isForbiddenResultImport(String importedType) {
        return importedType.startsWith("com.vladislav.training.platform.result.infrastructure.persistence.")
            || importedType.startsWith("com.vladislav.training.platform.result.repository.");
    }

    private boolean importsTestingOwnerRepository(List<String> importedTypes) {
        return importedTypes.stream().anyMatch(importedType ->
            importedType.startsWith("com.vladislav.training.platform.testing.infrastructure.persistence.")
                || importedType.startsWith("com.vladislav.training.platform.testing.repository.")
        );
    }

    private boolean importsAssignmentOwnerPersistence(List<String> importedTypes) {
        return importedTypes.stream().anyMatch(importedType ->
            importedType.startsWith("com.vladislav.training.platform.assignment.infrastructure.persistence.")
                || importedType.startsWith("com.vladislav.training.platform.assignment.repository.")
        );
    }

    private boolean importsResultOwnerPersistence(List<String> importedTypes) {
        return importedTypes.stream().anyMatch(importedType ->
            importedType.startsWith("com.vladislav.training.platform.result.infrastructure.persistence.")
                || importedType.startsWith("com.vladislav.training.platform.result.repository.")
        );
    }

    private String normalize(Path sourceFile) {
        return sourceFile.toString().replace('\\', '/').toLowerCase();
    }

    private String stripComments(String source) {
        String withoutBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)//.*$", "");
    }
}

