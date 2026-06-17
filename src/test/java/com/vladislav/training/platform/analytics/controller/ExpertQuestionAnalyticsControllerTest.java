package com.vladislav.training.platform.analytics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.analytics.controller.dto.ExpertQuestionAnalyticsResponse;
import com.vladislav.training.platform.analytics.query.ExpertQuestionAnalyticsDto;
import com.vladislav.training.platform.analytics.query.ExpertQuestionAnalyticsQueryService;
import com.vladislav.training.platform.analytics.query.ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.service.ManagerialCurrentSupervisionQueryService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.infrastructure.persistence.QuestionEntity;
import com.vladislav.training.platform.content.service.CourseQueryService;
import com.vladislav.training.platform.content.service.QuestionCommandService;
import com.vladislav.training.platform.content.service.QuestionLifecycleService;
import com.vladislav.training.platform.result.query.SelfHistoricalResultQueryService;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
/**
 * Проверяет поведение контроллера {@code ExpertQuestionAnalytics}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class ExpertQuestionAnalyticsControllerTest {

    private static final Instant FIXED_EFFECTIVE_AT = Instant.parse("2026-04-28T15:30:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");

    @Mock
    private ExpertQuestionAnalyticsQueryService expertQuestionAnalyticsQueryService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ExpertQuestionAnalyticsController(
                    expertQuestionAnalyticsQueryService,
                    interactiveActorResolver,
                    utcClock
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void expertQuestionAnalyticsEndpointUsesTrustedActorClockAndPeriodParamsAndReturnsRows() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_EFFECTIVE_AT);
        when(expertQuestionAnalyticsQueryService.findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(101L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        )).thenReturn(List.of(
            new ExpertQuestionAnalyticsDto(
                701L,
                PERIOD_START,
                PERIOD_END,
                15,
                8,
                7,
                new BigDecimal("4.7500"),
                Instant.parse("2026-05-01T09:00:00Z"),
                Instant.parse("2026-05-01T09:15:00Z")
            ),
            new ExpertQuestionAnalyticsDto(
                702L,
                PERIOD_START,
                PERIOD_END,
                11,
                5,
                6,
                new BigDecimal("3.2500"),
                Instant.parse("2026-05-01T10:00:00Z"),
                Instant.parse("2026-05-01T10:15:00Z")
            )
        ));

        mockMvc.perform(get("/api/v1/expert/question-analytics")
                .queryParam("periodStart", "2026-04-01T00:00:00Z")
                .queryParam("periodEnd", "2026-04-30T23:59:59Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].questionId").value(701))
            .andExpect(jsonPath("$[0].periodStart").value("2026-04-01T00:00:00Z"))
            .andExpect(jsonPath("$[0].periodEnd").value("2026-04-30T23:59:59Z"))
            .andExpect(jsonPath("$[0].attemptCount").value(15))
            .andExpect(jsonPath("$[0].correctCount").value(8))
            .andExpect(jsonPath("$[0].incorrectCount").value(7))
            .andExpect(jsonPath("$[0].averageEarnedScore").value(4.7500))
            .andExpect(jsonPath("$[0].calculatedAt").value("2026-05-01T09:00:00Z"))
            .andExpect(jsonPath("$[0].refreshedAt").value("2026-05-01T09:15:00Z"))
            .andExpect(jsonPath("$[0].actorUserId").doesNotExist())
            .andExpect(jsonPath("$[0].expertUserId").doesNotExist())
            .andExpect(jsonPath("$[0].targetUserId").doesNotExist())
            .andExpect(jsonPath("$[0].managerUserId").doesNotExist())
            .andExpect(jsonPath("$[0].questionText").doesNotExist())
            .andExpect(jsonPath("$[0].questionLabel").doesNotExist());

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
        verify(expertQuestionAnalyticsQueryService).findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(101L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        );
        verifyNoMoreInteractions(expertQuestionAnalyticsQueryService, interactiveActorResolver, utcClock);
    }

    @Test
    void emptyServiceResponseIsReturnedAsEmptyJsonArray() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_EFFECTIVE_AT);
        when(expertQuestionAnalyticsQueryService.findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(101L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/expert/question-analytics")
                .queryParam("periodStart", "2026-04-01T00:00:00Z")
                .queryParam("periodEnd", "2026-04-30T23:59:59Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
        verify(expertQuestionAnalyticsQueryService).findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(101L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        );
        verifyNoMoreInteractions(expertQuestionAnalyticsQueryService, interactiveActorResolver, utcClock);
    }

    @Test
    void policyViolationFromServiceIsReturnedAsForbiddenWithoutFakeEmptyFallback() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_EFFECTIVE_AT);
        when(expertQuestionAnalyticsQueryService.findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(101L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        )).thenThrow(new PolicyViolationException(
            "EXPERT_QUESTION_ANALYTICS_DENIED",
            "Expert question analytics denied"
        ));

        mockMvc.perform(get("/api/v1/expert/question-analytics")
                .queryParam("periodStart", "2026-04-01T00:00:00Z")
                .queryParam("periodEnd", "2026-04-30T23:59:59Z"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Expert question analytics denied"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock, times(2)).now();
        verify(expertQuestionAnalyticsQueryService).findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(101L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        );
        verifyNoMoreInteractions(expertQuestionAnalyticsQueryService, interactiveActorResolver, utcClock);
    }

    @Test
    void requestSelectorsDoNotOverrideResolvedActorIdentity() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_EFFECTIVE_AT);
        when(expertQuestionAnalyticsQueryService.findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(101L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        )).thenReturn(List.of(
            new ExpertQuestionAnalyticsDto(
                701L,
                PERIOD_START,
                PERIOD_END,
                15,
                8,
                7,
                new BigDecimal("4.7500"),
                Instant.parse("2026-05-01T09:00:00Z"),
                Instant.parse("2026-05-01T09:15:00Z")
            )
        ));

        mockMvc.perform(get("/api/v1/expert/question-analytics")
                .queryParam("periodStart", "2026-04-01T00:00:00Z")
                .queryParam("periodEnd", "2026-04-30T23:59:59Z")
                .queryParam("actorUserId", "999")
                .queryParam("expertUserId", "999")
                .queryParam("targetUserId", "999")
                .queryParam("scope", "full")
                .queryParam("scopeOverride", "deny")
                .queryParam("questionIdOverride", "999")
                .queryParam("rebuild", "true")
                .queryParam("refresh", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].questionId").value(701));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
        verify(expertQuestionAnalyticsQueryService).findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(101L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        );
        verify(expertQuestionAnalyticsQueryService, never()).findQuestionAnalytics(
            new ExpertQuestionAnalyticsQuery(999L, FIXED_EFFECTIVE_AT, PERIOD_START, PERIOD_END)
        );
        verifyNoMoreInteractions(expertQuestionAnalyticsQueryService, interactiveActorResolver, utcClock);
    }

    @Test
    void controllerMethodAcceptsOnlyPeriodBoundsRequestParamsAndReturnsPublicResponseDto() throws Exception {
        Method controllerMethod = ExpertQuestionAnalyticsController.class.getDeclaredMethod(
            "findExpertQuestionAnalytics",
            Instant.class,
            Instant.class
        );

        assertThat(controllerMethod.getParameterCount()).isEqualTo(2);
        assertThat(Arrays.stream(controllerMethod.getParameters())
            .map(parameter -> parameter.getName())
            .toList())
            .containsExactly("periodStart", "periodEnd");
        assertThat(Arrays.stream(controllerMethod.getParameterAnnotations())
            .flatMap(Arrays::stream)
            .filter(annotation -> annotation.annotationType().equals(RequestParam.class))
            .count())
            .isEqualTo(2);
        assertThat(controllerMethod.getGenericReturnType().getTypeName())
            .isEqualTo("java.util.List<com.vladislav.training.platform.analytics.controller.dto.ExpertQuestionAnalyticsResponse>");
        assertThat(controllerMethod.isAnnotationPresent(PathVariable.class)).isFalse();
        assertThat(controllerMethod.isAnnotationPresent(RequestBody.class)).isFalse();
    }

    @Test
    void controllerDependsOnlyOnQueryServiceTrustedActorResolverAndClock() {
        assertThat(fieldTypes(ExpertQuestionAnalyticsController.class))
            .containsExactly(
                ExpertQuestionAnalyticsQueryService.class,
                InteractiveActorResolver.class,
                UtcClock.class
            )
            .doesNotContain(
                ExpertQuestionAnalyticsReadRepository.class,
                QuestionEntity.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContext.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class,
                ResultRecordingService.class,
                CapabilityAdmissionPolicy.class,
                com.vladislav.training.platform.analytics.query.ManagerialHistoricalAnalyticsQueryService.class,
                ManagerialCurrentSupervisionQueryService.class,
                SelfHistoricalResultQueryService.class,
                CourseQueryService.class,
                QuestionCommandService.class,
                QuestionLifecycleService.class
            );
    }

    @Test
    void controllerSourceDoesNotDependOnRepositoryPolicyRefreshRebuildOrForeignContours() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsController.java"
        ));

        assertThat(source)
            .doesNotContain("ExpertQuestionAnalyticsReadRepository")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("AccessPolicyQueryContext")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("SelfHistoricalResultQueryService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("CourseQueryService")
            .doesNotContain("ContentLifecycleController")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("publish")
            .doesNotContain("archive")
            .doesNotContain("refresh(")
            .doesNotContain("rebuild")
            .doesNotContain("recalculate")
            .doesNotContain("payload");
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
