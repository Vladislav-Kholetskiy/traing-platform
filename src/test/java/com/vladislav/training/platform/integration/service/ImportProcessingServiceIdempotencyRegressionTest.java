package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.service.UserCommandService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImportProcessingServiceIdempotency} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportProcessingServiceIdempotencyRegressionTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T16:00:00Z");

    @Test
    void terminalItemProcessingDoesNotCreateRepeatedOwnerEffect() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportJobItem appliedItem = new ImportJobItem(
            5101L,
            9010L,
            0,
            "APP_USER",
            "EXT-900",
            "EMP-900",
            ImportItemStatus.APPLIED,
            "700",
            "{\"employeeNumber\":\"EMP-900\",\"externalId\":\"EXT-900\",\"lastName\":\"Done\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}",
            null,
            null,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(30)
        );

        when(itemRepository.findImportJobItemById(5101L)).thenReturn(appliedItem);

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJobItem processed = service.processImportJobItem(5101L);

        assertThat(processed).isEqualTo(appliedItem);
        verify(itemRepository, never()).saveImportJobItem(any(ImportJobItem.class));
        verifyNoInteractions(appUserRepository, userCommandService, jobRepository, utcClock);
    }

    @Test
    void completedJobProcessingDoesNotReprocessTerminalItems() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportJob completedJob = new ImportJob(
            5102L,
            "HR_CSV",
            "terminal.csv",
            701L,
            ImportJobStatus.COMPLETED,
            "{\"rows\":1}",
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(30),
            1,
            1,
            1,
            0,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(30)
        );
        ImportJobItem appliedItem = new ImportJobItem(
            5103L,
            5102L,
            0,
            "APP_USER",
            "EXT-901",
            "EMP-901",
            ImportItemStatus.APPLIED,
            "701",
            "{\"employeeNumber\":\"EMP-901\",\"externalId\":\"EXT-901\",\"lastName\":\"Done\",\"firstName\":\"Bob\",\"status\":\"ACTIVE\"}",
            null,
            null,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(30)
        );

        when(jobRepository.findImportJobById(5102L)).thenReturn(completedJob);
        when(itemRepository.findImportJobItemsByJobId(5102L)).thenReturn(List.of(appliedItem));

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJob processed = service.processImportJob(5102L);

        assertThat(processed).isEqualTo(completedJob);
        verify(jobRepository, never()).saveImportJob(any(ImportJob.class));
        verify(itemRepository, never()).saveImportJobItem(any(ImportJobItem.class));
        verifyNoInteractions(appUserRepository, userCommandService, utcClock);
    }
}
