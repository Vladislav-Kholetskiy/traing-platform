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
 * Проверяет, что {@code AuditNotJobOutcomeStore} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AuditNotJobOutcomeStoreRegressionTest {

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

    private static final List<String> FORBIDDEN_AUDIT_PERSISTENCE_REFERENCES = List.of(
        "AuditEventEntity",
        "AuditEventRepository",
        "SpringDataAuditEventRepository",
        "JpaAuditEventRepositoryAdapter",
        "PersistentAuditService",
        "AuditMapper",
        "auditEventRepository",
        "audit_event"
    );

    private static final List<String> FORBIDDEN_AUDIT_RECOVERY_REFERENCES = List.of(
        "missingAudit",
        "auditRecovery",
        "recoverAudit",
        "reconstructAudit",
        "replayAudit",
        "backfillAudit",
        "auditBackfill",
        "auditReplay",
        "historicalAudit",
        "restoreAudit",
        "repairAudit",
        "jobOutcome",
        "jobStatus",
        "recoveryOutcome",
        "schedulerOutcome",
        "reconciliationOutcome"
    );

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
        "\\b(class|interface|record|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+([\\w\\.]+);");
    private static final Pattern SCHEDULING_RUNTIME_PATTERN = Pattern.compile("(?m)^\\s*@(?:Scheduled|EnableScheduling)\\b");

    @Test
    void maintenanceSchedulerRecoveryCandidatesDoNotUseAuditAsJobOutcomeStoreOrRecoveryTarget() throws IOException {
        List<Path> productionJavaSources = productionJavaSources();

        assertThat(productionJavaSources).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Path sourceFile : productionJavaSources) {
            String source = Files.readString(sourceFile);
            String sanitizedSource = stripComments(source);

            if (!isMaintenanceCandidate(sourceFile, sanitizedSource)) {
                continue;
            }

            if (isAuditModuleFile(sourceFile)) {
                continue;
            }

            List<String> importedTypes = importedTypes(sanitizedSource);

            for (String importedType : importedTypes) {
                if (importedType.startsWith("com.vladislav.training.platform.audit.infrastructure.persistence.")
                    || importedType.startsWith("com.vladislav.training.platform.audit.repository.")) {
                    violations.add(sourceFile + " -> forbidden audit persistence import " + importedType);
                }
            }

            for (String forbiddenReference : FORBIDDEN_AUDIT_PERSISTENCE_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden audit job-store reference " + forbiddenReference);
                }
            }

            for (String forbiddenReference : FORBIDDEN_AUDIT_RECOVERY_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden audit recovery-target reference " + forbiddenReference);
                }
            }

            for (Pattern forbiddenSqlPattern : List.of(
                Pattern.compile("(?i)\\binsert\\s+into\\s+audit_event\\b"),
                Pattern.compile("(?i)\\bupdate\\s+audit_event\\b")
            )) {
                if (forbiddenSqlPattern.matcher(sanitizedSource).find()) {
                    violations.add(sourceFile + " -> forbidden direct audit SQL mutation " + forbiddenSqlPattern.pattern());
                }
            }

            if (importsAuditPersistence(importedTypes)) {
                for (String forbiddenMutation : List.of(".save(", ".saveAll(", ".saveAndFlush(")) {
                    if (sanitizedSource.contains(forbiddenMutation)) {
                        violations.add(sourceFile + " -> forbidden audit persistence mutation shortcut " + forbiddenMutation);
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
            && (normalizedPath.contains("/infrastructure/")
                || normalizedPath.contains("/scheduler/")
                || normalizedPath.contains("/recovery/")
                || normalizedPath.contains("/reconciliation/"));
    }

    private boolean isAuditModuleFile(Path sourceFile) {
        return normalize(sourceFile).contains("/audit/");
    }

    private boolean containsPrimaryMarker(String value) {
        return PRIMARY_WAVE_SEVEN_SCOPE_MARKERS.stream().anyMatch(value::contains);
    }

    private boolean containsSecondaryMarker(String value) {
        return SECONDARY_WAVE_SEVEN_SCOPE_MARKERS.stream().anyMatch(value::contains);
    }

    private boolean containsAnyForbiddenAuditReference(String sanitizedSource) {
        return FORBIDDEN_AUDIT_PERSISTENCE_REFERENCES.stream().anyMatch(sanitizedSource::contains)
            || FORBIDDEN_AUDIT_RECOVERY_REFERENCES.stream().anyMatch(sanitizedSource::contains);
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

    private boolean importsAuditPersistence(List<String> importedTypes) {
        return importedTypes.stream().anyMatch(importedType ->
            importedType.startsWith("com.vladislav.training.platform.audit.infrastructure.persistence.")
                || importedType.startsWith("com.vladislav.training.platform.audit.repository.")
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

