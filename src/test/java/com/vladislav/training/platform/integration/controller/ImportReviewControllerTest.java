package com.vladislav.training.platform.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.service.ImportItemReviewService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code ImportReview}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class ImportReviewControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T11:00:00Z");

    @Mock
    private ImportItemReviewService importItemReviewService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(interactiveActorResolver.resolveActorUserId()).thenReturn(701L);
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ImportItemReviewController(importItemReviewService, interactiveActorResolver)
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void applyReviewRouteExistsAndReturnsDtoWithoutOwnerEntityLeak() throws Exception {
        when(importItemReviewService.applyReview(eq(701L), eq(81L), any(ImportItemReviewService.ImportReviewApplyCommand.class)))
            .thenReturn(new ImportItemReviewService.ImportReviewResult(
                81L,
                61L,
                ImportItemStatus.APPLIED,
                "9001",
                null,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));

        mockMvc.perform(post("/api/v1/admin/import-job-items/81/apply-review")
                .contentType("application/json")
                .content("""
                    {
                      "matchedUserId": 9001
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemId").value(81))
            .andExpect(jsonPath("$.importJobId").value(61))
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andExpect(jsonPath("$.matchedEntityId").value("9001"))
            .andExpect(jsonPath("$.payload").doesNotExist())
            .andExpect(jsonPath("$.appUser").doesNotExist());
    }

    @Test
    void rejectReviewRouteExistsAndReturnsDtoWithoutOwnerEntityLeak() throws Exception {
        when(importItemReviewService.rejectReview(eq(701L), eq(81L), any(ImportItemReviewService.ImportReviewRejectCommand.class)))
            .thenReturn(new ImportItemReviewService.ImportReviewResult(
                81L,
                61L,
                ImportItemStatus.FAILED,
                null,
                "REVIEW_REJECTED",
                "manual reject",
                FIXED_INSTANT,
                FIXED_INSTANT
            ));

        mockMvc.perform(post("/api/v1/admin/import-job-items/81/reject-review")
                .contentType("application/json")
                .content("""
                    {
                      "reason": "manual reject"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemId").value(81))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.errorCode").value("REVIEW_REJECTED"))
            .andExpect(jsonPath("$.payload").doesNotExist())
            .andExpect(jsonPath("$.appUser").doesNotExist());
    }

    @Test
    void actorOverrideFieldsAreRejectedAndReviewServiceIsNotCalled() throws Exception {
        mockMvc.perform(post("/api/v1/admin/import-job-items/81/apply-review")
                .contentType("application/json")
                .content("""
                    {
                      "matchedUserId": 9001,
                      "actorUserId": 999
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(importItemReviewService, never()).applyReview(any(), any(), any());
    }

    @Test
    void invalidRequestFailsFastWithoutMutation() throws Exception {
        mockMvc.perform(post("/api/v1/admin/import-job-items/81/apply-review")
                .contentType("application/json")
                .content("""
                    {
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(importItemReviewService, never()).applyReview(any(), any(), any());
    }
}
