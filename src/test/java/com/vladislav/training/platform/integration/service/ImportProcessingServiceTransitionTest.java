package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.mockito.InOrder;
/**
 * Проверяет поведение {@code ImportProcessingServiceTransition}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportProcessingServiceTransitionTest {

    private static final String IMPORT_PROCESSING_SERVICE_IMPL =
        "com.vladislav.training.platform.integration.service.ImportProcessingServiceImpl";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T14:00:00Z");

    @Test
    void importProcessingServiceImplMustExistAndImplementContract() throws Exception {
        assertThatCode(() -> Class.forName(IMPORT_PROCESSING_SERVICE_IMPL)).doesNotThrowAnyException();
        assertThatCode(() -> ImportProcessingService.class.getDeclaredMethod("processImportJob", Long.class))
            .doesNotThrowAnyException();
        assertThatCode(() -> ImportProcessingService.class.getDeclaredMethod("processImportJobItem", Long.class))
            .doesNotThrowAnyException();
    }

    @Test
    void pendingJobMovesToInProgressBeforeItemProcessingAndCompletesWhenAllItemsApply() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJob pendingJob = job(900L, ImportJobStatus.PENDING, null, null);
        ImportJobItem pendingItem = item(
            3001L,
            900L,
            0,
            ImportItemStatus.PENDING,
            "EXT-100",
            "EMP-100",
            null,
            "{\"employeeNumber\":\"EMP-100\",\"externalId\":\"EXT-100\",\"lastName\":\"Updated\",\"firstName\":\"Alice\",\"middleName\":\"M\",\"status\":\"ACTIVE\"}",
            null,
            null,
            null
        );
        AppUser currentUser = user(55L, "EMP-100", "EXT-100", "Before", "Alice", null, UserStatus.ACTIVE);
        AppUser updatedUser = user(55L, "EMP-100", "EXT-100", "Updated", "Alice", "M", UserStatus.ACTIVE);

        when(jobRepository.findImportJobById(900L)).thenReturn(pendingJob);
        when(itemRepository.findImportJobItemsByJobId(900L)).thenReturn(List.of(pendingItem));
        when(itemRepository.findImportJobItemById(3001L)).thenReturn(pendingItem);
        when(appUserRepository.findAllUsers()).thenReturn(List.of(currentUser));
        when(userCommandService.updateUser(any(AppUser.class))).thenReturn(updatedUser);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(jobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJob.class));
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));

        ImportJob processed = service.processImportJob(900L);

        assertThat(processed.status()).isEqualTo(ImportJobStatus.COMPLETED);
        assertThat(processed.startedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(processed.completedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(processed.totalItemCount()).isEqualTo(1);
        assertThat(processed.processedItemCount()).isEqualTo(1);
        assertThat(processed.appliedItemCount()).isEqualTo(1);
        assertThat(processed.failedItemCount()).isZero();
        assertThat(processed.requiresReviewItemCount()).isZero();

        ArgumentCaptor<ImportJob> savedJobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(jobRepository, org.mockito.Mockito.times(2)).saveImportJob(savedJobCaptor.capture());
        assertThat(savedJobCaptor.getAllValues())
            .extracting(ImportJob::status)
            .containsExactly(ImportJobStatus.IN_PROGRESS, ImportJobStatus.COMPLETED);

        ArgumentCaptor<ImportJobItem> savedItemCaptor = ArgumentCaptor.forClass(ImportJobItem.class);
        verify(itemRepository, org.mockito.Mockito.times(2)).saveImportJobItem(savedItemCaptor.capture());
        assertThat(savedItemCaptor.getAllValues())
            .extracting(ImportJobItem::status)
            .containsExactly(ImportItemStatus.PROCESSING, ImportItemStatus.APPLIED);

        InOrder inOrder = inOrder(jobRepository, itemRepository, userCommandService);
        inOrder.verify(jobRepository).saveImportJob(any(ImportJob.class));
        inOrder.verify(itemRepository).saveImportJobItem(any(ImportJobItem.class));
        inOrder.verify(userCommandService).updateUser(any(AppUser.class));
        inOrder.verify(itemRepository).saveImportJobItem(any(ImportJobItem.class));
        inOrder.verify(jobRepository).saveImportJob(any(ImportJob.class));
    }

    @Test
    void mixedItemOutcomesCloseJobCompletedWithErrorsAndRecalculateCounters() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJob pendingJob = job(901L, ImportJobStatus.PENDING, null, null);
        ImportJobItem appliedItem = item(
            3002L,
            901L,
            0,
            ImportItemStatus.PENDING,
            "EXT-200",
            "EMP-200",
            null,
            "{\"employeeNumber\":\"EMP-200\",\"externalId\":\"EXT-200\",\"lastName\":\"Applied\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}",
            null,
            null,
            null
        );
        ImportJobItem reviewItem = item(
            3003L,
            901L,
            1,
            ImportItemStatus.PENDING,
            null,
            "EMP-201",
            null,
            "{\"employeeNumber\":\"EMP-201\",\"lastName\":\"Ambiguous\",\"firstName\":\"Bob\",\"status\":\"ACTIVE\"}",
            null,
            null,
            null
        );
        AppUser currentUser = user(56L, "EMP-200", "EXT-200", "Before", "Alice", null, UserStatus.ACTIVE);
        AppUser updatedUser = user(56L, "EMP-200", "EXT-200", "Applied", "Alice", null, UserStatus.ACTIVE);
        AppUser ambiguousA = user(57L, "EMP-201", "EXT-A", "A", "Bob", null, UserStatus.ACTIVE);
        AppUser ambiguousB = user(58L, "EMP-201", "EXT-B", "B", "Bob", null, UserStatus.ACTIVE);

        when(jobRepository.findImportJobById(901L)).thenReturn(pendingJob);
        when(itemRepository.findImportJobItemsByJobId(901L)).thenReturn(List.of(appliedItem, reviewItem));
        when(appUserRepository.findAllUsers()).thenReturn(List.of(currentUser, ambiguousA, ambiguousB));
        when(userCommandService.updateUser(any(AppUser.class))).thenReturn(updatedUser);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(jobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJob.class));
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));

        ImportJob processed = service.processImportJob(901L);

        assertThat(processed.status()).isEqualTo(ImportJobStatus.COMPLETED_WITH_ERRORS);
        assertThat(processed.totalItemCount()).isEqualTo(2);
        assertThat(processed.processedItemCount()).isEqualTo(2);
        assertThat(processed.appliedItemCount()).isEqualTo(1);
        assertThat(processed.failedItemCount()).isZero();
        assertThat(processed.requiresReviewItemCount()).isEqualTo(1);
    }

    @Test
    void fatalOwnerFailureClosesJobFailed() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJob pendingJob = job(902L, ImportJobStatus.PENDING, null, null);
        ImportJobItem pendingItem = item(
            3004L,
            902L,
            0,
            ImportItemStatus.PENDING,
            "EXT-300",
            "EMP-300",
            null,
            "{\"employeeNumber\":\"EMP-300\",\"externalId\":\"EXT-300\",\"lastName\":\"Fatal\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}",
            null,
            null,
            null
        );
        AppUser currentUser = user(59L, "EMP-300", "EXT-300", "Before", "Alice", null, UserStatus.ACTIVE);

        when(jobRepository.findImportJobById(902L)).thenReturn(pendingJob);
        when(itemRepository.findImportJobItemsByJobId(902L)).thenReturn(List.of(pendingItem));
        when(appUserRepository.findAllUsers()).thenReturn(List.of(currentUser));
        when(userCommandService.updateUser(any(AppUser.class))).thenThrow(new IllegalStateException("owner apply failed"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(jobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJob.class));
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));

        ImportJob processed = service.processImportJob(902L);

        assertThat(processed.status()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(processed.totalItemCount()).isEqualTo(1);
        assertThat(processed.processedItemCount()).isEqualTo(1);
        assertThat(processed.appliedItemCount()).isZero();
        assertThat(processed.failedItemCount()).isEqualTo(1);
        assertThat(processed.requiresReviewItemCount()).isZero();

        ArgumentCaptor<ImportJobItem> savedItemCaptor = ArgumentCaptor.forClass(ImportJobItem.class);
        verify(itemRepository, org.mockito.Mockito.times(2)).saveImportJobItem(savedItemCaptor.capture());
        assertThat(savedItemCaptor.getAllValues().get(1).status()).isEqualTo(ImportItemStatus.FAILED);
        assertThat(savedItemCaptor.getAllValues().get(1).errorCode()).isEqualTo("OWNER_COMMAND_FAILED");
    }

    private ImportJob job(Long id, ImportJobStatus status, Instant startedAt, Instant completedAt) {
        return new ImportJob(
            id,
            "HR_CSV",
            "administrative-stage53.csv",
            700L,
            status,
            "{\"rows\":1}",
            startedAt,
            completedAt,
            1,
            0,
            0,
            0,
            0,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
    }

    private ImportJobItem item(
        Long id,
        Long jobId,
        int itemNo,
        ImportItemStatus status,
        String externalId,
        String employeeNumber,
        String matchedEntityId,
        String payload,
        String errorCode,
        String errorMessage,
        Instant processedAt
    ) {
        return new ImportJobItem(
            id,
            jobId,
            itemNo,
            "APP_USER",
            externalId,
            employeeNumber,
            status,
            matchedEntityId,
            payload,
            errorCode,
            errorMessage,
            processedAt,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
    }

    private AppUser user(
        Long id,
        String employeeNumber,
        String externalId,
        String lastName,
        String firstName,
        String middleName,
        UserStatus status
    ) {
        return new AppUser(
            id,
            employeeNumber,
            externalId,
            lastName,
            firstName,
            middleName,
            status,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(60)
        );
    }
}
