package com.vladislav.training.platform.analytics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.analytics.query.ExpertQuestionAnalyticsQueryService;
import com.vladislav.training.platform.analytics.query.ManagerialDepartmentTopicAnalyticsDto;
import com.vladislav.training.platform.analytics.query.ManagerialHistoricalAnalyticsQueryService;
import com.vladislav.training.platform.analytics.query.ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery;
import com.vladislav.training.platform.analytics.query.ManagerialUserTopicAnalyticsDto;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.assignment.service.ManagerialCurrentSupervisionQueryService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
/**
 * Проверяет поведение контроллера {@code ManagerialHistoricalAnalytics}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class ManagerialHistoricalAnalyticsControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-01T09:15:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");

    @Mock
    private ManagerialHistoricalAnalyticsQueryService managerialHistoricalAnalyticsQueryService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ManagerialHistoricalAnalyticsController(
                    managerialHistoricalAnalyticsQueryService,
                    interactiveActorResolver,
                    utcClock
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void userTopicRouteUsesTrustedActorClockAndPassesPeriodToQueryService() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(managerialHistoricalAnalyticsQueryService.findUserTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(101L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        )).thenReturn(List.of(
            new ManagerialUserTopicAnalyticsDto(
                201L,
                "NPZ-OP-201",
                "Иванов Иван Иванович",
                501L,
                "Охрана труда",
                PERIOD_START,
                PERIOD_END,
                new java.math.BigDecimal("84.2500"),
                new java.math.BigDecimal("91.5000"),
                12,
                3,
                Instant.parse("2026-05-01T00:10:00Z"),
                Instant.parse("2026-05-01T00:15:00Z")
            )
        ));

        mockMvc.perform(get("/api/v1/managerial/historical-analytics/user-topic")
                .queryParam("periodStart", PERIOD_START.toString())
                .queryParam("periodEnd", PERIOD_END.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(201))
            .andExpect(jsonPath("$[0].userEmployeeNumber").value("NPZ-OP-201"))
            .andExpect(jsonPath("$[0].userDisplayName").value("Иванов Иван Иванович"))
            .andExpect(jsonPath("$[0].topicId").value(501))
            .andExpect(jsonPath("$[0].topicName").value("Охрана труда"))
            .andExpect(jsonPath("$[0].periodStart").value(PERIOD_START.toString()))
            .andExpect(jsonPath("$[0].periodEnd").value(PERIOD_END.toString()))
            .andExpect(jsonPath("$[0].averageScorePercent").value(84.2500))
            .andExpect(jsonPath("$[0].passRatePercent").value(91.5000))
            .andExpect(jsonPath("$[0].attemptCount").value(12))
            .andExpect(jsonPath("$[0].errorCount").value(3));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
        verify(managerialHistoricalAnalyticsQueryService).findUserTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(101L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        );
    }

    @Test
    void departmentTopicRouteUsesTrustedActorClockAndPassesPeriodToQueryService() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(managerialHistoricalAnalyticsQueryService.findDepartmentTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(101L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        )).thenReturn(List.of(
            new ManagerialDepartmentTopicAnalyticsDto(
                42L,
                "Установка по производству",
                "/company/division/department",
                501L,
                "Промышленная безопасность",
                PERIOD_START,
                PERIOD_END,
                new java.math.BigDecimal("77.2500"),
                new java.math.BigDecimal("61.5000"),
                25,
                4,
                Instant.parse("2026-05-01T00:10:00Z"),
                Instant.parse("2026-05-01T00:15:00Z")
            )
        ));

        mockMvc.perform(get("/api/v1/managerial/historical-analytics/department-topic")
                .queryParam("periodStart", PERIOD_START.toString())
                .queryParam("periodEnd", PERIOD_END.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].organizationalUnitIdSnapshot").value(42))
            .andExpect(jsonPath("$[0].organizationalUnitName").value("Установка по производству"))
            .andExpect(jsonPath("$[0].organizationalPathSnapshot").value("/company/division/department"))
            .andExpect(jsonPath("$[0].topicId").value(501))
            .andExpect(jsonPath("$[0].topicName").value("Промышленная безопасность"))
            .andExpect(jsonPath("$[0].periodStart").value(PERIOD_START.toString()))
            .andExpect(jsonPath("$[0].periodEnd").value(PERIOD_END.toString()));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
        verify(managerialHistoricalAnalyticsQueryService).findDepartmentTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(101L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        );
    }

    @Test
    void overrideLikeRequestParamsDoNotChangeTrustedActorOrBehavior() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(managerialHistoricalAnalyticsQueryService.findUserTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(101L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        )).thenReturn(List.of(
            new ManagerialUserTopicAnalyticsDto(
                201L,
                "NPZ-OP-201",
                "Иванов Иван Иванович",
                501L,
                "Охрана труда",
                PERIOD_START,
                PERIOD_END,
                new java.math.BigDecimal("84.2500"),
                new java.math.BigDecimal("91.5000"),
                12,
                3,
                Instant.parse("2026-05-01T00:10:00Z"),
                Instant.parse("2026-05-01T00:15:00Z")
            )
        ));

        mockMvc.perform(get("/api/v1/managerial/historical-analytics/user-topic")
                .queryParam("periodStart", PERIOD_START.toString())
                .queryParam("periodEnd", PERIOD_END.toString())
                .queryParam("actorUserId", "999")
                .queryParam("managerUserId", "999")
                .queryParam("targetUserId", "999")
                .queryParam("scope", "FULL")
                .queryParam("scopeOverride", "FULL")
                .queryParam("organizationalUnitIds", "999")
                .queryParam("subtreePaths", "/root/finance")
                .queryParam("rebuild", "true")
                .queryParam("refresh", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(201));

        verify(managerialHistoricalAnalyticsQueryService).findUserTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(101L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        );
        verify(managerialHistoricalAnalyticsQueryService, never()).findUserTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(999L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        );
    }

    @Test
    void policyViolationIsReturnedAsForbidden() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(managerialHistoricalAnalyticsQueryService.findDepartmentTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(101L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        )).thenThrow(new PolicyViolationException(
            "MANAGERIAL_HISTORICAL_ANALYTICS_DENIED",
            "Managerial historical analytics denied"
        ));

        mockMvc.perform(get("/api/v1/managerial/historical-analytics/department-topic")
                .queryParam("periodStart", PERIOD_START.toString())
                .queryParam("periodEnd", PERIOD_END.toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Managerial historical analytics denied"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock, times(2)).now();
        verify(managerialHistoricalAnalyticsQueryService).findDepartmentTopicAnalytics(
            new ManagerialHistoricalAnalyticsQuery(101L, FIXED_INSTANT, PERIOD_START, PERIOD_END)
        );
    }

    @Test
    void controllerUsesExistingPublicQueryDtos() throws Exception {
        Method userMethod = ManagerialHistoricalAnalyticsController.class.getDeclaredMethod(
            "findUserTopicAnalytics",
            Instant.class,
            Instant.class
        );
        Method departmentMethod = ManagerialHistoricalAnalyticsController.class.getDeclaredMethod(
            "findDepartmentTopicAnalytics",
            Instant.class,
            Instant.class
        );

        assertThat(userMethod.getReturnType()).isEqualTo(List.class);
        assertThat(departmentMethod.getReturnType()).isEqualTo(List.class);
        assertThat(userMethod.getGenericReturnType()).isInstanceOf(ParameterizedType.class);
        assertThat(departmentMethod.getGenericReturnType()).isInstanceOf(ParameterizedType.class);

        Type userDtoType = ((ParameterizedType) userMethod.getGenericReturnType()).getActualTypeArguments()[0];
        Type departmentDtoType = ((ParameterizedType) departmentMethod.getGenericReturnType()).getActualTypeArguments()[0];

        assertThat(userDtoType).isEqualTo(ManagerialUserTopicAnalyticsDto.class);
        assertThat(departmentDtoType).isEqualTo(ManagerialDepartmentTopicAnalyticsDto.class);
    }

    @Test
    void controllerDependsOnlyOnQueryServiceTrustedActorResolverAndClock() {
        assertThat(fieldTypes(ManagerialHistoricalAnalyticsController.class))
            .containsExactly(
                ManagerialHistoricalAnalyticsQueryService.class,
                InteractiveActorResolver.class,
                UtcClock.class
            )
            .doesNotContain(
                ManagerialHistoricalAnalyticsReadRepository.class,
                ManagerialCurrentSupervisionQueryService.class,
                ExpertQuestionAnalyticsQueryService.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class,
                AssignmentCommandService.class,
                AssignmentStatusRecalculationService.class,
                ResultRecordingService.class,
                CapabilityAdmissionPolicy.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContext.class,
                ActiveAttemptAnswerMutationService.class
            );
    }

    @Test
    void controllerSourceDoesNotCallRepositoryPolicyOrMutationServices() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsController.java"
        ));

        assertThat(source)
            .doesNotContain("ManagerialHistoricalAnalyticsReadRepository")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("recalculate")
            .doesNotContain("refresh(")
            .doesNotContain("rebuild(");
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
