package com.vladislav.training.platform.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignCommandService;
import com.vladislav.training.platform.assignment.service.LaunchAssignmentCampaignCommand;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code AssignmentCampaignLaunch}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentCampaignLaunchControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-10T11:00:00Z");

    @Mock
    private AssignmentCampaignCommandService assignmentCampaignCommandService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AssignmentCampaignLaunchController(assignmentCampaignCommandService)
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void launchEndpointCallsScenarioOrientedCommandServiceAndReturnsCreatedCampaign() throws Exception {
        when(assignmentCampaignCommandService.launchAssignmentCampaign(any())).thenReturn(
            new AssignmentCampaign(
                17L,
                "Запуск кампании назначений",
                "first runtime launch",
                "MANUAL",
                "launch-source-42",
                "Manual source snapshot",
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        );

        mockMvc.perform(post("/api/v1/assignment-campaigns/launch")
                .contentType("application/json")
                .content(validLaunchRequestBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.name").value("Запуск кампании назначений"))
            .andExpect(jsonPath("$.sourceType").value("MANUAL"))
            .andExpect(jsonPath("$.sourceRef").value("launch-source-42"))
            .andExpect(jsonPath("$.sourceNameSnapshot").value("Manual source snapshot"));

        ArgumentCaptor<LaunchAssignmentCampaignCommand> commandCaptor =
            ArgumentCaptor.forClass(LaunchAssignmentCampaignCommand.class);
        verify(assignmentCampaignCommandService).launchAssignmentCampaign(commandCaptor.capture());
        LaunchAssignmentCampaignCommand command = commandCaptor.getValue();

        assertThat(command.name()).isEqualTo("Запуск кампании назначений");
        assertThat(command.description()).isEqualTo("first runtime launch");
        assertThat(command.sourceType()).isEqualTo("MANUAL");
        assertThat(command.sourceRef()).isEqualTo("launch-source-42");
        assertThat(command.sourceNameSnapshot()).isEqualTo("Manual source snapshot");
        assertThat(command.courseIds()).containsExactly(501L, 502L);
        assertThat(command.targeting().basisType()).isEqualTo("ORG_UNIT");
        assertThat(command.targeting().basisRef()).isEqualTo("/company/ops");
        assertThat(command.deadlinePolicy().deadlineAt()).isEqualTo(Instant.parse("2026-04-12T11:00:00Z"));
    }

    @Test
    void launchEndpointPropagatesConflictThroughExistingExceptionMapping() throws Exception {
        when(assignmentCampaignCommandService.launchAssignmentCampaign(any()))
            .thenThrow(new ConflictException("Campaign launch conflict"));

        mockMvc.perform(post("/api/v1/assignment-campaigns/launch")
                .contentType("application/json")
                .content(validLaunchRequestBody()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Campaign launch conflict"));
    }

    @Test
    void invalidLaunchRequestFailsValidationBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/v1/assignment-campaigns/launch")
                .contentType("application/json")
                .content("""
                    {
                      "name": " ",
                      "sourceType": "MANUAL",
                      "courseIds": [501],
                      "targeting": {
                        "basisType": "ORG_UNIT",
                        "basisRef": "/company/ops"
                      },
                      "deadlinePolicy": {
                        "deadlineAt": "2026-04-12T11:00:00Z"
                      }
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    private String validLaunchRequestBody() {
        return """
            {
              "name": "Запуск кампании назначений",
              "description": "first runtime launch",
              "sourceType": "MANUAL",
              "sourceRef": "launch-source-42",
              "sourceNameSnapshot": "Manual source snapshot",
              "courseIds": [501, 502],
              "targeting": {
                "basisType": "ORG_UNIT",
                "basisRef": "/company/ops"
              },
              "deadlinePolicy": {
                "deadlineAt": "2026-04-12T11:00:00Z"
              }
            }
            """;
    }
}
