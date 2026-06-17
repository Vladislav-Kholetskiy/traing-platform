package com.vladislav.training.platform.testing.controller;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code SelfAttemptAbandon}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptAbandonControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T13:20:00Z");

    @Mock
    private SelfAttemptAbandonSequencingService selfAttemptAbandonOrchestrationFacade;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(new SelfAttemptAbandonController(selfAttemptAbandonOrchestrationFacade))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void abandonSelfAttemptEndpointDelegatesToSelfAbandonFacadeAndReturnsAbandonResponse() throws Exception {
        when(selfAttemptAbandonOrchestrationFacade.abandonSelfAttempt(9001L)).thenReturn(9001L);

        mockMvc.perform(post("/api/v1/self-attempt-abandonments/attempts/9001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9001))
            .andExpect(jsonPath("$.resultId").doesNotExist())
            .andExpect(jsonPath("$.status").doesNotExist())
            .andExpect(jsonPath("$.attemptMode").doesNotExist());

        verify(selfAttemptAbandonOrchestrationFacade).abandonSelfAttempt(9001L);
        verifyNoMoreInteractions(selfAttemptAbandonOrchestrationFacade, utcClock);
    }

    @Test
    void whenSelfAbandonFailsWithNotFoundControllerReturnsNotFound() throws Exception {
        when(selfAttemptAbandonOrchestrationFacade.abandonSelfAttempt(9002L))
            .thenThrow(new NotFoundException("Self attempt not found for abandon: testAttemptId=9002"));

        mockMvc.perform(post("/api/v1/self-attempt-abandonments/attempts/9002"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Self attempt not found for abandon: testAttemptId=9002"));

        verify(selfAttemptAbandonOrchestrationFacade).abandonSelfAttempt(9002L);
        verify(utcClock).now();
        verifyNoMoreInteractions(selfAttemptAbandonOrchestrationFacade, utcClock);
    }

    @Test
    void whenSelfAbandonFailsWithConflictControllerReturnsConflict() throws Exception {
        when(selfAttemptAbandonOrchestrationFacade.abandonSelfAttempt(9003L))
            .thenThrow(new ConflictException("Self abandon conflict: testAttemptId=9003"));

        mockMvc.perform(post("/api/v1/self-attempt-abandonments/attempts/9003"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Self abandon conflict: testAttemptId=9003"));

        verify(selfAttemptAbandonOrchestrationFacade).abandonSelfAttempt(9003L);
        verify(utcClock).now();
        verifyNoMoreInteractions(selfAttemptAbandonOrchestrationFacade, utcClock);
    }
}
