package com.vladislav.training.platform.testing.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code CurrentAttemptRead}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class CurrentAttemptReadControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T10:15:00Z");

    @Mock
    private AssignedCurrentAttemptReadService assignedCurrentAttemptReadService;
    @Mock
    private SelfCurrentAttemptReadService selfCurrentAttemptReadService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new CurrentAttemptReadController(
                    assignedCurrentAttemptReadService,
                    selfCurrentAttemptReadService,
                    interactiveActorResolver
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void assignedCurrentAttemptEndpointReturnsExistingActiveAssignedAttempt() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(assignedCurrentAttemptReadService.findCurrentAssignedAttemptForActor(41L, 71L, 501L)).thenReturn(
            activeAssignedAttempt(9001L, 501L)
        );

        mockMvc.perform(get("/api/v1/current-attempts/assigned/assignments/71/assignment-tests/501"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(9001))
            .andExpect(jsonPath("$.assignmentTestId").value(501))
            .andExpect(jsonPath("$.attemptMode").value("ASSIGNED"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.testId").value(301))
            .andExpect(jsonPath("$.resultId").doesNotExist());

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignedCurrentAttemptReadService).findCurrentAssignedAttemptForActor(41L, 71L, 501L);
        verifyNoMoreInteractions(
            assignedCurrentAttemptReadService,
            selfCurrentAttemptReadService,
            interactiveActorResolver
        );
    }

    @Test
    void selfCurrentAttemptEndpointReturnsExistingActiveSelfAttemptForCurrentActor() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(77L);
        when(selfCurrentAttemptReadService.findCurrentSelfAttemptForActor(77L, 301L)).thenReturn(
            activeSelfAttempt(9002L, 77L, 301L)
        );

        mockMvc.perform(get("/api/v1/current-attempts/self/tests/301"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(9002))
            .andExpect(jsonPath("$.assignmentTestId").isEmpty())
            .andExpect(jsonPath("$.attemptMode").value("SELF"))
            .andExpect(jsonPath("$.status").value("STARTED"))
            .andExpect(jsonPath("$.userId").value(77));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(selfCurrentAttemptReadService).findCurrentSelfAttemptForActor(77L, 301L);
        verifyNoMoreInteractions(
            assignedCurrentAttemptReadService,
            selfCurrentAttemptReadService,
            interactiveActorResolver
        );
    }

    @Test
    void whenAssignedActiveAttemptIsMissingControllerReturnsNotFound() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(assignedCurrentAttemptReadService.findCurrentAssignedAttemptForActor(41L, 71L, 999L))
            .thenThrow(new NotFoundException("Active assigned attempt not found for assignmentTestId=999"));

        mockMvc.perform(get("/api/v1/current-attempts/assigned/assignments/71/assignment-tests/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Active assigned attempt not found for assignmentTestId=999"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignedCurrentAttemptReadService).findCurrentAssignedAttemptForActor(41L, 71L, 999L);
        verifyNoMoreInteractions(
            assignedCurrentAttemptReadService,
            selfCurrentAttemptReadService,
            interactiveActorResolver
        );
    }

    @Test
    void foreignAssignmentLinkedReadDoesNotReachTestingLookupWhenGatewayDoesNotResolveAssignmentTest() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(assignedCurrentAttemptReadService.findCurrentAssignedAttemptForActor(41L, 71L, 999L))
            .thenThrow(new NotFoundException(
                "Assignment test not found in self-scoped assignment context: assignmentId=71, assignmentTestId=999"
            ));

        mockMvc.perform(get("/api/v1/current-attempts/assigned/assignments/71/assignment-tests/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Assignment test not found in self-scoped assignment context: assignmentId=71, assignmentTestId=999"
            ));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignedCurrentAttemptReadService).findCurrentAssignedAttemptForActor(41L, 71L, 999L);
        verifyNoMoreInteractions(
            assignedCurrentAttemptReadService,
            selfCurrentAttemptReadService,
            interactiveActorResolver
        );
    }

    @Test
    void assignedGatewayNotFoundStopsBeforeTestingLookup() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(assignedCurrentAttemptReadService.findCurrentAssignedAttemptForActor(41L, 71L, 999L))
            .thenThrow(new NotFoundException("Assignment not found in self scope: actorUserId=41, assignmentId=71"));

        mockMvc.perform(get("/api/v1/current-attempts/assigned/assignments/71/assignment-tests/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Assignment not found in self scope: actorUserId=41, assignmentId=71"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignedCurrentAttemptReadService).findCurrentAssignedAttemptForActor(41L, 71L, 999L);
        verifyNoMoreInteractions(
            assignedCurrentAttemptReadService,
            selfCurrentAttemptReadService,
            interactiveActorResolver
        );
    }

    @Test
    void whenSelfActiveAttemptIsMissingControllerReturnsNotFound() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(88L);
        when(selfCurrentAttemptReadService.findCurrentSelfAttemptForActor(88L, 777L))
            .thenThrow(new NotFoundException("Active self attempt not found for testId=777"));

        mockMvc.perform(get("/api/v1/current-attempts/self/tests/777"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Active self attempt not found for testId=777"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(selfCurrentAttemptReadService).findCurrentSelfAttemptForActor(88L, 777L);
        verifyNoMoreInteractions(
            assignedCurrentAttemptReadService,
            selfCurrentAttemptReadService,
            interactiveActorResolver
        );
    }

    @Test
    void selfCurrentAttemptFoundationMissingStopsBeforeTestingLookup() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(88L);
        when(selfCurrentAttemptReadService.findCurrentSelfAttemptForActor(88L, 777L))
            .thenThrow(new NotFoundException("Self current attempt foundation not found for testId=777"));

        mockMvc.perform(get("/api/v1/current-attempts/self/tests/777"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Self current attempt foundation not found for testId=777"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(selfCurrentAttemptReadService).findCurrentSelfAttemptForActor(88L, 777L);
        verifyNoMoreInteractions(
            assignedCurrentAttemptReadService,
            selfCurrentAttemptReadService,
            interactiveActorResolver
        );
    }

    private TestAttempt activeAssignedAttempt(Long attemptId, Long assignmentTestId) {
        return new TestAttempt(
            attemptId,
            41L,
            301L,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.IN_PROGRESS,
            FIXED_INSTANT.minusSeconds(600),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(30)
        );
    }

    private TestAttempt activeSelfAttempt(Long attemptId, Long userId, Long testId) {
        return new TestAttempt(
            attemptId,
            userId,
            testId,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.STARTED,
            FIXED_INSTANT.minusSeconds(300),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT.minusSeconds(10)
        );
    }
}
