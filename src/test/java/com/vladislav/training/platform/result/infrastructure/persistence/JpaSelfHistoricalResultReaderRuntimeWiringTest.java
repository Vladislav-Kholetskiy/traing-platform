package com.vladislav.training.platform.result.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader.SelfHistoricalResultReadCriteria;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader.SelfHistoricalResultReadRow;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
/**
 * Проверяет, что {@code JpaSelfHistoricalResultReader} правильно связан с зависимостями и конфигурацией при запуске.
 * Такой тест быстро показывает ошибки в сборке связей.
 */
@SpringJUnitConfig(JpaSelfHistoricalResultReaderRuntimeWiringTest.JpaSelfHistoricalResultReaderRuntimeTestConfiguration.class)
class JpaSelfHistoricalResultReaderRuntimeWiringTest {

    private static final Instant COMPLETED_AT = Instant.parse("2026-04-27T09:15:00Z");
    private static final String LEGACY_BLOCKED_MESSAGE =
        "Immutable result-side baseline" + " does not expose a materialized actor anchor";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SelfHistoricalResultReader seam;

    @Autowired
    private SpringDataResultJpaRepository repository;

    @Test
    void contextPublishesJpaSelfHistoricalResultReadSeamAsSpringManagedBean() {
        assertThat(applicationContext.getBean(SelfHistoricalResultReader.class))
            .isInstanceOf(JpaSelfHistoricalResultReader.class);
    }

    @Test
    void seamUsesResultRootedRepositoryPathAndMapsRowsWithoutAttemptSideBackdoorRecovery() {
        when(repository.findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(301L))
            .thenReturn(List.of(
                historyRow(7002L, 9002L, 502L, "Assigned Test", new BigDecimal("80.0000"), new BigDecimal("8.0000"), true, AttemptMode.ASSIGNED, 3001L, COMPLETED_AT),
                historyRow(7001L, 9001L, 501L, "Self Test", new BigDecimal("40.0000"), new BigDecimal("4.0000"), false, AttemptMode.SELF, null, COMPLETED_AT.minusSeconds(60))
            ));

        List<SelfHistoricalResultReadRow> result = seam.findSelfHistoricalResultRows(new SelfHistoricalResultReadCriteria(301L));

        assertThat(result).containsExactly(
            new SelfHistoricalResultReadRow(
                7002L,
                COMPLETED_AT,
                9002L,
                502L,
                "Assigned Test",
                new BigDecimal("80.0000"),
                new BigDecimal("8.0000"),
                true,
                AttemptMode.ASSIGNED,
                3001L
            ),
            new SelfHistoricalResultReadRow(
                7001L,
                COMPLETED_AT.minusSeconds(60),
                9001L,
                501L,
                "Self Test",
                new BigDecimal("40.0000"),
                new BigDecimal("4.0000"),
                false,
                AttemptMode.SELF,
                null
            )
        );
        verify(repository).findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(301L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void seamSourceDoesNotContainLegacyBlockedMessageOrAttemptBackdoorDependencies() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"
        ));

        assertThat(source)
            .contains("findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc")
            .doesNotContain(LEGACY_BLOCKED_MESSAGE)
            .doesNotContain("IllegalStateException")
            .doesNotContain("SpringDataTestAttempt")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService");
    }

    private SpringDataResultJpaRepository.ResultHistorySummaryRowView historyRow(
        Long resultId,
        Long testAttemptId,
        Long testId,
        String testName,
        BigDecimal scorePercent,
        BigDecimal score,
        boolean passed,
        AttemptMode attemptMode,
        Long assignmentId,
        Instant recordedAt
    ) {
        return new SpringDataResultJpaRepository.ResultHistorySummaryRowView() {
            @Override
            public Long getResultId() {
                return resultId;
            }

            @Override
            public Instant getRecordedAt() {
                return recordedAt;
            }

            @Override
            public Long getTestAttemptId() {
                return testAttemptId;
            }

            @Override
            public Long getTestId() {
                return testId;
            }

            @Override
            public String getTestName() {
                return testName;
            }

            @Override
            public BigDecimal getScorePercent() {
                return scorePercent;
            }

            @Override
            public BigDecimal getScore() {
                return score;
            }

            @Override
            public boolean isPassed() {
                return passed;
            }

            @Override
            public AttemptMode getAttemptMode() {
                return attemptMode;
            }

            @Override
            public Long getAssignmentId() {
                return assignmentId;
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
        basePackageClasses = JpaSelfHistoricalResultReader.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Repository.class),
        resourcePattern = "JpaSelfHistoricalResultReader.class"
    )
    static class JpaSelfHistoricalResultReaderRuntimeTestConfiguration {

        @Bean
        SpringDataResultJpaRepository springDataResultJpaRepository() {
            return org.mockito.Mockito.mock(SpringDataResultJpaRepository.class);
        }
    }
}


