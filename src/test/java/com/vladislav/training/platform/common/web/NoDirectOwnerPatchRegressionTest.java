package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Проверяет, что {@code NoDirectOwnerPatch} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class NoDirectOwnerPatchRegressionTest {

    private static final Path PRODUCTION_SOURCE_ROOT =
        Path.of("src/main/java/com/vladislav/training/platform");

    private static final List<String> WAVE_SEVEN_SCOPE_MARKERS = List.of(
        "scheduler",
        "reconciliation",
        "recovery",
        "rebuild",
        "repair",
        "maintenance",
        "backfill",
        "replay"
    );

    private static final List<String> KNOWN_ALLOWED_CONTRACT_REFERENCES = List.of(
        "AssignmentStatusRecalculationService",
        "AttemptStatusRecalculationService",
        "ResultRecordingService",
        "AnalyticsRebuildService",
        "AnalyticsRefreshService"
    );

    private static final Map<String, Pattern> OWNER_TABLE_SQL_PATTERNS = new LinkedHashMap<>();

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
        "\\b(class|interface|record|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+([\\w\\.]+);");
    private static final Pattern OWNER_ENTITY_IMPORT_PATTERN = Pattern.compile(
        "^com\\.vladislav\\.training\\.platform\\.(assignment|testing|result|content|userorg|audit)"
            + "\\.infrastructure\\.persistence\\.[A-Za-z0-9]+Entity$"
    );
    private static final Pattern OWNER_SPRING_DATA_REPOSITORY_IMPORT_PATTERN = Pattern.compile(
        "^com\\.vladislav\\.training\\.platform\\.(assignment|testing|result|content|userorg|audit)"
            + "\\.infrastructure\\.persistence\\.SpringData[A-Za-z0-9]*Repository$"
    );

    static {
        for (String ownerTable : List.of(
            "assignment",
            "assignment_campaign",
            "assignment_campaign_recipient_snapshot",
            "test_attempt",
            "user_answer",
            "user_answer_item",
            "result",
            "result_question_snapshot",
            "result_answer_option_snapshot",
            "course",
            "topic",
            "material",
            "question",
            "answer_option",
            "app_user",
            "organizational_unit",
            "audit_event"
        )) {
            OWNER_TABLE_SQL_PATTERNS.put(
                ownerTable,
                Pattern.compile(
                    "(?i)\\b(?:update\\s+"
                        + Pattern.quote(ownerTable)
                        + "|delete\\s+from\\s+"
                        + Pattern.quote(ownerTable)
                        + "|insert\\s+into\\s+"
                        + Pattern.quote(ownerTable)
                        + ")\\b"
                )
            );
        }
    }

    @Test
    void maintenanceCandidateFilesDoNotOpenDirectOwnerPatchShortcuts() throws IOException {
        List<Path> productionJavaSources = productionJavaSources();

        assertThat(productionJavaSources).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Path sourceFile : productionJavaSources) {
            String source = Files.readString(sourceFile);
            String sanitizedSource = stripComments(source);

            if (!isMaintenanceCandidate(sourceFile, sanitizedSource)) {
                continue;
            }

            for (Map.Entry<String, Pattern> entry : OWNER_TABLE_SQL_PATTERNS.entrySet()) {
                if (entry.getValue().matcher(sanitizedSource).find()) {
                    violations.add(sourceFile + " -> direct SQL owner-table patch for " + entry.getKey());
                }
            }

            List<String> importedTypes = importedTypes(sanitizedSource);
            for (String importedType : importedTypes) {
                if (OWNER_ENTITY_IMPORT_PATTERN.matcher(importedType).matches()) {
                    violations.add(sourceFile + " -> owner JPA entity import " + importedType);
                }
                if (OWNER_SPRING_DATA_REPOSITORY_IMPORT_PATTERN.matcher(importedType).matches()) {
                    violations.add(sourceFile + " -> owner Spring Data repository import " + importedType);
                }
            }

            if (importsOwnerSpringDataRepository(importedTypes)) {
                for (String forbiddenMutation : List.of(".save(", ".saveAll(", ".saveAndFlush(", ".delete(", ".deleteAll(", ".flush(")) {
                    if (sanitizedSource.contains(forbiddenMutation)) {
                        violations.add(sourceFile + " -> repository mutation shortcut " + forbiddenMutation);
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
        String normalizedPath = sourceFile.toString().replace('\\', '/').toLowerCase();
        String className = declaredClassName(sanitizedSource).toLowerCase();
        String normalizedSource = sanitizedSource.toLowerCase();

        return containsAnyMarker(normalizedPath)
            || containsAnyMarker(className)
            || containsAnyMarker(normalizedSource);
    }

    private boolean containsAnyMarker(String value) {
        return WAVE_SEVEN_SCOPE_MARKERS.stream().anyMatch(value::contains);
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
            if (KNOWN_ALLOWED_CONTRACT_REFERENCES.stream().anyMatch(importedType::endsWith)) {
                continue;
            }
            imports.add(importedType);
        }
        return imports;
    }

    private boolean importsOwnerSpringDataRepository(List<String> importedTypes) {
        return importedTypes.stream()
            .anyMatch(importedType -> OWNER_SPRING_DATA_REPOSITORY_IMPORT_PATTERN.matcher(importedType).matches());
    }

    private String stripComments(String source) {
        String withoutBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)//.*$", "");
    }
}
