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
 * Проверяет, что {@code BoundaryAntiDrift} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class BoundaryAntiDriftRegressionTest {

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

    private static final List<String> GENERIC_WAVE_SEVEN_RECOVERY_TERMS = List.of(
        "backfill",
        "replay",
        "recover",
        "recovery",
        "repair",
        "reconstruct"
    );

    private static final List<String> GENERIC_IMPORT_PATCH_TERMS = List.of(
        "ownerPatch",
        "patchOwner",
        "directOwner",
        "repairOwner",
        "rebuildOwner",
        "genericImportRepair"
    );

    private static final List<String> GENERIC_AUDIT_AUTHORITY_TERMS = List.of(
        "recover",
        "recovery",
        "repair",
        "backfill",
        "replay",
        "reconstruct",
        "jobOutcome",
        "schedulerOutcome"
    );

    private static final List<String> CAPABILITY_AUTHORITY_TERMS = List.of(
        "RECOVERY",
        "REPAIR",
        "MAINTENANCE",
        "REBUILD",
        "RECONCILIATION"
    );

    private static final List<String> WAVE_SIX_CAPABILITY_FAMILIES = List.of(
        "NOTIFICATION",
        "IMPORT",
        "AUDIT"
    );

    private static final Map<String, Pattern> FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS = new LinkedHashMap<>();

    static {
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("repair", Pattern.compile("(?i)repair"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("rebuild", Pattern.compile("(?i)rebuild"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("recovery", Pattern.compile("(?i)recovery"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("recover", Pattern.compile("(?i)recover"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("reconcile", Pattern.compile("(?i)reconcile"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("reconciliation", Pattern.compile("(?i)reconciliation"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("maintenance", Pattern.compile("(?i)maintenance"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("fix", Pattern.compile("(?i)(?:/fix\\b|\\bfix\\b|\\bfix[A-Z]\\w*)"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("backfill", Pattern.compile("(?i)backfill"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("replay", Pattern.compile("(?i)replay"));
        FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.put("reconstruct", Pattern.compile("(?i)reconstruct"));
    }

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
        "\\b(class|interface|record|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+([\\w\\.]+);");
    private static final Pattern SCHEDULING_RUNTIME_PATTERN = Pattern.compile("(?m)^\\s*@(?:Scheduled|EnableScheduling)\\b");

    @Test
    void maintenanceBoundaryDoesNotAbsorbAdministrativeOperationalSurfaces() throws IOException {
        List<Path> productionJavaSources = productionJavaSources();

        assertThat(productionJavaSources).isNotEmpty();

        List<String> candidateBoundaryViolations = new ArrayList<>();
        List<String> controllerBoundaryViolations = new ArrayList<>();

        for (Path sourceFile : productionJavaSources) {
            String source = Files.readString(sourceFile);
            String sanitizedSource = stripComments(source);

            if (isMaintenanceCandidate(sourceFile, sanitizedSource)) {
                if (!isAdministrativeModuleFile(sourceFile)) {
                    collectCandidateViolations(sourceFile, sanitizedSource, candidateBoundaryViolations);
                }
            }

            if (isAdministrativeControllerInScope(sourceFile, sanitizedSource)) {
                for (String mappingSnippet : extractMappingSnippets(sanitizedSource)) {
                    for (Map.Entry<String, Pattern> forbiddenSemantic : FORBIDDEN_GENERIC_CONTROLLER_SEMANTICS.entrySet()) {
                        if (forbiddenSemantic.getValue().matcher(mappingSnippet).find()) {
                            controllerBoundaryViolations.add(
                                sourceFile + " -> " + forbiddenSemantic.getKey() + " in " + normalizeSnippet(mappingSnippet)
                            );
                        }
                    }
                }
            }
        }

        assertThat(candidateBoundaryViolations)
            
            .isEmpty();
        assertThat(controllerBoundaryViolations)
            
            .isEmpty();
    }

    private void collectCandidateViolations(Path sourceFile, String sanitizedSource, List<String> violations) {
        List<String> importedTypes = importedTypes(sanitizedSource);

        for (String importedType : importedTypes) {
            if (importedType.startsWith("com.vladislav.training.platform.notification.controller.")
                || importedType.startsWith("com.vladislav.training.platform.integration.controller.")
                || importedType.startsWith("com.vladislav.training.platform.audit.controller.")
                || importedType.startsWith("com.vladislav.training.platform.audit.infrastructure.persistence.")
                || importedType.startsWith("com.vladislav.training.platform.audit.repository.")) {
                violations.add(sourceFile + " -> forbidden administrative contour surface import " + importedType);
            }
        }

        if (sanitizedSource.contains("NotificationRuleService")
            && containsAny(sanitizedSource, GENERIC_WAVE_SEVEN_RECOVERY_TERMS)) {
            violations.add(sourceFile + " -> forbidden Notification generic recovery/backfill authority");
        }

        if (sanitizedSource.contains("ImportCommandService")
            && containsAny(sanitizedSource, GENERIC_IMPORT_PATCH_TERMS)) {
            violations.add(sourceFile + " -> forbidden Import generic owner-domain patch channel");
        }

        if (containsAny(sanitizedSource, List.of("AuditService", "PersistentAuditService", "AuditEventRepository", "audit_event"))
            && containsAny(sanitizedSource, GENERIC_AUDIT_AUTHORITY_TERMS)) {
            violations.add(sourceFile + " -> forbidden Audit recovery/repair authority");
        }

        if (containsAny(sanitizedSource, List.of("CapabilityOperationCode", "CapabilityOperationCodes"))
            && containsAny(sanitizedSource, WAVE_SIX_CAPABILITY_FAMILIES)
            && containsAny(sanitizedSource, CAPABILITY_AUTHORITY_TERMS)) {
            violations.add(sourceFile + " -> forbidden administrative contour capability vocabulary as generic maintenance contour maintenance permission");
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

    private boolean isAdministrativeModuleFile(Path sourceFile) {
        String normalizedPath = normalize(sourceFile);
        return normalizedPath.contains("/notification/")
            || normalizedPath.contains("/integration/")
            || normalizedPath.contains("/audit/");
    }

    private boolean isAdministrativeControllerInScope(Path sourceFile, String sanitizedSource) {
        String normalizedPath = normalize(sourceFile);
        if (!normalizedPath.endsWith("controller.java")) {
            return false;
        }

        if (normalizedPath.contains("/notification/")
            || normalizedPath.contains("/integration/")
            || normalizedPath.contains("/audit/")) {
            return true;
        }

        return normalizedPath.contains("/common/web/")
            && containsAny(sanitizedSource, List.of("notification", "import", "audit"));
    }

    private boolean containsPrimaryMarker(String value) {
        return PRIMARY_WAVE_SEVEN_SCOPE_MARKERS.stream().anyMatch(value::contains);
    }

    private boolean containsSecondaryMarker(String value) {
        return SECONDARY_WAVE_SEVEN_SCOPE_MARKERS.stream().anyMatch(value::contains);
    }

    private boolean containsAdministrativeRecoveryAuthorityReference(String sanitizedSource) {
        return containsAny(sanitizedSource, List.of(
            "NotificationRuleService",
            "ImportCommandService",
            "AuditService",
            "PersistentAuditService",
            "AuditEventRepository",
            "audit_event"
        )) && containsAny(sanitizedSource, List.of(
            "recover",
            "recovery",
            "repair",
            "backfill",
            "replay",
            "reconstruct",
            "jobOutcome",
            "schedulerOutcome"
        ));
    }

    private boolean containsAny(String source, List<String> terms) {
        return terms.stream().anyMatch(source::contains);
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

    private List<String> extractMappingSnippets(String sanitizedSource) {
        List<String> snippets = new ArrayList<>();
        List<String> lines = sanitizedSource.lines().toList();

        for (int index = 0; index < lines.size(); index++) {
            String trimmedLine = lines.get(index).trim();
            if (!isMappingAnnotation(trimmedLine)) {
                continue;
            }

            StringBuilder annotation = new StringBuilder(trimmedLine);
            int parenthesisBalance = parenthesisBalance(trimmedLine);
            int cursor = index;
            while (parenthesisBalance > 0 && cursor + 1 < lines.size()) {
                cursor++;
                String continuationLine = lines.get(cursor).trim();
                annotation.append('\n').append(continuationLine);
                parenthesisBalance += parenthesisBalance(continuationLine);
            }

            String signatureLine = "";
            while (cursor + 1 < lines.size()) {
                cursor++;
                String candidate = lines.get(cursor).trim();
                if (candidate.isEmpty() || candidate.startsWith("@")) {
                    continue;
                }
                signatureLine = candidate;
                break;
            }

            snippets.add(annotation + "\n" + signatureLine);
            index = cursor;
        }

        return snippets;
    }

    private boolean isMappingAnnotation(String trimmedLine) {
        return trimmedLine.startsWith("@RequestMapping")
            || trimmedLine.startsWith("@GetMapping")
            || trimmedLine.startsWith("@PostMapping")
            || trimmedLine.startsWith("@PutMapping")
            || trimmedLine.startsWith("@DeleteMapping")
            || trimmedLine.startsWith("@PatchMapping");
    }

    private int parenthesisBalance(String value) {
        int balance = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '(') {
                balance++;
            } else if (current == ')') {
                balance--;
            }
        }
        return balance;
    }

    private String normalize(Path sourceFile) {
        return sourceFile.toString().replace('\\', '/').toLowerCase();
    }

    private String stripComments(String source) {
        String withoutBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)//.*$", "");
    }

    private String normalizeSnippet(String snippet) {
        return snippet.replaceAll("\\s+", " ").trim();
    }
}

