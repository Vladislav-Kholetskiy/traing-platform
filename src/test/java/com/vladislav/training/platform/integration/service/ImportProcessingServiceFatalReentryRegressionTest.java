package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.repository.ImportJobItemRepository;
import com.vladislav.training.platform.integration.repository.ImportJobRepository;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.service.UserCommandService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
/**
 * Проверяет, что {@code ImportProcessingServiceFatalReentry} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportProcessingServiceFatalReentryRegressionTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T17:00:00Z");

    @Test
    void reentryStopsAtSavedFatalOwnerFailureAndKeepsLaterItemsUntouched() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportJob inProgressJob = new ImportJob(
            5201L,
            "HR_CSV",
            "fatal-reentry.csv",
            702L,
            ImportJobStatus.IN_PROGRESS,
            "{\"rows\":2}",
            FIXED_INSTANT.minusSeconds(120),
            null,
            2,
            1,
            0,
            1,
            0,
            FIXED_INSTANT.minusSeconds(240),
            FIXED_INSTANT.minusSeconds(120)
        );
        ImportJobItem fatalItem = new ImportJobItem(
            5202L,
            5201L,
            0,
            "APP_USER",
            "EXT-1000",
            "EMP-1000",
            ImportItemStatus.FAILED,
            "800",
            "{\"employeeNumber\":\"EMP-1000\",\"externalId\":\"EXT-1000\",\"lastName\":\"Fatal\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}",
            "OWNER_COMMAND_FAILED",
            "owner apply failed",
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(240),
            FIXED_INSTANT.minusSeconds(60)
        );
        ImportJobItem pendingItem = new ImportJobItem(
            5203L,
            5201L,
            1,
            "APP_USER",
            "EXT-1001",
            "EMP-1001",
            ImportItemStatus.PENDING,
            null,
            "{\"employeeNumber\":\"EMP-1001\",\"externalId\":\"EXT-1001\",\"lastName\":\"Later\",\"firstName\":\"Bob\",\"status\":\"ACTIVE\"}",
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(240),
            FIXED_INSTANT.minusSeconds(120)
        );

        when(jobRepository.findImportJobById(5201L)).thenReturn(inProgressJob);
        when(itemRepository.findImportJobItemsByJobId(5201L)).thenReturn(List.of(fatalItem, pendingItem));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(jobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJob.class));
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));
        when(appUserRepository.findAllUsers()).thenReturn(List.of());

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJob processed = service.processImportJob(5201L);

        assertThat(processed.status()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(processed.totalItemCount()).isEqualTo(2);
        assertThat(processed.processedItemCount()).isEqualTo(1);
        assertThat(processed.appliedItemCount()).isZero();
        assertThat(processed.failedItemCount()).isEqualTo(1);
        assertThat(processed.requiresReviewItemCount()).isZero();

        ArgumentCaptor<ImportJob> savedJobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(jobRepository).saveImportJob(savedJobCaptor.capture());
        assertThat(savedJobCaptor.getValue().status()).isEqualTo(ImportJobStatus.FAILED);

        verify(itemRepository, never()).saveImportJobItem(any(ImportJobItem.class));
        verify(userCommandService, never()).updateUser(any());
        verify(appUserRepository, never()).findAllUsers();

        assertThat(pendingItem.status()).isEqualTo(ImportItemStatus.PENDING);
    }
}
