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
 * Проверяет, что {@code CampaignAggregateNoResultCoverage} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class CampaignAggregateNoResultCoverageRegressionTest {

    private static final Path PRODUCTION_SOURCE_ROOT =
        Path.of("src/main/java/com/vladislav/training/platform");

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
        "\\b(class|interface|record|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+([\\w\\.]+);");
    private static final Pattern ANALYTICS_SCOPE_PATTERN = Pattern.compile("(?i)analytics");
    private static final List<Pattern> SCN12_CAMPAIGN_MARKERS = List.of(
        Pattern.compile("(?i)campaignaggregate"),
        Pattern.compile("(?i)campaign-aggregate"),
        Pattern.compile("(?i)analyticscampaign"),
        Pattern.compile("(?i)analyticsCampaign"),
        Pattern.compile("(?i)campaignanalytics"),
        Pattern.compile("(?i)campaign")
    );

    private static final List<Pattern> SCN12_RUNTIME_MARKERS = List.of(
        Pattern.compile("(?i)campaignrebuild"),
        Pattern.compile("(?i)campaignrefresh"),
        Pattern.compile("(?i)campaignsource"),
        Pattern.compile("(?i)\\brebuild(?:[A-Z][A-Za-z0-9_]*)?\\b"),
        Pattern.compile("(?i)\\brefresh(?:[A-Z][A-Za-z0-9_]*)?\\b"),
        Pattern.compile("(?i)aggregatewriter"),
        Pattern.compile("(?i)aggregateupdater"),
        Pattern.compile("(?i)sourceReader"),
        Pattern.compile("(?i)source-reader")
    );

    private static final List<String> FORBIDDEN_RESULT_IMPORT_PREFIXES = List.of(
        "com.vladislav.training.platform.result.infrastructure.persistence.",
        "com.vladislav.training.platform.result.repository.",
        "com.vladislav.training.platform.result.service."
    );

    private static final List<String> FORBIDDEN_RESULT_REFERENCES = List.of(
        "ResultEntity",
        "ResultRepository",
        "ResultQuestionSnapshotEntity",
        "ResultAnswerOptionSnapshotEntity",
        "ResultQuestionSnapshotRepository",
        "ResultAnswerOptionSnapshotRepository",
        "ResultRecordingService",
        "resultRepository",
        "result_question_snapshot",
        "result_answer_option_snapshot",
        "user_id_snapshot",
        "question_original_id",
        "completed_at",
        "passed",
        "score"
    );

    private static final List<String> FORBIDDEN_LIVE_ORG_IMPORT_PREFIXES = List.of(
        "com.vladislav.training.platform.userorg.infrastructure.persistence.",
        "com.vladislav.training.platform.userorg.repository.",
        "com.vladislav.training.platform.userorg.service."
    );

    private static final List<String> FORBIDDEN_LIVE_ORG_REFERENCES = List.of(
        "OrganizationalUnitEntity",
        "UserOrganizationAssignmentEntity",
        "UserRoleAssignmentEntity",
        "organizationalUnitRepository",
        "userOrganizationAssignmentRepository",
        "currentOrg",
        "currentOrganization",
        "orgTree",
        "subtree"
    );

    private static final List<String> FORBIDDEN_ASSIGNMENT_PATCH_REFERENCES = List.of(
        "setStatus(",
        ".status(",
        "assignment.status",
        "AssignmentStatusRecalculationService"
    );

    @Test
    void scn12CampaignAggregateCandidatesDoNotUseResultCoverageLiveOrgFallbackOrDirectStatusPatch() throws IOException {
        List<Path> productionJavaSources = productionJavaSources();

        assertThat(productionJavaSources).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Path sourceFile : productionJavaSources) {
            String source = Files.readString(sourceFile);
            String sanitizedSource = stripComments(source);

            if (!isScn12CampaignCandidate(sourceFile, sanitizedSource)) {
                continue;
            }

            List<String> importedTypes = importedTypes(sanitizedSource);

            for (String importedType : importedTypes) {
                if (FORBIDDEN_RESULT_IMPORT_PREFIXES.stream().anyMatch(importedType::startsWith)) {
                    violations.add(sourceFile + " -> forbidden result-as-coverage import " + importedType);
                }
                if (FORBIDDEN_LIVE_ORG_IMPORT_PREFIXES.stream().anyMatch(importedType::startsWith)) {
                    violations.add(sourceFile + " -> forbidden live-org import " + importedType);
                }
            }

            for (String forbiddenReference : FORBIDDEN_RESULT_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden result-as-coverage reference " + forbiddenReference);
                }
            }

            for (String forbiddenReference : FORBIDDEN_LIVE_ORG_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden live-org recipient rebuild reference " + forbiddenReference);
                }
            }

            if (Pattern.compile("(?i)\\bupdate\\s+assignment\\b").matcher(sanitizedSource).find()) {
                violations.add(sourceFile + " -> forbidden direct SQL patch update assignment");
            }

            for (String forbiddenReference : FORBIDDEN_ASSIGNMENT_PATCH_REFERENCES) {
                if (sanitizedSource.contains(forbiddenReference)) {
                    violations.add(sourceFile + " -> forbidden direct assignment patch reference " + forbiddenReference);
                }
            }

            if (importsOwnerAssignmentRepository(importedTypes)) {
                for (String forbiddenMutation : List.of(".save(", ".saveAll(", ".saveAndFlush(")) {
                    if (sanitizedSource.contains(forbiddenMutation)) {
                        violations.add(sourceFile + " -> forbidden assignment-owner mutation shortcut " + forbiddenMutation);
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

    private boolean isScn12CampaignCandidate(Path sourceFile, String sanitizedSource) {
        String normalizedPath = sourceFile.toString().replace('\\', '/');
        String declaredClassName = declaredClassName(sanitizedSource);

        if (!matchesAnalyticsScope(normalizedPath) && !matchesAnalyticsScope(declaredClassName) && !matchesAnalyticsScope(sanitizedSource)) {
            return false;
        }

        boolean campaignScopeMatched = matchesCampaignMarker(normalizedPath)
            || matchesCampaignMarker(declaredClassName)
            || matchesCampaignMarker(sanitizedSource);

        if (!campaignScopeMatched) {
            return false;
        }

        return matchesRuntimeMarker(normalizedPath) || matchesRuntimeMarker(declaredClassName);
    }

    private boolean matchesAnalyticsScope(String value) {
        return ANALYTICS_SCOPE_PATTERN.matcher(value).find();
    }

    private boolean matchesCampaignMarker(String value) {
        return SCN12_CAMPAIGN_MARKERS.stream().anyMatch(pattern -> pattern.matcher(value).find());
    }

    private boolean matchesRuntimeMarker(String value) {
        return SCN12_RUNTIME_MARKERS.stream().anyMatch(pattern -> pattern.matcher(value).find());
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

    private boolean importsOwnerAssignmentRepository(List<String> importedTypes) {
        return importedTypes.stream().anyMatch(importedType ->
            importedType.equals("com.vladislav.training.platform.assignment.repository.AssignmentRepository")
                || importedType.equals("com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository")
                || importedType.equals(
                    "com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository"
                )
        );
    }

    private String stripComments(String source) {
        String withoutBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)//.*$", "");
    }
}
