package com.vladislav.training.platform.analytics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.analytics.controller.dto.AnalyticsResultRebuildRequest;
import com.vladislav.training.platform.analytics.service.AnalyticsAdminRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsResultRebuildOutcome;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code AnalyticsAdminRebuild}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsAdminRebuildControllerTest {

    private static final Path CONTROLLER_SOURCE =
        Path.of("src/main/java/com/vladislav/training/platform/analytics/controller/AnalyticsAdminRebuildController.java");

    private static final Instant PERIOD_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private AnalyticsAdminRebuildService analyticsAdminRebuildService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AnalyticsAdminRebuildController(analyticsAdminRebuildService, interactiveActorResolver)
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void rebuildRouteUsesTrustedActorAndReturnsTechnicalOutcome() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(1L);
        when(analyticsAdminRebuildService.rebuildResultAnalytics(1L, PERIOD_START, PERIOD_END))
            .thenReturn(new AnalyticsResultRebuildOutcome(
                PERIOD_START,
                PERIOD_END,
                12L,
                12L,
                0L,
                3L,
                2L,
                12L,
                List.of()
            ));

        mockMvc.perform(post("/api/v1/admin/analytics/result-rebuild")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new AnalyticsResultRebuildRequest(PERIOD_START, PERIOD_END))))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.periodStart").value(PERIOD_START.toString()))
            .andExpect(jsonPath("$.periodEnd").value(PERIOD_END.toString()))
            .andExpect(jsonPath("$.sourceRowCount").value(12))
            .andExpect(jsonPath("$.supportedTopicRowCount").value(12))
            .andExpect(jsonPath("$.unsupportedTopicRowCount").value(0))
            .andExpect(jsonPath("$.userTopicAggregateRowCount").value(3))
            .andExpect(jsonPath("$.departmentTopicAggregateRowCount").value(2))
            .andExpect(jsonPath("$.questionAggregateRowCount").value(12));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(analyticsAdminRebuildService).rebuildResultAnalytics(1L, PERIOD_START, PERIOD_END);
    }

    @Test
    void controllerRemainsNarrowExplicitMaintenanceMaintenanceSurface() throws Exception {
        String source = Files.readString(CONTROLLER_SOURCE);

        assertThat(source)
            .contains("@RequestMapping(\"/api/v1/admin/analytics\")")
            .contains("@PostMapping(\"/result-rebuild\")")
            .doesNotContain("@GetMapping(\"/result-rebuild\")")
            .doesNotContain("repair")
            .doesNotContain("recovery")
            .doesNotContain("reconcile")
            .doesNotContain("backfill")
            .doesNotContain("replay")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AttemptStatusRecalculationService")
            .doesNotContain("ResultRecordingService");
    }
}
