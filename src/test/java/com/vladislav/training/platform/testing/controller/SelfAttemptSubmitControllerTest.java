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
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code SelfAttemptSubmit}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptSubmitControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T13:00:00Z");

    @Mock
    private SelfAttemptSubmitSequencingService selfAttemptSubmitOrchestrationFacade;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(new SelfAttemptSubmitController(selfAttemptSubmitOrchestrationFacade))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void submitSelfAttemptEndpointDelegatesToSelfSubmitFacadeAndReturnsSubmitResponse() throws Exception {
        when(selfAttemptSubmitOrchestrationFacade.submitSelfAttempt(9001L)).thenReturn(7001L);

        mockMvc.perform(post("/api/v1/self-attempt-submissions/attempts/9001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9001))
            .andExpect(jsonPath("$.resultId").value(7001))
            .andExpect(jsonPath("$.status").doesNotExist())
            .andExpect(jsonPath("$.attemptMode").doesNotExist())
            .andExpect(jsonPath("$.assignmentTestId").doesNotExist())
            .andExpect(jsonPath("$.testId").doesNotExist());

        verify(selfAttemptSubmitOrchestrationFacade).submitSelfAttempt(9001L);
        verifyNoMoreInteractions(selfAttemptSubmitOrchestrationFacade, utcClock);
    }

    @Test
    void whenSelfSubmitFailsWithNotFoundControllerReturnsNotFound() throws Exception {
        when(selfAttemptSubmitOrchestrationFacade.submitSelfAttempt(9002L))
            .thenThrow(new NotFoundException("Self attempt not found: testAttemptId=9002"));

        mockMvc.perform(post("/api/v1/self-attempt-submissions/attempts/9002"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Self attempt not found: testAttemptId=9002"));

        verify(selfAttemptSubmitOrchestrationFacade).submitSelfAttempt(9002L);
        verify(utcClock).now();
        verifyNoMoreInteractions(selfAttemptSubmitOrchestrationFacade, utcClock);
    }

    @Test
    void whenSelfSubmitFailsWithConflictControllerReturnsConflict() throws Exception {
        when(selfAttemptSubmitOrchestrationFacade.submitSelfAttempt(9003L))
            .thenThrow(new ConflictException("Self submit conflict: testAttemptId=9003"));

        mockMvc.perform(post("/api/v1/self-attempt-submissions/attempts/9003"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Self submit conflict: testAttemptId=9003"));

        verify(selfAttemptSubmitOrchestrationFacade).submitSelfAttempt(9003L);
        verify(utcClock).now();
        verifyNoMoreInteractions(selfAttemptSubmitOrchestrationFacade, utcClock);
    }
}
