package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.assignment.service.AssignmentSelfScopedQueryService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.result.query.SelfHistoricalResultQueryService.SelfHistoricalResultQuery;
import com.vladislav.training.platform.result.query.SelfHistoricalResultQueryService.SelfHistoricalResultReadModel;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.controller.CurrentAttemptReadController;
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code SelfHistoricalResult}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class SelfHistoricalResultControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T18:00:00Z");

    @Mock
    private SelfHistoricalResultQueryService selfHistoricalResultQueryService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new SelfHistoricalResultController(selfHistoricalResultQueryService, interactiveActorResolver)
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void selfHistoricalResultListEndpointUsesTrustedActorIdentityAndReturnsSummaryDtos() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(selfHistoricalResultQueryService.findSelfHistoricalResults(new SelfHistoricalResultQuery(101L)))
            .thenReturn(List.of(
                new SelfHistoricalResultReadModel(
                    7001L,
                    Instant.parse("2026-04-20T08:00:00Z"),
                    9101L,
                    501L,
                    "Assigned Test",
                    new BigDecimal("70.0000"),
                    new BigDecimal("7.0000"),
                    true,
                    AttemptMode.ASSIGNED,
                    3001L
                ),
                new SelfHistoricalResultReadModel(
                    7002L,
                    Instant.parse("2026-04-21T09:30:00Z"),
                    9102L,
                    502L,
                    "Self Test",
                    new BigDecimal("40.0000"),
                    new BigDecimal("4.0000"),
                    false,
                    AttemptMode.SELF,
                    null
                )
            ));

        mockMvc.perform(get("/api/v1/self/results/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].resultId").value(7001))
            .andExpect(jsonPath("$[0].recordedAt").value("2026-04-20T08:00:00Z"))
            .andExpect(jsonPath("$[0].testAttemptId").value(9101))
            .andExpect(jsonPath("$[0].testId").value(501))
            .andExpect(jsonPath("$[0].testName").value("Assigned Test"))
            .andExpect(jsonPath("$[0].scorePercent").value(70.0000))
            .andExpect(jsonPath("$[0].score").value(7.0000))
            .andExpect(jsonPath("$[0].passed").value(true))
            .andExpect(jsonPath("$[0].attemptMode").value("ASSIGNED"))
            .andExpect(jsonPath("$[0].assignmentId").value(3001))
            .andExpect(jsonPath("$[1].resultId").value(7002))
            .andExpect(jsonPath("$[1].attemptMode").value("SELF"))
            .andExpect(jsonPath("$[1].assignmentId").isEmpty())
            .andExpect(jsonPath("$[0].targetUserId").doesNotExist())
            .andExpect(jsonPath("$[0].currentAttemptId").doesNotExist())
            .andExpect(jsonPath("$[0].isCorrect").doesNotExist())
            .andExpect(jsonPath("$[0].correctAnswers").doesNotExist())
            .andExpect(jsonPath("$[0].scoringPolicyCode").doesNotExist());

        verify(interactiveActorResolver).resolveActorUserId();
        verify(selfHistoricalResultQueryService).findSelfHistoricalResults(new SelfHistoricalResultQuery(101L));
    }

    @Test
    void requestActorOrSubjectOverrideParametersDoNotOverrideResolvedActorIdentity() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(selfHistoricalResultQueryService.findSelfHistoricalResults(new SelfHistoricalResultQuery(101L)))
            .thenReturn(List.of(
                new SelfHistoricalResultReadModel(
                    7001L,
                    Instant.parse("2026-04-20T08:00:00Z"),
                    9101L,
                    501L,
                    "Assigned Test",
                    new BigDecimal("70.0000"),
                    new BigDecimal("7.0000"),
                    true,
                    AttemptMode.ASSIGNED,
                    3001L
                )
            ));

        mockMvc.perform(get("/api/v1/self/results/history")
                .queryParam("userId", "888")
                .queryParam("targetUserId", "999")
                .queryParam("subjectUserId", "777")
                .queryParam("actorUserId", "666"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].resultId").value(7001));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(selfHistoricalResultQueryService).findSelfHistoricalResults(new SelfHistoricalResultQuery(101L));
    }

    @Test
    void policyViolationFromServiceIsReturnedAsForbiddenWithoutFakeEmptyFallback() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(selfHistoricalResultQueryService.findSelfHistoricalResults(new SelfHistoricalResultQuery(101L)))
            .thenThrow(new PolicyViolationException("SELF_RESULT_HISTORY_DENIED", "Self result history denied"));

        mockMvc.perform(get("/api/v1/self/results/history"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Self result history denied"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(selfHistoricalResultQueryService).findSelfHistoricalResults(new SelfHistoricalResultQuery(101L));
    }

    @Test
    void unauthenticatedActorResolutionFailureIsReturnedPredictably() throws Exception {
        when(interactiveActorResolver.resolveActorUserId())
            .thenThrow(new PolicyViolationException("ACTOR_UNAUTHENTICATED", "Authenticated principal is required"));

        mockMvc.perform(get("/api/v1/self/results/history"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Authenticated principal is required"));
    }

    @Test
    void controllerMethodAcceptsNoClientSuppliedActorSelectorParameters() throws Exception {
        Method controllerMethod = SelfHistoricalResultController.class.getDeclaredMethod("findSelfHistoricalResults");

        assertThat(controllerMethod.getParameterCount()).isZero();
        assertThat(controllerMethod.getGenericReturnType()).isInstanceOf(ParameterizedType.class);
        ParameterizedType returnType = (ParameterizedType) controllerMethod.getGenericReturnType();
        assertThat(returnType.getRawType()).isEqualTo(List.class);
        assertThat(returnType.getActualTypeArguments()).containsExactly(SelfHistoricalResultSummaryDto.class);
        assertThat(controllerMethod.isAnnotationPresent(RequestParam.class)).isFalse();
        assertThat(controllerMethod.isAnnotationPresent(PathVariable.class)).isFalse();
        assertThat(controllerMethod.isAnnotationPresent(RequestBody.class)).isFalse();
    }

    @Test
    void selfHistoricalResultControllerDependsOnlyOnQueryServiceAndTrustedActorResolver() {
        assertThat(fieldTypes(SelfHistoricalResultController.class))
            .containsExactly(
                SelfHistoricalResultQueryService.class,
                InteractiveActorResolver.class
            )
            .doesNotContain(
                CurrentAttemptReadController.class,
                AssignmentSelfScopedQueryService.class,
                AssignedCurrentAttemptReadService.class,
                SelfCurrentAttemptReadService.class,
                SelfHistoricalResultQueryServiceImpl.class,
                SelfHistoricalResultReader.class,
                SpringDataResultJpaRepository.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class,
                ResultRecordingService.class,
                AssignmentStatusRecalculationService.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class
            );
    }

    @Test
    void controllerSourceDoesNotDependOnRepositorySeamOrPolicyImplementations() throws Exception {
        String source = Files.readString(
            Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java")
        );

        assertThat(source)
            .doesNotContain("SelfHistoricalResultReader")
            .doesNotContain("JpaSelfHistoricalResultReader")
            .doesNotContain("SpringDataResultJpaRepository")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("AccessPolicyQueryContextResolver");
    }

    @Test
    void emptyServiceResponseIsReturnedAsEmptyJsonArray() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(selfHistoricalResultQueryService.findSelfHistoricalResults(new SelfHistoricalResultQuery(101L)))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/self/results/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        verify(interactiveActorResolver).resolveActorUserId();
        verify(selfHistoricalResultQueryService).findSelfHistoricalResults(new SelfHistoricalResultQuery(101L));
        verify(selfHistoricalResultQueryService, never()).findSelfHistoricalResults(new SelfHistoricalResultQuery(999L));
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}

