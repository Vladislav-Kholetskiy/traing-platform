package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.result.query.SelfHistoricalResultQueryService.SelfHistoricalResultQuery;
import com.vladislav.training.platform.result.query.SelfHistoricalResultQueryService.SelfHistoricalResultReadModel;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
/**
 * Проверяет, что {@code SelfHistoricalResultQueryService} правильно связан с зависимостями и конфигурацией при запуске.
 * Такой тест быстро показывает ошибки в сборке связей.
 */
@SpringJUnitConfig(SelfHistoricalResultQueryServiceRuntimeWiringTest.SelfHistoricalResultQueryRuntimeTestConfiguration.class)
class SelfHistoricalResultQueryServiceRuntimeWiringTest {

    private static final Instant RECORDED_AT = Instant.parse("2026-04-27T11:20:00Z");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SelfHistoricalResultQueryService queryService;

    @Autowired
    private SelfHistoricalResultReader readSeam;

    @Autowired
    private SpringDataResultJpaRepository repository;

    @Autowired
    private AccessSpecificationPolicy accessSpecificationPolicy;

    @Autowired
    private AccessPolicyQueryContextResolver contextResolver;

    @Test
    void contextPublishesPolicyAwareQueryServiceAndAssemblesThroughJpaReadSeam() {
        assertThat(applicationContext.getBean(SelfHistoricalResultQueryService.class))
            .isInstanceOf(SelfHistoricalResultQueryServiceImpl.class);
        assertThat(applicationContext.getBean(SelfHistoricalResultReader.class))
            .isNotNull();
        assertThat(readSeam.getClass().getSimpleName()).isEqualTo("JpaSelfHistoricalResultReader");
    }

    @Test
    void runtimeServiceReachesWorkingJpaReadSeamWhenCanonicalPolicyAllowsContext() {
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            301L,
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            RECORDED_AT,
            null,
            null,
            "self_result_history",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(repository.findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(301L)).thenReturn(List.of(
            historyRow(7002L, 9002L, 502L, "Assigned Test", new BigDecimal("80.0000"), new BigDecimal("8.0000"), true, AttemptMode.ASSIGNED, 3001L, RECORDED_AT),
            historyRow(7001L, 9001L, 501L, "Self Test", new BigDecimal("40.0000"), new BigDecimal("4.0000"), false, AttemptMode.SELF, null, RECORDED_AT.minusSeconds(60))
        ));

        List<SelfHistoricalResultReadModel> result = queryService.findSelfHistoricalResults(new SelfHistoricalResultQuery(301L));

        assertThat(result).containsExactly(
            new SelfHistoricalResultReadModel(
                7002L,
                RECORDED_AT,
                9002L,
                502L,
                "Assigned Test",
                new BigDecimal("80.0000"),
                new BigDecimal("8.0000"),
                true,
                AttemptMode.ASSIGNED,
                3001L
            ),
            new SelfHistoricalResultReadModel(
                7001L,
                RECORDED_AT.minusSeconds(60),
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
        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verify(repository).findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(301L);
        verifyNoMoreInteractions(repository, accessSpecificationPolicy, contextResolver);
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
    static class SelfHistoricalResultQueryRuntimeTestConfiguration {

        @Bean
        SpringDataResultJpaRepository springDataResultJpaRepository() {
            return org.mockito.Mockito.mock(SpringDataResultJpaRepository.class);
        }

        @Bean
        AccessSpecificationPolicy accessSpecificationPolicy() {
            return org.mockito.Mockito.mock(AccessSpecificationPolicy.class);
        }

        @Bean
        AccessPolicyQueryContextResolver accessPolicyQueryContextResolver() {
            return org.mockito.Mockito.mock(AccessPolicyQueryContextResolver.class);
        }

        @Bean
        SelfHistoricalResultReader SelfHistoricalResultReader(SpringDataResultJpaRepository repository) {
            try {
                Class<?> type = Class.forName(
                    "com.vladislav.training.platform.result.infrastructure.persistence.JpaSelfHistoricalResultReader"
                );
                var constructor = type.getDeclaredConstructor(SpringDataResultJpaRepository.class);
                constructor.setAccessible(true);
                return (SelfHistoricalResultReader) constructor.newInstance(repository);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to instantiate JpaSelfHistoricalResultReader", exception);
            }
        }

        @Bean
        SelfHistoricalResultQueryService selfHistoricalResultQueryService(
            SelfHistoricalResultReader SelfHistoricalResultReader,
            AccessSpecificationPolicy accessSpecificationPolicy,
            AccessPolicyQueryContextResolver contextResolver
        ) {
            return new SelfHistoricalResultQueryServiceImpl(
                SelfHistoricalResultReader,
                accessSpecificationPolicy,
                contextResolver
            );
        }
    }
}

