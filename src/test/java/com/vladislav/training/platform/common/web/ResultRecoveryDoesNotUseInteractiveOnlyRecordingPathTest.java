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
 * Проверяет поведение {@code ResultRecoveryDoesNotUseInteractiveOnlyRecordingPath}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ResultRecoveryDoesNotUseInteractiveOnlyRecordingPathTest {

    private static final Path PRODUCTION_SOURCE_ROOT =
        Path.of("src/main/java/com/vladislav/training/platform");
    private static final Path RESULT_RECORDING_SERVICE_CONTRACT = Path.of(
        "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingService.java"
    );
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

    private static final List<String> FORBIDDEN_RESULT_RECOVERY_REFERENCES = List.of(
        "recordResult(",
        "ResultRecordingService",
        "completed attempt without result",
        "missing result",
        "recoverResult",
        "resultRecovery"
    );

    private static final List<String> FORBIDDEN_RESULT_MATERIALIZATION_REFERENCES = List.of(
        "ResultEntity",
        "ResultQuestionSnapshotEntity",
        "ResultAnswerOptionSnapshotEntity",
        "ResultRepository",
        "ResultQuestionSnapshotRepository",
        "ResultAnswerOptionSnapshotRepository"
    );

    private static final List<String> FORBIDDEN_CONTRACT_SHAPE_MARKERS = List.of(
        "recover",
        "recovery",
        "recordMissing",
        "recordFromScheduler",
        "recordBySystem",
        "recordWithActor",
        "recordWithTechnicalActor"
    );

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
        "\\b(class|interface|record|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+([\\w\\.]+);");
    private static final Pattern SCHEDULING_RUNTIME_PATTERN = Pattern.compile("(?m)^\\s*@(?:Scheduled|EnableScheduling)\\b");

    @Test
    void maintenanceRecoveryCandidatesDoNotUseInteractiveOnlyResultRecordingPath() throws IOException {
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

            if (importedTypes.contains("com.vladislav.training.platform.result.service.ResultRecordingService")
                && sanitizedSource.contains(".recordResult(")) {
                violations.add(sourceFile + " -> forbidden interactive-only ResultRecordingService.recordResult usage");
            }

            for (String forbiddenReference : FORBIDDEN_RESULT_RECOVERY_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden maintenance contour result recovery reference " + forbiddenReference);
                }
            }

            for (String importedType : importedTypes) {
                if (importedType.startsWith("com.vladislav.training.platform.result.infrastructure.persistence.")
                    || importedType.startsWith("com.vladislav.training.platform.result.repository.")) {
                    violations.add(sourceFile + " -> forbidden direct Result persistence import " + importedType);
                }
            }

            for (String forbiddenReference : FORBIDDEN_RESULT_MATERIALIZATION_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden direct Result materialization reference " + forbiddenReference);
                }
            }

            for (Pattern forbiddenSqlPattern : List.of(
                Pattern.compile("(?i)\\binsert\\s+into\\s+result\\b"),
                Pattern.compile("(?i)\\bupdate\\s+result\\b"),
                Pattern.compile("(?i)\\binsert\\s+into\\s+result_question_snapshot\\b"),
                Pattern.compile("(?i)\\binsert\\s+into\\s+result_answer_option_snapshot\\b")
            )) {
                if (forbiddenSqlPattern.matcher(sanitizedSource).find()) {
                    violations.add(sourceFile + " -> forbidden direct Result SQL mutation " + forbiddenSqlPattern.pattern());
                }
            }

            if (importsResultOwnerPersistence(importedTypes)) {
                for (String forbiddenMutation : List.of(".save(", ".saveAll(", ".saveAndFlush(")) {
                    if (sanitizedSource.contains(forbiddenMutation)) {
                        violations.add(sourceFile + " -> forbidden direct Result repository mutation " + forbiddenMutation);
                    }
                }
            }
        }

        assertThat(violations)
            
            .isEmpty();
    }

    @Test
    void resultRecordingServiceContractShapeStillLacksSchedulerSafeRecoveryApi() throws IOException {
        assertThat(RESULT_RECORDING_SERVICE_CONTRACT).exists();

        String sanitizedContractSource = stripComments(Files.readString(RESULT_RECORDING_SERVICE_CONTRACT));

        assertThat(sanitizedContractSource).contains("Long recordResult(Long testAttemptId);");
        for (String forbiddenMarker : FORBIDDEN_CONTRACT_SHAPE_MARKERS) {
            assertThat(sanitizedContractSource)
                
                .doesNotContain(forbiddenMarker);
        }
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
            && (normalizedPath.contains("/infrastructure/")
                || normalizedPath.contains("/scheduler/")
                || normalizedPath.contains("/recovery/")
                || normalizedPath.contains("/reconciliation/"));
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
            imports.add(matcher.group(1));
        }
        return imports;
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

