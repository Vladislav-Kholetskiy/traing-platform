package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessReadArea;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ScenarioBoundary} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ScenarioBoundaryRegressionTest {

    @Test
    void analyticsPolicyContoursStaySeparateWithoutGenericAnalyticsReplacement() {
        assertThat(Arrays.stream(AccessReadArea.values()).toList())
            .contains(
                AccessReadArea.SELF_RESULT_HISTORY,
                AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
                AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
                AccessReadArea.EXPERT_QUESTION_ANALYTICS
            );

        assertThat(Arrays.stream(AccessReadArea.values())
            .map(Enum::name)
            .toList())
            .doesNotContain(
                "WAVE5",
                "WAVE5_ANALYTICS",
                "MANAGERIAL_ANALYTICS",
                "EXPERT_ANALYTICS"
            );
    }

    @Test
    void analyticsQueryServicesDoNotCrossMergeScenarioDependencies() throws IOException {
        assertThat(read(
            "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"
        ))
            .contains("SelfHistoricalResultReader")
            .contains("AccessSpecificationPolicy")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("SpringDataTestAttempt")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ExpertQuestionAnalyticsQueryService")
            .doesNotContain("CapabilityAdmissionPolicy");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryServiceImpl.java"
        ))
            .contains("ManagerialCurrentSupervisionReadRepository")
            .contains("ManagerialReadScopeProjectionService")
            .doesNotContain("ManagerialHistoricalAnalyticsReadRepository")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ExpertQuestionAnalyticsReadRepository")
            .doesNotContain("ExpertQuestionAnalyticsQueryService")
            .doesNotContain("AnalyticsUserTopicAggregateEntity")
            .doesNotContain("AnalyticsDepartmentTopicAggregateEntity")
            .doesNotContain("AnalyticsQuestionAggregateEntity")
            .doesNotContain("CapabilityAdmissionPolicy");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryServiceImpl.java"
        ))
            .contains("ManagerialHistoricalAnalyticsReadRepository")
            .contains("ManagerialReadScopeProjectionService")
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("JpaManagerialCurrentSupervisionReadRepositoryAdapter")
            .doesNotContain("AssignmentEntity")
            .doesNotContain("AssignmentTestEntity")
            .doesNotContain("ExpertQuestionAnalyticsReadRepository")
            .doesNotContain("ExpertQuestionAnalyticsQueryService")
            .doesNotContain("AnalyticsCampaignAggregateRepository")
            .doesNotContain("AnalyticsCampaignAggregate")
            .doesNotContain("analytics_campaign_aggregate")
            .doesNotContain("CapabilityAdmissionPolicy");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryServiceImpl.java"
        ))
            .contains("ExpertQuestionAnalyticsReadRepository")
            .contains("AccessSpecificationPolicy")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("ManagerialHistoricalAnalyticsReadRepository")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("ContentLifecycleController")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("CapabilityAdmissionPolicy");
    }

    @Test
    void analyticsReadRepositoriesStayBoundToTheirOwnSourceFamilies() throws IOException {
        assertThat(read(
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/"
                + "JpaManagerialCurrentSupervisionReadRepositoryAdapter.java"
        ))
            .contains("AssignmentEntity")
            .contains("AssignmentTestEntity")
            .doesNotContain("AnalyticsUserTopicAggregateEntity")
            .doesNotContain("AnalyticsDepartmentTopicAggregateEntity")
            .doesNotContain("AnalyticsQuestionAggregateEntity")
            .doesNotContain("ManagerialHistoricalAnalyticsReadRepository")
            .doesNotContain("ExpertQuestionAnalyticsReadRepository");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                + "JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.java"
        ))
            .contains("AnalyticsUserTopicAggregateEntity")
            .contains("AnalyticsDepartmentTopicAggregateEntity")
            .doesNotContain("AssignmentEntity")
            .doesNotContain("AssignmentTestEntity")
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("JpaManagerialCurrentSupervisionReadRepositoryAdapter")
            .doesNotContain("AnalyticsQuestionAggregateEntity")
            .doesNotContain("ExpertQuestionAnalyticsReadRepository");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                + "JpaExpertQuestionAnalyticsReadRepositoryAdapter.java"
        ))
            .contains("AnalyticsQuestionAggregateEntity")
            .doesNotContain("AnalyticsUserTopicAggregateEntity")
            .doesNotContain("AnalyticsDepartmentTopicAggregateEntity")
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("ManagerialHistoricalAnalyticsReadRepository")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("ContentLifecycleController")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AssignmentEntity")
            .doesNotContain("AssignmentTestEntity");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/"
                + "JpaSelfHistoricalResultReader.java"
        ))
            .contains("SpringDataResultJpaRepository")
            .doesNotContain("SpringDataTestAttempt")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService");
    }

    @Test
    void analyticsControllersDependOnlyOnTheirOwnScenarioQueryServiceWithoutCrossMerge() throws IOException {
        assertThat(read(
            "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java"
        ))
            .contains("SelfHistoricalResultQueryService")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ExpertQuestionAnalyticsQueryService");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/assignment/controller/ManagerialCurrentSupervisionController.java"
        ))
            .contains("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("SelfHistoricalResultQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ExpertQuestionAnalyticsQueryService");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsController.java"
        ))
            .contains("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("SelfHistoricalResultQueryService")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("ExpertQuestionAnalyticsQueryService");

        assertThat(read(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsController.java"
        ))
            .contains("ExpertQuestionAnalyticsQueryService")
            .doesNotContain("SelfHistoricalResultQueryService")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService");
    }

    @Test
    void analyticsDoesNotIntroduceGenericSharedScenarioFacadeNames() throws IOException {
        List<String> productionSources = List.of(
            read("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java"),
            read("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"),
            read("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"),
            read("src/main/java/com/vladislav/training/platform/assignment/controller/ManagerialCurrentSupervisionController.java"),
            read("src/main/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryServiceImpl.java"),
            read("src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/JpaManagerialCurrentSupervisionReadRepositoryAdapter.java"),
            read("src/main/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsController.java"),
            read("src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryServiceImpl.java"),
            read("src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.java"),
            read("src/main/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsController.java"),
            read("src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryServiceImpl.java"),
            read("src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/JpaExpertQuestionAnalyticsReadRepositoryAdapter.java")
        );

        assertThat(productionSources)
            .allSatisfy(source -> assertThat(source)
                .doesNotContain("AnalyticsAnalyticsService")
                .doesNotContain("CommonAnalyticsQueryService")
                .doesNotContain("ProgressQueryService")
                .doesNotContain("ManagerialAnalyticsQueryService")
                .doesNotContain("AnalyticsAnalyticsController")
                .doesNotContain("CommonAnalyticsController")
                .doesNotContain("ProgressController")
                .doesNotContain("ManagerialAnalyticsController"));
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}

