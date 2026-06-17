package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AnalyticsCampaignAggregateSourceReaderImpl}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AnalyticsCampaignAggregateSourceReaderImplContractTest {

    private static final Path SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/service/AnalyticsCampaignAggregateSourceReaderImpl.java"
    );

    @Test
    void sourceReaderUsesOnlyCampaignRecipientSnapshotAndAssignmentFacts() throws Exception {
        String source = Files.readString(SOURCE);

        assertThat(source)
            .contains("assignment_campaign")
            .contains("assignment_campaign_recipient_snapshot")
            .contains("assignment a")
            .doesNotContain("result_question_snapshot")
            .doesNotContain("result_answer_option_snapshot")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("test_attempt")
            .doesNotContain("organizational_unit")
            .doesNotContain("userOrganizationAssignment")
            .doesNotContain("setStatus(")
            .doesNotContain("saveAndFlush")
            .doesNotContain("update assignment")
            .doesNotContain("insert into assignment");
    }
}
