package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SchedulerAndPublicApiAntiDrift} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SchedulerAndPublicApiAntiDriftRegressionTest {

    private static final Path PRODUCTION_SOURCE_ROOT =
        Path.of("src/main/java/com/vladislav/training/platform");
    private static final Path ASSIGNMENT_SCHEDULER_CONFIGURATION =
        Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/scheduler/"
                + "AssignmentSchedulerConfiguration.java"
        );
    private static final Path ASSIGNMENT_RECONCILIATION_SCHEDULER =
        Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/scheduler/"
                + "AssignmentStatusRecalculationScheduler.java"
        );
    private static final Path WAVE7_ANALYTICS_SCHEDULER_CONFIGURATION =
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/scheduler/"
                + "AnalyticsSchedulerConfiguration.java"
        );
    private static final Path WAVE7_ANALYTICS_RECONCILIATION_SCHEDULER =
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/scheduler/"
                + "AnalyticsRebuildScheduler.java"
        );
    private static final Path ANALYTICS_ADMIN_REBUILD_CONTROLLER =
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/"
                + "AnalyticsAdminRebuildController.java"
        );

    private static final List<String> FORBIDDEN_RUNTIME_SCHEDULING_ANNOTATIONS = List.of(
        "@Scheduled",
        "@EnableScheduling"
    );

    private static final Map<String, Pattern> FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS = new LinkedHashMap<>();

    static {
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("repair", Pattern.compile("(?i)repair"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("rebuild", Pattern.compile("(?i)rebuild"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("recovery", Pattern.compile("(?i)recovery"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("recover", Pattern.compile("(?i)recover"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("reconcile", Pattern.compile("(?i)reconcile"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("reconciliation", Pattern.compile("(?i)reconciliation"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("maintenance", Pattern.compile("(?i)maintenance"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("fix", Pattern.compile("(?i)(?:/fix\\b|\\bfix\\b|\\bfix[A-Z]\\w*)"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("backfill", Pattern.compile("(?i)backfill"));
        FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.put("replay", Pattern.compile("(?i)replay"));
    }

    @Test
    void productionBaselineKeepsOnlyExplicitMaintenanceResultRebuildMaintenanceRouteWhileSchedulingRuntimeStaysBounded() throws IOException {
        List<Path> productionJavaSources = productionJavaSources();

        assertThat(productionJavaSources).isNotEmpty();

        List<String> runtimeSchedulingViolations = new ArrayList<>();
        List<String> allowlistedSchedulingFiles = new ArrayList<>();
        List<String> publicMaintenanceApiViolations = new ArrayList<>();
        List<String> allowedMaintenanceRoutes = new ArrayList<>();
        List<String> readControllerRebuildDependencyViolations = new ArrayList<>();

        for (Path sourceFile : productionJavaSources) {
            String sanitizedSource = stripComments(Files.readString(sourceFile));

            boolean allowlistedSchedulingFile = sourceFile.equals(WAVE7_ANALYTICS_SCHEDULER_CONFIGURATION)
                || sourceFile.equals(WAVE7_ANALYTICS_RECONCILIATION_SCHEDULER)
                || sourceFile.equals(ASSIGNMENT_SCHEDULER_CONFIGURATION)
                || sourceFile.equals(ASSIGNMENT_RECONCILIATION_SCHEDULER);

            for (String forbiddenAnnotation : FORBIDDEN_RUNTIME_SCHEDULING_ANNOTATIONS) {
                if (sanitizedSource.contains(forbiddenAnnotation)) {
                    if (allowlistedSchedulingFile) {
                        allowlistedSchedulingFiles.add(sourceFile + " -> " + forbiddenAnnotation);
                    } else {
                        runtimeSchedulingViolations.add(sourceFile + " -> " + forbiddenAnnotation);
                    }
                }
            }

            if (!sourceFile.getFileName().toString().endsWith("Controller.java")) {
                continue;
            }

            boolean isAllowedMaintenanceController = sourceFile.equals(ANALYTICS_ADMIN_REBUILD_CONTROLLER);

            if (isAllowedMaintenanceController) {
                continue;
            }

            for (String mappingSnippet : extractMappingSnippets(sanitizedSource)) {
                String normalizedSnippet = normalizeSnippet(mappingSnippet);

                for (Map.Entry<String, Pattern> forbiddenSemantic : FORBIDDEN_GENERIC_MAINTENANCE_SEMANTICS.entrySet()) {
                    if (forbiddenSemantic.getValue().matcher(mappingSnippet).find()) {
                        publicMaintenanceApiViolations.add(
                            sourceFile + " -> " + forbiddenSemantic.getKey() + " in " + normalizedSnippet
                        );
                    }
                }
            }

            if (!isAllowedMaintenanceController
                && (sanitizedSource.contains("AnalyticsAdminRebuildService")
                    || sanitizedSource.contains("AnalyticsRebuildService")
                    || sanitizedSource.contains("AnalyticsRefreshService"))) {
                readControllerRebuildDependencyViolations.add(sourceFile.toString());
            }
        }

        String allowedControllerSource = stripComments(Files.readString(ANALYTICS_ADMIN_REBUILD_CONTROLLER));
        for (String mappingSnippet : extractMappingSnippets(allowedControllerSource)) {
            String normalizedSnippet = normalizeSnippet(mappingSnippet);
            if (normalizedSnippet.contains("/result-rebuild")) {
                allowedMaintenanceRoutes.add(ANALYTICS_ADMIN_REBUILD_CONTROLLER + " -> " + normalizedSnippet);
            } else if (normalizedSnippet.contains("/api/v1/admin/analytics")) {
                continue;
            } else {
                publicMaintenanceApiViolations.add(
                    ANALYTICS_ADMIN_REBUILD_CONTROLLER + " -> unexpected maintenance mapping " + normalizedSnippet
                );
            }
        }

        assertThat(runtimeSchedulingViolations)
            
            .isEmpty();
        assertThat(allowlistedSchedulingFiles)
            .containsExactlyInAnyOrder(
                ASSIGNMENT_SCHEDULER_CONFIGURATION + " -> @EnableScheduling",
                ASSIGNMENT_RECONCILIATION_SCHEDULER + " -> @Scheduled",
                WAVE7_ANALYTICS_SCHEDULER_CONFIGURATION + " -> @EnableScheduling",
                WAVE7_ANALYTICS_RECONCILIATION_SCHEDULER + " -> @Scheduled"
            );
        assertThat(allowedMaintenanceRoutes)
            
            .hasSize(1);
        assertThat(publicMaintenanceApiViolations)
            
            .isEmpty();
        assertThat(readControllerRebuildDependencyViolations)
            
            .isEmpty();

        assertThat(allowedControllerSource)
            .contains("@RequestMapping(\"/api/v1/admin/analytics\")")
            .contains("@PostMapping(\"/result-rebuild\")")
            .doesNotContain("@GetMapping(\"/result-rebuild\")")
            .doesNotContain("repair")
            .doesNotContain("recovery")
            .doesNotContain("reconcile")
            .doesNotContain("backfill")
            .doesNotContain("replay")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AttemptStatusRecalculationService")
            .doesNotContain("ResultRecordingService");

        String schedulerSource = stripComments(Files.readString(WAVE7_ANALYTICS_RECONCILIATION_SCHEDULER));
        assertThat(schedulerSource)
            .contains("AnalyticsResultRebuildService")
            .contains("AnalyticsRefreshService")
            .contains("AnalyticsCampaignAggregateSourceReader")
            .contains("@Scheduled")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AttemptStatusRecalculationService")
            .doesNotContain("NotificationRuleService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("auditEventRepository")
            .doesNotContain("/repair")
            .doesNotContain("/recovery");
    }

    @Test
    void assignmentSchedulerStaysBoundedAndUsesOnlyOwnerLocalRecalculationContract() throws IOException {
        String schedulerSource = stripComments(Files.readString(ASSIGNMENT_RECONCILIATION_SCHEDULER));

        assertThat(schedulerSource)
            .contains("AssignmentStatusRecalculationBatchService")
            .contains("@Scheduled")
            .doesNotContain("AssignmentRepository")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("dispatchPendingNotifications")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("auditEventRepository");
    }

    private List<Path> productionJavaSources() throws IOException {
        try (Stream<Path> paths = Files.walk(PRODUCTION_SOURCE_ROOT)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }
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

    private String stripComments(String source) {
        String withoutBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)//.*$", "");
    }

    private String normalizeSnippet(String snippet) {
        return snippet.replaceAll("\\s+", " ").trim();
    }
}


