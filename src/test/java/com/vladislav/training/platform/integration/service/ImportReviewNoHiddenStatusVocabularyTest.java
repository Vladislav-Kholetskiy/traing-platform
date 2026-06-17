package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code ImportReviewNoHiddenStatusVocabulary}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportReviewNoHiddenStatusVocabularyTest {

    private static final Path REVIEW_SERVICE_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportItemReviewServiceImpl.java"
    );
    private static final Path REVIEW_CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/controller/ImportItemReviewController.java"
    );

    @Test
    void importItemStatusVocabularyDoesNotGrowHiddenResolvedOrRejectedStates() {
        assertThat(ImportItemStatus.values())
            .extracting(Enum::name)
            .doesNotContain("RESOLVED", "REJECTED");
    }

    @Test
    void reviewImplementationDoesNotMaterializeHiddenStatusStrings() throws Exception {
        String reviewServiceSource = Files.readString(REVIEW_SERVICE_PATH);
        String reviewControllerSource = Files.readString(REVIEW_CONTROLLER_PATH);

        assertThat(reviewServiceSource)
            .doesNotContain("ImportItemStatus.RESOLVED")
            .doesNotContain("ImportItemStatus.REJECTED")
            .doesNotContain("\"RESOLVED\"");

        assertThat(reviewControllerSource)
            .contains("/apply-review")
            .contains("/reject-review")
            .doesNotContain("/resolve")
            .doesNotContain("ImportItemStatus.REJECTED");
    }
}
