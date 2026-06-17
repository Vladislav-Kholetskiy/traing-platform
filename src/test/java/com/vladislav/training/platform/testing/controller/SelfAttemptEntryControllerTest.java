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
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code SelfAttemptEntry}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptEntryControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T12:20:00Z");

    @Mock
    private SelfAttemptEntryService selfAttemptEntryService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(new SelfAttemptEntryController(selfAttemptEntryService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void startOrContinueSelfAttemptEndpointDelegatesToSelfEntryServiceAndReturnsEntryResponse() throws Exception {
        when(selfAttemptEntryService.startOrContinueSelfAttempt(301L))
            .thenReturn(startedAttempt(9002L, 301L, TestAttemptStatus.STARTED));

        mockMvc.perform(post("/api/v1/self-attempt-entries/tests/301"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9002))
            .andExpect(jsonPath("$.testId").value(301))
            .andExpect(jsonPath("$.attemptMode").value("SELF"))
            .andExpect(jsonPath("$.status").value("STARTED"))
            .andExpect(jsonPath("$.startedAt").value("2026-04-20T12:10:00Z"))
            .andExpect(jsonPath("$.lastActivityAt").value("2026-04-20T12:19:30Z"))
            .andExpect(jsonPath("$.assignmentTestId").doesNotExist())
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.questionId").doesNotExist())
            .andExpect(jsonPath("$.resultId").doesNotExist());

        verify(selfAttemptEntryService).startOrContinueSelfAttempt(301L);
        verifyNoMoreInteractions(selfAttemptEntryService, utcClock);
    }

    @Test
    void whenSelfEntryFailsWithNotFoundControllerReturnsNotFound() throws Exception {
        when(selfAttemptEntryService.startOrContinueSelfAttempt(301L))
            .thenThrow(new NotFoundException("Self execution foundation not found: testId=301"));

        mockMvc.perform(post("/api/v1/self-attempt-entries/tests/301"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Self execution foundation not found: testId=301"));

        verify(selfAttemptEntryService).startOrContinueSelfAttempt(301L);
        verify(utcClock).now();
        verifyNoMoreInteractions(selfAttemptEntryService, utcClock);
    }

    @Test
    void whenSelfEntryFailsWithConflictControllerReturnsConflict() throws Exception {
        when(selfAttemptEntryService.startOrContinueSelfAttempt(301L))
            .thenThrow(new ConflictException("Active self attempt is inconsistent with self anchor: testId=301"));

        mockMvc.perform(post("/api/v1/self-attempt-entries/tests/301"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(
                "Active self attempt is inconsistent with self anchor: testId=301"
            ));

        verify(selfAttemptEntryService).startOrContinueSelfAttempt(301L);
        verify(utcClock).now();
        verifyNoMoreInteractions(selfAttemptEntryService, utcClock);
    }

    private TestAttempt startedAttempt(Long attemptId, Long testId, TestAttemptStatus status) {
        return new TestAttempt(
            attemptId,
            41L,
            testId,
            null,
            AttemptMode.SELF,
            status,
            FIXED_INSTANT.minusSeconds(600),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(30)
        );
    }
}
