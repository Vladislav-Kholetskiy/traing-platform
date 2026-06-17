package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AnalyticsResultRebuildServiceEmptyScope}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AnalyticsResultRebuildServiceEmptyScopeTest {

    private static final String CONTRACT_MESSAGE =
        "Stage 2 Step 1 empty-scope lock requires AnalyticsResultRebuildServiceImpl to remain fail-closed until explicit SCN-11 source reader and aggregate writer steps are opened.";

    private static final Path IMPLEMENTATION_SOURCE_PATH = Path.of(
        "src",
        "main",
        "java",
        "com",
        "vladislav",
        "training",
        "platform",
        "analytics",
        "service",
        "AnalyticsResultRebuildServiceImpl.java"
    );

    private static final List<String> FORBIDDEN_ANNOTATION_MARKERS = List.of(
        "@Service",
        "@Component",
        "@Scheduled",
        "@EnableScheduling",
        "@RestController",
        "@Controller",
        "@RequestMapping",
        "@PostMapping",
        "@GetMapping"
    );

    private static final List<String> FORBIDDEN_RUNTIME_PERSISTENCE_MARKERS = List.of(
        "Repository",
        "Entity",
        "JdbcTemplate",
        "EntityManager",
        "save(",
        "saveAll(",
        "delete(",
        "deleteAll(",
        "insert into",
        "update ",
        "delete from",
        "questionRepository",
        "topicRepository",
        "assignmentRepository",
        "testAttemptRepository",
        "resultRepository"
    );

    @Test
    void runtimeShellRemainsFailClosedAndEmptyScopedUntilExplicitScn11StepsOpen() throws IOException {
        AnalyticsResultRebuildServiceImpl service = new AnalyticsResultRebuildServiceImpl();
        Instant validStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant validEnd = Instant.parse("2026-01-02T00:00:00Z");

        assertThatThrownBy(service::rebuildAllAnalytics)
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(service::reconcileAnalytics)
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> service.rebuildResultAnalytics(validStart, validEnd))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> service.rebuildResultAnalytics(null, validEnd))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.rebuildResultAnalytics(validStart, null))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.rebuildResultAnalytics(validStart, validStart))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.rebuildResultAnalytics(validEnd, validStart))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(IllegalArgumentException.class);

        String source = Files.readString(IMPLEMENTATION_SOURCE_PATH);

        assertThat(FORBIDDEN_ANNOTATION_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(FORBIDDEN_RUNTIME_PERSISTENCE_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
    }
}
