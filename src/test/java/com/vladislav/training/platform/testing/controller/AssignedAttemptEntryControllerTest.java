package com.vladislav.training.platform.testing.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
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
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code AssignedAttemptEntry}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptEntryControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T12:00:00Z");

    @Mock
    private AssignedAttemptEntryService assignedAttemptEntryService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(new AssignedAttemptEntryController(assignedAttemptEntryService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void enterAssignedAttemptEndpointDelegatesToAssignedEntryServiceAndReturnsEntryResponse() throws Exception {
        when(assignedAttemptEntryService.enterAssignedAttempt(77L, 701L))
            .thenReturn(startedAttempt(9001L, 701L, 301L, TestAttemptStatus.STARTED));

        mockMvc.perform(post("/api/v1/assigned-attempt-entries/assignments/77/assignment-tests/701"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9001))
            .andExpect(jsonPath("$.assignmentTestId").value(701))
            .andExpect(jsonPath("$.testId").value(301))
            .andExpect(jsonPath("$.attemptMode").value("ASSIGNED"))
            .andExpect(jsonPath("$.status").value("STARTED"))
            .andExpect(jsonPath("$.startedAt").value("2026-04-20T11:50:00Z"))
            .andExpect(jsonPath("$.lastActivityAt").value("2026-04-20T11:59:30Z"))
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.questionId").doesNotExist())
            .andExpect(jsonPath("$.resultId").doesNotExist());

        verify(assignedAttemptEntryService).enterAssignedAttempt(77L, 701L);
        verifyNoMoreInteractions(assignedAttemptEntryService, utcClock);
    }

    @Test
    void whenAssignedEntryFailsWithNotFoundControllerReturnsNotFound() throws Exception {
        when(assignedAttemptEntryService.enterAssignedAttempt(77L, 701L))
            .thenThrow(new NotFoundException("Assigned execution foundation not found: assignmentId=77, assignmentTestId=701"));

        mockMvc.perform(post("/api/v1/assigned-attempt-entries/assignments/77/assignment-tests/701"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Assigned execution foundation not found: assignmentId=77, assignmentTestId=701"
            ));

        verify(assignedAttemptEntryService).enterAssignedAttempt(77L, 701L);
        verify(utcClock).now();
        verifyNoMoreInteractions(assignedAttemptEntryService, utcClock);
    }

    @Test
    void whenAssignedEntryFailsWithConflictControllerReturnsConflict() throws Exception {
        when(assignedAttemptEntryService.enterAssignedAttempt(77L, 701L))
            .thenThrow(new ConflictException("Active assigned attempt is inconsistent with assignment anchor: assignmentTestId=701"));

        mockMvc.perform(post("/api/v1/assigned-attempt-entries/assignments/77/assignment-tests/701"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(
                "Active assigned attempt is inconsistent with assignment anchor: assignmentTestId=701"
            ));

        verify(assignedAttemptEntryService).enterAssignedAttempt(77L, 701L);
        verify(utcClock).now();
        verifyNoMoreInteractions(assignedAttemptEntryService, utcClock);
    }

    private TestAttempt startedAttempt(Long attemptId, Long assignmentTestId, Long testId, TestAttemptStatus status) {
        return new TestAttempt(
            attemptId,
            41L,
            testId,
            assignmentTestId,
            AttemptMode.ASSIGNED,
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
