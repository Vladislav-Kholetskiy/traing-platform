package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.result.query.SelfHistoricalResultQueryService.SelfHistoricalResultQuery;
import com.vladislav.training.platform.result.query.SelfHistoricalResultQueryService.SelfHistoricalResultReadModel;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader.SelfHistoricalResultReadCriteria;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader.SelfHistoricalResultReadRow;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code SelfHistoricalResultQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class SelfHistoricalResultQueryServiceImplTest {

    private static final Instant FIRST_RECORDED_AT = Instant.parse("2026-04-20T08:00:00Z");
    private static final Instant SECOND_RECORDED_AT = Instant.parse("2026-04-21T09:30:00Z");
    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-27T08:45:00Z");

    @Mock
    private SelfHistoricalResultReader SelfHistoricalResultReader;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    @InjectMocks
    private SelfHistoricalResultQueryServiceImpl service;

    @Test
    void allowPolicyCallsReadSeamAfterSelfHistoryContextResolutionAndMapsRows() {
        SelfHistoricalResultQuery query = new SelfHistoricalResultQuery(101L);
        AccessPolicyQueryContext context = selfHistoryContext(101L);
        List<SelfHistoricalResultReadRow> rows = List.of(
            new SelfHistoricalResultReadRow(
                7001L,
                FIRST_RECORDED_AT,
                9101L,
                501L,
                "Assigned Test",
                new BigDecimal("80.0000"),
                new BigDecimal("8.0000"),
                true,
                AttemptMode.ASSIGNED,
                3001L
            ),
            new SelfHistoricalResultReadRow(
                7002L,
                SECOND_RECORDED_AT,
                9102L,
                502L,
                "Self Test",
                new BigDecimal("40.0000"),
                new BigDecimal("4.0000"),
                false,
                AttemptMode.SELF,
                null
            )
        );
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(SelfHistoricalResultReader.findSelfHistoricalResultRows(new SelfHistoricalResultReadCriteria(101L)))
            .thenReturn(rows);

        List<SelfHistoricalResultReadModel> result = service.findSelfHistoricalResults(query);

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        );
        verify(accessSpecificationPolicy).canRead(context);
        ArgumentCaptor<SelfHistoricalResultReadCriteria> criteriaCaptor =
            ArgumentCaptor.forClass(SelfHistoricalResultReadCriteria.class);
        verify(SelfHistoricalResultReader).findSelfHistoricalResultRows(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue()).isEqualTo(new SelfHistoricalResultReadCriteria(101L));
        assertThat(result).containsExactly(
            new SelfHistoricalResultReadModel(
                7001L,
                FIRST_RECORDED_AT,
                9101L,
                501L,
                "Assigned Test",
                new BigDecimal("80.0000"),
                new BigDecimal("8.0000"),
                true,
                AttemptMode.ASSIGNED,
                3001L
            ),
            new SelfHistoricalResultReadModel(
                7002L,
                SECOND_RECORDED_AT,
                9102L,
                502L,
                "Self Test",
                new BigDecimal("40.0000"),
                new BigDecimal("4.0000"),
                false,
                AttemptMode.SELF,
                null
            )
        );
        var ordered = inOrder(contextResolver, accessSpecificationPolicy, SelfHistoricalResultReader);
        ordered.verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        );
        ordered.verify(accessSpecificationPolicy).canRead(context);
        ordered.verify(SelfHistoricalResultReader).findSelfHistoricalResultRows(new SelfHistoricalResultReadCriteria(101L));
        verifyNoMoreInteractions(SelfHistoricalResultReader, accessSpecificationPolicy, contextResolver);
    }

    @Test
    void denyPolicyDoesNotCallReadSeamAndFailsClosedBeforeMaterialization() {
        AccessPolicyQueryContext context = selfHistoryContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findSelfHistoricalResults(new SelfHistoricalResultQuery(101L)))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("self result history");

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(SelfHistoricalResultReader);
        verifyNoMoreInteractions(accessSpecificationPolicy, contextResolver);
    }

    @Test
    void mismatchedActorFailsClosedBeforePolicyOrMaterialization() {
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        )).thenReturn(selfHistoryContext(999L));

        assertThatThrownBy(() -> service.findSelfHistoricalResults(new SelfHistoricalResultQuery(101L)))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("self result history");

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        );
        verify(accessSpecificationPolicy, never()).canRead(selfHistoryContext(999L));
        verifyNoInteractions(SelfHistoricalResultReader);
        verifyNoMoreInteractions(contextResolver);
    }

    @Test
    void serviceContractOnlyAcceptsActorSelfCriteriaSoArbitraryTargetMismatchCannotBeIntroducedAtApiLevel() {
        RecordComponent[] components = SelfHistoricalResultQuery.class.getRecordComponents();

        assertThat(components).hasSize(1);
        assertThat(components[0].getName()).isEqualTo("actorUserId");
        assertThat(components[0].getType()).isEqualTo(Long.class);
    }

    @Test
    void allowPolicyKeepsEmptyReadSeamResponseAsEmptyList() {
        AccessPolicyQueryContext context = selfHistoryContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(SelfHistoricalResultReader.findSelfHistoricalResultRows(new SelfHistoricalResultReadCriteria(101L)))
            .thenReturn(List.of());

        List<SelfHistoricalResultReadModel> result = service.findSelfHistoricalResults(
            new SelfHistoricalResultQuery(101L)
        );

        assertThat(result).isEmpty();
        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verify(SelfHistoricalResultReader).findSelfHistoricalResultRows(new SelfHistoricalResultReadCriteria(101L));
        verifyNoMoreInteractions(SelfHistoricalResultReader, accessSpecificationPolicy, contextResolver);
    }

    @Test
    void serviceSourceDoesNotDependOnCurrentAttemptOrAttemptRecoveryServices() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"
        ));

        assertThat(source)
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("SpringDataTestAttemptJpaRepository")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService");
    }

    private AccessPolicyQueryContext selfHistoryContext(Long actorUserId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            EFFECTIVE_AT,
            null,
            null,
            "self_result_history",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }
}

