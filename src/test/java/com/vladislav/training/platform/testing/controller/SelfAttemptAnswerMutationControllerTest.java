package com.vladislav.training.platform.testing.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.service.SelfAttemptAnswerMutationEntryService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code SelfAttemptAnswerMutation}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptAnswerMutationControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T11:00:00Z");

    @Mock
    private SelfAttemptAnswerMutationEntryService selfAttemptAnswerMutationEntryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new SelfAttemptAnswerMutationController(selfAttemptAnswerMutationEntryService)
            ).setControllerAdvice(new com.vladislav.training.platform.common.web.GlobalExceptionHandler(
                () -> FIXED_INSTANT
            ))
            .build();
    }

    @Test
    void saveOrReplaceSelfAnswerDelegatesToEntryServiceAndReturnsMutationResponse() throws Exception {
        when(selfAttemptAnswerMutationEntryService.saveOrReplaceSelfAnswer(
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(updatedAttempt(101L, TestAttemptStatus.IN_PROGRESS));

        mockMvc.perform(put("/api/v1/self-attempt-answers/attempts/101/questions/501")
                .contentType("application/json")
                .content("""
                    {
                      "answerItems": [
                        { "answerOptionId": 7002 },
                        { "answerOptionId": 7001 }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(101))
            .andExpect(jsonPath("$.questionId").value(501))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.lastActivityAt").value("2026-04-20T11:00:00Z"));

        verify(selfAttemptAnswerMutationEntryService).saveOrReplaceSelfAnswer(
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.any()
        );
        verifyNoMoreInteractions(selfAttemptAnswerMutationEntryService);
    }

    @Test
    void clearSelfAnswerDelegatesToEntryServiceAndReturnsMutationResponse() throws Exception {
        when(selfAttemptAnswerMutationEntryService.clearSelfAnswer(101L, 501L))
            .thenReturn(updatedAttempt(101L, TestAttemptStatus.IN_PROGRESS));

        mockMvc.perform(delete("/api/v1/self-attempt-answers/attempts/101/questions/501"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(101))
            .andExpect(jsonPath("$.questionId").value(501))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.lastActivityAt").value("2026-04-20T11:00:00Z"));

        verify(selfAttemptAnswerMutationEntryService).clearSelfAnswer(101L, 501L);
        verifyNoMoreInteractions(selfAttemptAnswerMutationEntryService);
    }

    @Test
    void whenSaveOrReplaceSelfAnswerFailsControllerReturnsConflict() throws Exception {
        when(selfAttemptAnswerMutationEntryService.saveOrReplaceSelfAnswer(
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.any()
        )).thenThrow(new ConflictException("Answer mutation conflict"));

        mockMvc.perform(put("/api/v1/self-attempt-answers/attempts/101/questions/501")
                .contentType("application/json")
                .content("""
                    {
                      "answerItems": [
                        { "answerOptionId": 7001 }
                      ]
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Answer mutation conflict"));
    }

    @Test
    void whenClearSelfAnswerFailsControllerReturnsConflict() throws Exception {
        when(selfAttemptAnswerMutationEntryService.clearSelfAnswer(101L, 501L))
            .thenThrow(new ConflictException("Clear answer conflict"));

        mockMvc.perform(delete("/api/v1/self-attempt-answers/attempts/101/questions/501"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Clear answer conflict"));
    }

    private TestAttempt updatedAttempt(Long attemptId, TestAttemptStatus status) {
        return new TestAttempt(
            attemptId,
            41L,
            301L,
            null,
            AttemptMode.SELF,
            status,
            FIXED_INSTANT.minusSeconds(300),
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT
        );
    }
}
