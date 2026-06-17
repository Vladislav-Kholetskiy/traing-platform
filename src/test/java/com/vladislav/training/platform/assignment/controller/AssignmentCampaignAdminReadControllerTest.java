package com.vladislav.training.platform.assignment.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignQueryService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
/**
 * Проверяет поведение контроллера {@code AssignmentCampaignAdminRead}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
class AssignmentCampaignAdminReadControllerTest {

    @Mock
    private AssignmentCampaignQueryService assignmentCampaignQueryService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(Instant.parse("2026-06-07T09:00:00Z"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AssignmentCampaignAdminReadController(assignmentCampaignQueryService)
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void listEndpointReturnsAssignmentCampaigns() throws Exception {
        when(assignmentCampaignQueryService.findAllAssignmentCampaigns()).thenReturn(List.of(
            new AssignmentCampaign(
                11L,
                "Campaign A",
                "desc",
                "ORG_UNIT",
                "/plant/a",
                "Plant A",
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T10:00:00Z")
            )
        ));

        mockMvc.perform(get("/api/v1/admin/assignment-campaigns"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(11))
            .andExpect(jsonPath("$[0].name").value("Campaign A"));

        verify(assignmentCampaignQueryService).findAllAssignmentCampaigns();
    }

    @Test
    void detailEndpointReturnsAssignmentCampaignById() throws Exception {
        when(assignmentCampaignQueryService.findAssignmentCampaignById(17L)).thenReturn(
            new AssignmentCampaign(
                17L,
                "Campaign B",
                "desc",
                "ORG_UNIT",
                "/plant/b",
                "Plant B",
                Instant.parse("2026-06-02T10:00:00Z"),
                Instant.parse("2026-06-02T10:00:00Z")
            )
        );

        mockMvc.perform(get("/api/v1/admin/assignment-campaigns/17"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.sourceRef").value("/plant/b"));
    }
}
