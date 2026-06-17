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
 * Проверяет, что {@code AnalyticsNoLiveContentFallback} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AnalyticsNoLiveContentFallbackRegressionTest {

    private static final Path PRODUCTION_SOURCE_ROOT =
        Path.of("src/main/java/com/vladislav/training/platform");

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
        Pattern.compile("(?i)aggregatewriter"),
        Pattern.compile("(?i)aggregateupdater"),
        Pattern.compile("(?i)snapshotsource"),
        Pattern.compile("(?i)sourceReader"),
        Pattern.compile("(?i)source-reader")
    );

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
        "com.vladislav.training.platform.content.infrastructure.persistence.",
        "com.vladislav.training.platform.content.repository.",
        "com.vladislav.training.platform.content.service.",
        "com.vladislav.training.platform.userorg.infrastructure.persistence.",
        "com.vladislav.training.platform.userorg.repository.",
        "com.vladislav.training.platform.userorg.service."
    );

    private static final List<String> FORBIDDEN_LIVE_STATE_REFERENCES = List.of(
        "QuestionEntity",
        "AnswerOptionEntity",
        "TestEntity",
        "TopicEntity",
        "CourseEntity",
        "OrganizationalUnitEntity",
        "UserOrganizationAssignmentEntity",
        "UserRoleAssignmentEntity",
        "isActiveFinalForTopic",
        "activeFinal",
        "currentFinal",
        "currentOrg",
        "currentOrganization",
        "topicRepository",
        "questionRepository",
        "contentRepository",
        "organizationalUnitRepository",
        "userOrganizationAssignmentRepository"
    );

    @Test
    void scn11AnalyticsRebuildCandidatesDoNotFallbackToLiveContentOrCurrentOrgTruth() throws IOException {
        List<Path> productionJavaSources = productionJavaSources();

        assertThat(productionJavaSources).isNotEmpty();

        List<String> liveFallbackViolations = new ArrayList<>();
        List<String> topicFallbackViolations = new ArrayList<>();

        for (Path sourceFile : productionJavaSources) {
            String source = Files.readString(sourceFile);
            String sanitizedSource = stripComments(source);

            if (!isScn11AnalyticsCandidate(sourceFile, sanitizedSource)) {
                continue;
            }

            for (String importedType : importedTypes(sanitizedSource)) {
                if (FORBIDDEN_IMPORT_PREFIXES.stream().anyMatch(importedType::startsWith)) {
                    liveFallbackViolations.add(sourceFile + " -> forbidden live-source import " + importedType);
                }
            }

            for (String forbiddenReference : FORBIDDEN_LIVE_STATE_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    liveFallbackViolations.add(sourceFile + " -> forbidden live-state reference " + forbiddenReference);
                }
            }

            if (sanitizedSource.contains("question_original_id")) {
                for (String forbiddenTopicFallback : List.of(
                    "QuestionEntity",
                    "TopicEntity",
                    "topicRepository",
                    "questionRepository",
                    "isActiveFinalForTopic",
                    "activeFinal"
                )) {
                    if (sanitizedSource.contains(forbiddenTopicFallback)) {
                        topicFallbackViolations.add(
                            sourceFile + " -> question_original_id with live topic/content fallback " + forbiddenTopicFallback
                        );
                    }
                }
            }
        }

        assertThat(liveFallbackViolations)
            
            .isEmpty();
        assertThat(topicFallbackViolations)
            
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

    private boolean isScn11AnalyticsCandidate(Path sourceFile, String sanitizedSource) {
        String normalizedPath = sourceFile.toString().replace('\\', '/');
        String declaredClassName = declaredClassName(sanitizedSource);

        if (!matchesAnalyticsScope(normalizedPath) && !matchesAnalyticsScope(declaredClassName) && !matchesAnalyticsScope(sanitizedSource)) {
            return false;
        }

        String normalizedSource = sanitizedSource.toLowerCase();
        if (!(normalizedPath.contains("result")
            || declaredClassName.toLowerCase().contains("result")
            || normalizedSource.contains("result_question_snapshot")
            || normalizedSource.contains("result_answer_option_snapshot")
            || normalizedSource.contains("result_id"))) {
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
