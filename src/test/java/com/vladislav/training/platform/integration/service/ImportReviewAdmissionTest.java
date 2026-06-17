package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.repository.ImportJobItemRepository;
import com.vladislav.training.platform.integration.repository.ImportJobRepository;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.service.UserCommandService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
/**
 * Проверяет поведение {@code ImportReviewAdmission}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportReviewAdmissionTest {

    @Test
    void applyReviewChecksAdmissionBeforeAnyMutation() {
        ImportJobRepository jobRepository = Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = Mockito.mock(UserCommandService.class);
        CapabilityAdmissionPolicy capabilityAdmissionPolicy = Mockito.mock(CapabilityAdmissionPolicy.class);
        CapabilityAdmissionRequestFactory requestFactory = Mockito.mock(CapabilityAdmissionRequestFactory.class);
        UtcClock utcClock = Mockito.mock(UtcClock.class);
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            701L,
            "IMPORT_ITEM_REVIEW_APPLY",
            com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            81L,
            com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
            Instant.parse("2026-05-10T09:00:00Z")
        );
        when(requestFactory.createImportItemReviewApply(701L, 81L)).thenReturn(request);
        Mockito.doThrow(new PolicyViolationException("denied")).when(capabilityAdmissionPolicy).check(request);

        ImportItemReviewService service = new ImportItemReviewServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            capabilityAdmissionPolicy,
            requestFactory,
            utcClock
        );

        assertThatThrownBy(() -> service.applyReview(701L, 81L, new ImportItemReviewService.ImportReviewApplyCommand(9001L)))
            .isInstanceOf(PolicyViolationException.class);

        verify(requestFactory).createImportItemReviewApply(701L, 81L);
        verify(capabilityAdmissionPolicy).check(request);
        verify(itemRepository, never()).findImportJobItemById(any());
        verify(itemRepository, never()).saveImportJobItem(any());
        verify(jobRepository, never()).saveImportJob(any());
        verify(userCommandService, never()).updateUser(any());
    }

    @Test
    void rejectReviewChecksAdmissionBeforeAnyMutation() {
        ImportJobRepository jobRepository = Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = Mockito.mock(UserCommandService.class);
        CapabilityAdmissionPolicy capabilityAdmissionPolicy = Mockito.mock(CapabilityAdmissionPolicy.class);
        CapabilityAdmissionRequestFactory requestFactory = Mockito.mock(CapabilityAdmissionRequestFactory.class);
        UtcClock utcClock = Mockito.mock(UtcClock.class);
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            701L,
            "IMPORT_ITEM_REVIEW_REJECT",
            com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            81L,
            com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
            Instant.parse("2026-05-10T09:05:00Z")
        );
        when(requestFactory.createImportItemReviewReject(701L, 81L)).thenReturn(request);
        Mockito.doThrow(new PolicyViolationException("denied")).when(capabilityAdmissionPolicy).check(request);

        ImportItemReviewService service = new ImportItemReviewServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            capabilityAdmissionPolicy,
            requestFactory,
            utcClock
        );

        assertThatThrownBy(() -> service.rejectReview(701L, 81L, new ImportItemReviewService.ImportReviewRejectCommand("no")))
            .isInstanceOf(PolicyViolationException.class);

        verify(requestFactory).createImportItemReviewReject(701L, 81L);
        verify(capabilityAdmissionPolicy).check(request);
        verify(itemRepository, never()).findImportJobItemById(any());
        verify(itemRepository, never()).saveImportJobItem(any());
        verify(jobRepository, never()).saveImportJob(any());
        verify(userCommandService, never()).updateUser(any());
    }
}
