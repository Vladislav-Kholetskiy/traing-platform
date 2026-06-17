package com.vladislav.training.platform.content.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.service.TestQueryService;
import com.vladislav.training.platform.content.service.TopicFinalControlService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code TopicFinalControl}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class TopicFinalControlControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock
    private TopicFinalControlService topicFinalControlService;
    @Mock
    private TestQueryService testQueryService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TopicFinalControlController(topicFinalControlService, testQueryService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void getActiveFinalReturnsNotFoundWhenAbsent() throws Exception {
        when(testQueryService.findActiveFinalTestByTopicId(20L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/expert/content/topics/20/active-final-test"))
            .andExpect(status().isNotFound());

        verify(testQueryService).findActiveFinalTestByTopicId(20L);
        verifyNoInteractions(topicFinalControlService);
    }

    @Test
    void assignDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/expert/content/topics/20/active-final-tests/30/assign"))
            .andExpect(status().isOk());

        verify(topicFinalControlService).assignActiveFinalTest(20L, 30L);
        verifyNoInteractions(testQueryService);
    }

    @Test
    void replaceDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/expert/content/topics/20/active-final-tests/30/replace"))
            .andExpect(status().isOk());

        verify(topicFinalControlService).replaceActiveFinalTest(20L, 30L);
        verifyNoInteractions(testQueryService);
    }

    @Test
    void clearDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/expert/content/topics/20/active-final-test"))
            .andExpect(status().isOk());

        verify(topicFinalControlService).clearActiveFinalTest(20L);
        verifyNoInteractions(testQueryService);
    }

    @Test
    void getActiveFinalReturnsBodyWhenPresent() throws Exception {
        when(testQueryService.findActiveFinalTestByTopicId(20L)).thenReturn(Optional.of(
            new com.vladislav.training.platform.content.domain.Test(30L, 20L, "Final", null, TestType.CONTROL, ContentStatus.PUBLISHED, BigDecimal.valueOf(80), "DEFAULT", true, 0, FIXED_INSTANT, FIXED_INSTANT)
        ));

        mockMvc.perform(get("/api/v1/expert/content/topics/20/active-final-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(30))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(testQueryService).findActiveFinalTestByTopicId(20L);
        verifyNoInteractions(topicFinalControlService);
    }

    @Test
    void getEligibleFinalControlTestsReturnsList() throws Exception {
        when(testQueryService.findEligibleFinalControlTestsByTopicId(20L)).thenReturn(List.of(
            new com.vladislav.training.platform.content.domain.Test(31L, 20L, "Eligible", null, TestType.CONTROL, ContentStatus.PUBLISHED, BigDecimal.valueOf(70), "DEFAULT", false, 1, FIXED_INSTANT, FIXED_INSTANT)
        ));

        mockMvc.perform(get("/api/v1/expert/content/topics/20/tests").param("eligibleForFinalControl", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(31))
            .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(testQueryService).findEligibleFinalControlTestsByTopicId(20L);
        verifyNoInteractions(topicFinalControlService);
    }
}
