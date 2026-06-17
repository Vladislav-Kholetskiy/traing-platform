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
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code AssignedAttemptSubmit}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptSubmitControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T12:40:00Z");

    @Mock
    private AssignedAttemptSubmissionService assignedAttemptSubmissionService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(new AssignedAttemptSubmitController(assignedAttemptSubmissionService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void completedAssignedSubmitResponseUsesExplicitOutcomeContract() throws Exception {
        when(assignedAttemptSubmissionService.submitAssignedAttempt(9001L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9001L, 7001L));

        mockMvc.perform(post("/api/v1/assigned-attempt-submissions/attempts/9001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9001))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.resultId").value(7001))
            .andExpect(jsonPath("$.attemptMode").doesNotExist())
            .andExpect(jsonPath("$.assignmentTestId").doesNotExist())
            .andExpect(jsonPath("$.testId").doesNotExist());

        verify(assignedAttemptSubmissionService).submitAssignedAttempt(9001L);
        verifyNoMoreInteractions(assignedAttemptSubmissionService, utcClock);
    }

    @Test
    void controllerBuildsAssignedSubmitResponseFromOutcomeAttemptIdInsteadOfPathVariable() throws Exception {
        when(assignedAttemptSubmissionService.submitAssignedAttempt(9001L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9100L, 7001L));

        mockMvc.perform(post("/api/v1/assigned-attempt-submissions/attempts/9001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9100))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.resultId").value(7001));

        verify(assignedAttemptSubmissionService).submitAssignedAttempt(9001L);
        verifyNoMoreInteractions(assignedAttemptSubmissionService, utcClock);
    }

    @Test
    void expiredAssignedSubmitResponseUsesExplicitTerminalOutcomeContract() throws Exception {
        when(assignedAttemptSubmissionService.submitAssignedAttempt(9004L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.expired(9004L));

        mockMvc.perform(post("/api/v1/assigned-attempt-submissions/attempts/9004"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9004))
            .andExpect(jsonPath("$.status").value("EXPIRED"))
            .andExpect(jsonPath("$.resultId").isEmpty());

        verify(assignedAttemptSubmissionService).submitAssignedAttempt(9004L);
        verifyNoMoreInteractions(assignedAttemptSubmissionService, utcClock);
    }

    @Test
    void whenAssignedSubmitFailsWithNotFoundControllerReturnsNotFound() throws Exception {
        when(assignedAttemptSubmissionService.submitAssignedAttempt(9002L))
            .thenThrow(new NotFoundException("Assigned attempt not found: testAttemptId=9002"));

        mockMvc.perform(post("/api/v1/assigned-attempt-submissions/attempts/9002"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Assigned attempt not found: testAttemptId=9002"));

        verify(assignedAttemptSubmissionService).submitAssignedAttempt(9002L);
        verify(utcClock).now();
        verifyNoMoreInteractions(assignedAttemptSubmissionService, utcClock);
    }

    @Test
    void whenAssignedSubmitFailsWithConflictControllerReturnsConflict() throws Exception {
        when(assignedAttemptSubmissionService.submitAssignedAttempt(9003L))
            .thenThrow(new ConflictException("Assigned submit conflict: testAttemptId=9003"));

        mockMvc.perform(post("/api/v1/assigned-attempt-submissions/attempts/9003"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Assigned submit conflict: testAttemptId=9003"));

        verify(assignedAttemptSubmissionService).submitAssignedAttempt(9003L);
        verify(utcClock).now();
        verifyNoMoreInteractions(assignedAttemptSubmissionService, utcClock);
    }
}
