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
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.service.UserCommandService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
/**
 * Проверяет поведение {@code ImportProcessingServiceNoAppliedBeforeOwnerCommandSuccess}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportProcessingServiceNoAppliedBeforeOwnerCommandSuccessTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T19:00:00Z");

    @Test
    void ownerCommandExceptionKeepsItemNonAppliedAndClosesJobFailedWithoutRunningNextUnsafeEffect() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportJob inProgressJob = new ImportJob(
            5401L,
            "HR_CSV",
            "typed-owner-seam.csv",
            703L,
            ImportJobStatus.IN_PROGRESS,
            "{\"rows\":2}",
            FIXED_INSTANT.minusSeconds(180),
            null,
            2,
            0,
            0,
            0,
            0,
            FIXED_INSTANT.minusSeconds(240),
            FIXED_INSTANT.minusSeconds(180)
        );
        ImportJobItem firstPendingItem = new ImportJobItem(
            5402L,
            5401L,
            0,
            "APP_USER",
            "EXT-1200",
            "EMP-1200",
            ImportItemStatus.PENDING,
            null,
            "{\"employeeNumber\":\"EMP-1200\",\"externalId\":\"EXT-1200\",\"lastName\":\"Boom\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}",
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(240),
            FIXED_INSTANT.minusSeconds(180)
        );
        ImportJobItem secondPendingItem = new ImportJobItem(
            5403L,
            5401L,
            1,
            "APP_USER",
            "EXT-1201",
            "EMP-1201",
            ImportItemStatus.PENDING,
            null,
            "{\"employeeNumber\":\"EMP-1201\",\"externalId\":\"EXT-1201\",\"lastName\":\"Later\",\"firstName\":\"Bob\",\"status\":\"ACTIVE\"}",
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(240),
            FIXED_INSTANT.minusSeconds(180)
        );
        AppUser currentUser = new AppUser(
            812L,
            "EMP-1200",
            "EXT-1200",
            "Before",
            "Alice",
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(180)
        );

        when(jobRepository.findImportJobById(5401L)).thenReturn(inProgressJob);
        when(itemRepository.findImportJobItemsByJobId(5401L)).thenReturn(List.of(firstPendingItem, secondPendingItem));
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));
        when(jobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportJob.class));
        when(appUserRepository.findAllUsers()).thenReturn(List.of(currentUser));
        when(userCommandService.updateUser(any(AppUser.class))).thenThrow(new IllegalStateException("owner apply failed"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJob processed = service.processImportJob(5401L);

        assertThat(processed.status()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(processed.appliedItemCount()).isZero();
        assertThat(processed.failedItemCount()).isEqualTo(1);
        assertThat(processed.processedItemCount()).isEqualTo(1);
        assertThat(processed.totalItemCount()).isEqualTo(2);

        ArgumentCaptor<ImportJobItem> savedItemCaptor = ArgumentCaptor.forClass(ImportJobItem.class);
        verify(itemRepository, org.mockito.Mockito.times(2)).saveImportJobItem(savedItemCaptor.capture());
        assertThat(savedItemCaptor.getAllValues())
            .extracting(ImportJobItem::status)
            .containsExactly(ImportItemStatus.PROCESSING, ImportItemStatus.FAILED);
        assertThat(savedItemCaptor.getAllValues().get(1).errorCode()).isEqualTo("OWNER_COMMAND_FAILED");
        assertThat(savedItemCaptor.getAllValues())
            .extracting(ImportJobItem::status)
            .doesNotContain(ImportItemStatus.APPLIED);

        verify(userCommandService).updateUser(any(AppUser.class));
        verify(userCommandService, never()).updateUser(argThatUserWithEmployeeNumber("EMP-1201"));
        assertThat(secondPendingItem.status()).isEqualTo(ImportItemStatus.PENDING);
    }

    private AppUser argThatUserWithEmployeeNumber(String employeeNumber) {
        return org.mockito.ArgumentMatchers.argThat(user -> user != null && employeeNumber.equals(user.employeeNumber()));
    }
}
