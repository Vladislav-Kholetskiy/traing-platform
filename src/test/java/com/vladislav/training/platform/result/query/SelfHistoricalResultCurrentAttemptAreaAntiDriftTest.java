package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentQueryService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.AssignedCurrentAttemptReadFoundationStateReadService;
import com.vladislav.training.platform.testing.admission.SelfCurrentAttemptReadFoundationStateReadService;
import com.vladislav.training.platform.testing.controller.CurrentAttemptReadController;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitTerminalService;
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonTerminalService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitTerminalService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Не даёт {@code SelfHistoricalResultCurrentAttemptArea} незаметно уйти от текущего замысла.
 * Тест страхует важные ограничения и исходную идею решения.
 */
class SelfHistoricalResultCurrentAttemptAreaAntiDriftTest {

    @Test
    void selfHistoricalResultReadPathDoesNotDependOnCurrentAttemptContourOrExecutionEntryServices() throws IOException {
        assertThat(fieldTypes(SelfHistoricalResultQueryServiceImpl.class))
            .containsExactly(
                SelfHistoricalResultReader.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class
            )
            .doesNotContain(
                CurrentAttemptReadController.class,
                AssignedCurrentAttemptReadFoundationStateReadService.class,
                SelfCurrentAttemptReadFoundationStateReadService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                AssignmentQueryService.class,
                AssignmentCommandService.class,
                AssignmentStatusRecalculationService.class,
                ResultRecordingService.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class,
                SpringDataResultJpaRepository.class,
                ResultEntity.class,
                AssignedCurrentAttemptReadService.class,
                SelfCurrentAttemptReadService.class
            );

        List<String> selfHistorySources = List.of(
            read("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryService.java"),
            read("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"),
            read("src/main/java/com/vladislav/training/platform/result/query/internal/SelfHistoricalResultReader.java")
        );

        assertThat(selfHistorySources).allSatisfy(source -> assertThat(source)
            .doesNotContain("CurrentAttemptReadController")
            .doesNotContain("AssignedCurrentAttemptReadFoundationStateReadService")
            .doesNotContain("SelfCurrentAttemptReadFoundationStateReadService")
            .doesNotContain("AssignedAttemptEntryFacade")
            .doesNotContain("SelfAttemptEntryFacade")
            .doesNotContain("AssignedAttemptEntryService")
            .doesNotContain("SelfAttemptEntryService")
            .doesNotContain("AssignedAttemptSubmitSequencingService")
            .doesNotContain("AssignedAttemptSubmitTerminalService")
            .doesNotContain("SelfAttemptSubmitSequencingService")
            .doesNotContain("SelfAttemptSubmitTerminalService")
            .doesNotContain("SelfAttemptAbandonSequencingService")
            .doesNotContain("SelfAttemptAbandonTerminalService")
            .doesNotContain("AssignmentQueryService")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("SpringDataResultJpaRepository")
            .doesNotContain("ResultEntity")
            .doesNotContain("AssignedCurrentAttemptReadService")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("findCurrentAssignedAttemptForActor(")
            .doesNotContain("findCurrentSelfAttemptForActor(")
            .doesNotContain("findActiveAssignedAttemptForActor(")
            .doesNotContain("findActiveSelfAttempt(")
            .doesNotContain("startOrContinueAssignedAttempt(")
            .doesNotContain("startOrContinueSelfAttempt(")
            .doesNotContain("submitAssignedAttempt(")
            .doesNotContain("submitSelfAttempt(")
            .doesNotContain("abandonSelfAttempt(")
            .doesNotContain("recalculate")
            .doesNotContain("rebuild")
            .doesNotContain("refresh")
            .doesNotContain("recordResult"));
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .toList();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}

