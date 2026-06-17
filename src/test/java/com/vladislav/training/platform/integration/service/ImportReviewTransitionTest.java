package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.repository.ImportJobItemRepository;
import com.vladislav.training.platform.integration.repository.ImportJobRepository;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.service.UserCommandService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
/**
 * Проверяет поведение {@code ImportReviewTransition}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportReviewTransitionTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T10:00:00Z");

    @Test
    void applyReviewIsAllowedOnlyFromRequiresReviewAndUsesTypedOwnerCommand() {
        ImportJobRepository jobRepository = mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        UserCommandService userCommandService = mock(UserCommandService.class);
        CapabilityAdmissionPolicy capabilityAdmissionPolicy = mock(CapabilityAdmissionPolicy.class);
        CapabilityAdmissionRequestFactory requestFactory = mock(CapabilityAdmissionRequestFactory.class);
        UtcClock utcClock = mock(UtcClock.class);

        ImportJobItem reviewItem = importItem(81L, 61L, ImportItemStatus.REQUIRES_REVIEW, "APP_USER", "AMBIGUOUS_ANCHOR");
        ImportJobItem alreadyApplied = importItem(82L, 61L, ImportItemStatus.APPLIED, "APP_USER", null);
        ImportJob job = importJob(61L, 2, 2, 0, 0, 1, ImportJobStatus.COMPLETED_WITH_ERRORS);
        AppUser matchedUser = new AppUser(
            9001L,
            "EMP-1",
            "EXT-1",
            "Ivanov",
            "Ivan",
            null,
            UserStatus.INACTIVE,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(300)
        );
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            701L,
            "IMPORT_ITEM_REVIEW_APPLY",
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            81L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        when(requestFactory.createImportItemReviewApply(701L, 81L)).thenReturn(request);
        when(itemRepository.findImportJobItemById(81L)).thenReturn(reviewItem);
        when(jobRepository.findImportJobById(61L)).thenReturn(job);
        when(itemRepository.findImportJobItemsByJobId(61L)).thenReturn(List.of(reviewItem, alreadyApplied));
        when(appUserRepository.findUserById(9001L)).thenReturn(matchedUser);
        when(userCommandService.updateUser(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0, AppUser.class));
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));
        when(jobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJob.class));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        ImportItemReviewService service = new ImportItemReviewServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            capabilityAdmissionPolicy,
            requestFactory,
            utcClock
        );

        ImportItemReviewService.ImportReviewResult result = service.applyReview(
            701L,
            81L,
            new ImportItemReviewService.ImportReviewApplyCommand(9001L)
        );

        assertThat(result.status()).isEqualTo(ImportItemStatus.APPLIED);
        assertThat(result.matchedEntityId()).isEqualTo("9001");
        assertThat(result.errorCode()).isNull();

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userCommandService).updateUser(userCaptor.capture());
        assertThat(userCaptor.getValue().id()).isEqualTo(9001L);
        assertThat(userCaptor.getValue().status()).isEqualTo(UserStatus.ACTIVE);

        ArgumentCaptor<ImportJob> jobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(jobRepository).saveImportJob(jobCaptor.capture());
        assertThat(jobCaptor.getValue().status()).isEqualTo(ImportJobStatus.COMPLETED);
        assertThat(jobCaptor.getValue().processedItemCount()).isEqualTo(2);
        assertThat(jobCaptor.getValue().appliedItemCount()).isEqualTo(2);
        assertThat(jobCaptor.getValue().failedItemCount()).isZero();
        assertThat(jobCaptor.getValue().requiresReviewItemCount()).isZero();

    }

    @Test
    void applyReviewOwnerCommandFailureDoesNotProduceAppliedAndLeavesJobFailed() {
        ImportJobRepository jobRepository = mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        UserCommandService userCommandService = mock(UserCommandService.class);
        CapabilityAdmissionPolicy capabilityAdmissionPolicy = mock(CapabilityAdmissionPolicy.class);
        CapabilityAdmissionRequestFactory requestFactory = mock(CapabilityAdmissionRequestFactory.class);
        UtcClock utcClock = mock(UtcClock.class);

        ImportJobItem reviewItem = importItem(81L, 61L, ImportItemStatus.REQUIRES_REVIEW, "APP_USER", "AMBIGUOUS_ANCHOR");
        ImportJobItem nextReviewItem = importItem(82L, 61L, ImportItemStatus.REQUIRES_REVIEW, "APP_USER", "AMBIGUOUS_ANCHOR");
        ImportJob job = importJob(61L, 2, 2, 0, 0, 2, ImportJobStatus.COMPLETED_WITH_ERRORS);
        AppUser matchedUser = new AppUser(
            9001L,
            "EMP-1",
            "EXT-1",
            "Ivanov",
            "Ivan",
            null,
            UserStatus.INACTIVE,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(300)
        );
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            701L,
            "IMPORT_ITEM_REVIEW_APPLY",
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            81L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        when(requestFactory.createImportItemReviewApply(701L, 81L)).thenReturn(request);
        when(itemRepository.findImportJobItemById(81L)).thenReturn(reviewItem);
        when(jobRepository.findImportJobById(61L)).thenReturn(job);
        when(itemRepository.findImportJobItemsByJobId(61L)).thenReturn(List.of(reviewItem, nextReviewItem));
        when(appUserRepository.findUserById(9001L)).thenReturn(matchedUser);
        when(userCommandService.updateUser(any(AppUser.class))).thenThrow(new IllegalStateException("boom"));
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));
        when(jobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJob.class));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        ImportItemReviewService service = new ImportItemReviewServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            capabilityAdmissionPolicy,
            requestFactory,
            utcClock
        );

        ImportItemReviewService.ImportReviewResult result = service.applyReview(
            701L,
            81L,
            new ImportItemReviewService.ImportReviewApplyCommand(9001L)
        );

        assertThat(result.status()).isEqualTo(ImportItemStatus.FAILED);
        assertThat(result.errorCode()).isEqualTo("OWNER_COMMAND_FAILED");

        ArgumentCaptor<ImportJob> jobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(jobRepository).saveImportJob(jobCaptor.capture());
        assertThat(jobCaptor.getValue().status()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(jobCaptor.getValue().failedItemCount()).isEqualTo(1);
        assertThat(jobCaptor.getValue().requiresReviewItemCount()).isEqualTo(1);
    }

    @Test
    void rejectReviewIsAllowedOnlyFromRequiresReviewAndDoesNotCallOwnerCommand() {
        ImportJobRepository jobRepository = mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        UserCommandService userCommandService = mock(UserCommandService.class);
        CapabilityAdmissionPolicy capabilityAdmissionPolicy = mock(CapabilityAdmissionPolicy.class);
        CapabilityAdmissionRequestFactory requestFactory = mock(CapabilityAdmissionRequestFactory.class);
        UtcClock utcClock = mock(UtcClock.class);

        ImportJobItem reviewItem = importItem(81L, 61L, ImportItemStatus.REQUIRES_REVIEW, "APP_USER", "AMBIGUOUS_ANCHOR");
        ImportJob job = importJob(61L, 1, 1, 0, 0, 1, ImportJobStatus.COMPLETED_WITH_ERRORS);
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            701L,
            "IMPORT_ITEM_REVIEW_REJECT",
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            81L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        when(requestFactory.createImportItemReviewReject(701L, 81L)).thenReturn(request);
        when(itemRepository.findImportJobItemById(81L)).thenReturn(reviewItem);
        when(jobRepository.findImportJobById(61L)).thenReturn(job);
        when(itemRepository.findImportJobItemsByJobId(61L)).thenReturn(List.of(reviewItem));
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));
        when(jobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJob.class));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        ImportItemReviewService service = new ImportItemReviewServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            capabilityAdmissionPolicy,
            requestFactory,
            utcClock
        );

        ImportItemReviewService.ImportReviewResult result = service.rejectReview(
            701L,
            81L,
            new ImportItemReviewService.ImportReviewRejectCommand("manual reject")
        );

        assertThat(result.status()).isEqualTo(ImportItemStatus.FAILED);
        assertThat(result.errorCode()).isEqualTo("REVIEW_REJECTED");
        verify(userCommandService, never()).updateUser(any());
    }

    @Test
    void reviewActionsRejectNonReviewItems() {
        ImportJobRepository jobRepository = mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        UserCommandService userCommandService = mock(UserCommandService.class);
        CapabilityAdmissionPolicy capabilityAdmissionPolicy = mock(CapabilityAdmissionPolicy.class);
        CapabilityAdmissionRequestFactory requestFactory = mock(CapabilityAdmissionRequestFactory.class);
        UtcClock utcClock = mock(UtcClock.class);
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            701L,
            "IMPORT_ITEM_REVIEW_REJECT",
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            81L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        when(requestFactory.createImportItemReviewReject(701L, 81L)).thenReturn(request);
        when(itemRepository.findImportJobItemById(81L)).thenReturn(importItem(81L, 61L, ImportItemStatus.APPLIED, "APP_USER", null));

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
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("REQUIRES_REVIEW");

        verify(itemRepository, never()).saveImportJobItem(any());
        verify(userCommandService, never()).updateUser(any());
    }

    private ImportJob importJob(
        Long jobId,
        int total,
        int processed,
        int applied,
        int failed,
        int requiresReview,
        ImportJobStatus status
    ) {
        return new ImportJob(
            jobId,
            "HR_CSV",
            "review.csv",
            701L,
            status,
            "{\"rows\":2}",
            FIXED_INSTANT.minusSeconds(600),
            status == ImportJobStatus.PENDING || status == ImportJobStatus.IN_PROGRESS ? null : FIXED_INSTANT.minusSeconds(60),
            total,
            processed,
            applied,
            failed,
            requiresReview,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT.minusSeconds(300)
        );
    }

    private ImportJobItem importItem(
        Long itemId,
        Long jobId,
        ImportItemStatus status,
        String targetEntityType,
        String errorCode
    ) {
        return new ImportJobItem(
            itemId,
            jobId,
            0,
            targetEntityType,
            "EXT-1",
            "EMP-1",
            status,
            null,
            "{\"employeeNumber\":\"EMP-1\",\"externalId\":\"EXT-1\",\"lastName\":\"Ivanov\",\"firstName\":\"Ivan\",\"status\":\"ACTIVE\"}",
            errorCode,
            errorCode == null ? null : errorCode,
            status == ImportItemStatus.REQUIRES_REVIEW || status == ImportItemStatus.APPLIED || status == ImportItemStatus.FAILED
                ? FIXED_INSTANT.minusSeconds(120)
                : null,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT.minusSeconds(120)
        );
    }
}
