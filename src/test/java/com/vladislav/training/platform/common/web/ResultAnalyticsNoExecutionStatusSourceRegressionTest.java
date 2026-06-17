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
 * Проверяет, что {@code ResultAnalyticsNoExecutionStatusSource} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ResultAnalyticsNoExecutionStatusSourceRegressionTest {

    private static final Path PRODUCTION_SOURCE_ROOT =
        Path.of("src/main/java/com/vladislav/training/platform");
    private static final Path RESULT_REBUILD_RUNTIME =
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/service/"
                + "AnalyticsResultRebuildServiceImpl.java"
        );
    private static final Path RESULT_SOURCE_READER =
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/service/"
                + "AnalyticsQuestionAggregateResultSourceReaderImpl.java"
        );
    private static final Path TOPIC_KEY_STRATEGY =
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/service/"
                + "SnapshotBackedAnalyticsTopicKeyStrategy.java"
        );

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
        "\\b(class|interface|record|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+([\\w\\.]+);");
    private static final Pattern ANALYTICS_SCOPE_PATTERN = Pattern.compile("(?i)analytics");
    private static final List<Pattern> SCN11_IMPLEMENTATION_MARKERS = List.of(
        Pattern.compile("(?i)\\brebuild(?:[A-Z][A-Za-z0-9_]*)?\\b"),
        Pattern.compile("(?i)\\brefresh(?:[A-Z][A-Za-z0-9_]*)?\\b"),
        Pattern.compile("(?i)resultbased"),
        Pattern.compile("(?i)result-based"),
        Pattern.compile("(?i)resultsource"),
        Pattern.compile("(?i)result_source"),
        Pattern.compile("(?i)resultaggregate"),
        Pattern.compile("(?i)resultanalytics"),
        Pattern.compile("(?i)aggregatewriter"),
        Pattern.compile("(?i)aggregateupdater"),
        Pattern.compile("(?i)snapshotsource"),
        Pattern.compile("(?i)sourceReader"),
        Pattern.compile("(?i)source-reader")
    );

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
        "com.vladislav.training.platform.assignment.infrastructure.persistence.",
        "com.vladislav.training.platform.assignment.repository.",
        "com.vladislav.training.platform.testing.infrastructure.persistence.",
        "com.vladislav.training.platform.testing.repository."
    );

    private static final List<String> FORBIDDEN_EXACT_IMPORTS = List.of(
        "com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService",
        "com.vladislav.training.platform.testing.service.AttemptStatusRecalculationService"
    );

    private static final List<String> FORBIDDEN_EXECUTION_STATE_REFERENCES = List.of(
        "AssignmentEntity",
        "AssignmentStatus",
        "AssignmentRepository",
        "SpringDataAssignment",
        "assignmentStatus",
        "assignment.status",
        "getStatus()",
        "TestAttemptEntity",
        "TestAttemptStatus",
        "TestAttemptRepository",
        "SpringDataTestAttempt",
        "attemptStatus",
        "testAttemptStatus",
        "testAttempt.status",
        "attempt.status",
        "IN_PROGRESS",
        "COMPLETED",
        "EXPIRED",
        "ABANDONED",
        "OVERDUE",
        "CANCELLED"
    );

    @Test
    void scn11ResultAnalyticsCandidatesDoNotUseExecutionStatusAsHistoricalMetricSource() throws IOException {
        List<Path> productionJavaSources = productionJavaSources();

        assertThat(productionJavaSources).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Path sourceFile : productionJavaSources) {
            String source = Files.readString(sourceFile);
            String sanitizedSource = stripComments(source);

            if (!isScn11AnalyticsCandidate(sourceFile, sanitizedSource)) {
                continue;
            }

            for (String importedType : importedTypes(sanitizedSource)) {
                if (FORBIDDEN_IMPORT_PREFIXES.stream().anyMatch(importedType::startsWith)) {
                    violations.add(sourceFile + " -> forbidden execution-state import " + importedType);
                }
                if (FORBIDDEN_EXACT_IMPORTS.contains(importedType)) {
                    violations.add(sourceFile + " -> forbidden execution-status service import " + importedType);
                }
            }

            for (String forbiddenReference : FORBIDDEN_EXECUTION_STATE_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden execution/current-state reference " + forbiddenReference);
                }
            }
        }

        assertThat(violations)
            
            .isEmpty();
    }

    @Test
    void scn11RuntimeUsesResultSnapshotsTopicSnapshotAndAnalyticsOnlyWriteBoundary() throws IOException {
        String rebuildRuntimeSource = stripComments(Files.readString(RESULT_REBUILD_RUNTIME));
        String sourceReaderSource = stripComments(Files.readString(RESULT_SOURCE_READER));
        String topicKeyStrategySource = stripComments(Files.readString(TOPIC_KEY_STRATEGY));

        assertThat(sourceReaderSource)
            .contains("from result r")
            .contains("join result_question_snapshot rqs")
            .contains("r.user_id_snapshot")
            .contains("rqs.topic_id_snapshot")
            .doesNotContain("assignment.status")
            .doesNotContain("test_attempt.status")
            .doesNotContain("from assignment ")
            .doesNotContain("join assignment ")
            .doesNotContain("from test_attempt ")
            .doesNotContain("join test_attempt ");

        assertThat(topicKeyStrategySource)
            .contains("sourceRow.topicIdSnapshot()")
            .doesNotContain("topicRepository")
            .doesNotContain("questionRepository")
            .doesNotContain("currentFinal")
            .doesNotContain("activeFinal");

        assertThat(rebuildRuntimeSource)
            .contains("replaceUserTopicAggregates")
            .contains("replaceDepartmentTopicAggregates")
            .contains("replaceQuestionAggregates")
            .doesNotContain("saveAssignment")
            .doesNotContain("refreshAssignmentStatusCache")
            .doesNotContain("refreshAttemptStatusCache")
            .doesNotContain("recordResult(")
            .doesNotContain("insert into result")
            .doesNotContain("update result")
            .doesNotContain("update assignment")
            .doesNotContain("update test_attempt")
            .doesNotContain("result_question_snapshot")
            .doesNotContain("result_answer_option_snapshot");
    }

    private List<Path> productionJavaSources() throws IOException {
        try (Stream<Path> paths = Files.walk(PRODUCTION_SOURCE_ROOT)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }
    }

    private boolean isScn11AnalyticsCandidate(Path sourceFile, String sanitizedSource) {
        String normalizedPath = sourceFile.toString().replace('\\', '/');
        String declaredClassName = declaredClassName(sanitizedSource);

        if (!normalizedPath.contains("/analytics/")) {
            return false;
        }

        if (!matchesAnalyticsScope(normalizedPath) && !matchesAnalyticsScope(declaredClassName) && !matchesAnalyticsScope(sanitizedSource)) {
            return false;
        }

        if (!(normalizedPath.contains("result")
            || declaredClassName.toLowerCase().contains("result")
            || sanitizedSource.toLowerCase().contains("result_question_snapshot")
            || sanitizedSource.toLowerCase().contains("result_answer_option_snapshot")
            || sanitizedSource.toLowerCase().contains("result_id"))) {
            return false;
        }

        return matchesImplementationMarker(normalizedPath)
            || matchesImplementationMarker(declaredClassName)
            || matchesImplementationMarker(sanitizedSource);
    }

    private boolean matchesAnalyticsScope(String value) {
        return ANALYTICS_SCOPE_PATTERN.matcher(value).find();
    }

    private boolean matchesImplementationMarker(String value) {
        return SCN11_IMPLEMENTATION_MARKERS.stream().anyMatch(pattern -> pattern.matcher(value).find());
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

    private String stripComments(String source) {
        String withoutBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)//.*$", "");
    }
}
