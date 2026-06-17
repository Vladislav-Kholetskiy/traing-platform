package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
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
 * Проверяет поведение сервиса {@code ImportProcessingServiceTypedOwner}.
 * Сценарии сосредоточены на прикладной логике.
 */
class ImportProcessingServiceTypedOwnerServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T18:00:00Z");

    @Test
    void appUserEffectUsesTypedUserCommandSeamAndMarksAppliedOnlyAfterSuccessfulOwnerCommand() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportJobItem pendingItem = new ImportJobItem(
            5301L,
            9301L,
            0,
            "APP_USER",
            "EXT-1100",
            "EMP-1100",
            ImportItemStatus.PENDING,
            null,
            "{\"employeeNumber\":\"EMP-1100\",\"externalId\":\"EXT-1100\",\"lastName\":\"Updated\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}",
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AppUser currentUser = new AppUser(
            811L,
            "EMP-1100",
            "EXT-1100",
            "Before",
            "Alice",
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(120)
        );

        when(itemRepository.findImportJobItemById(5301L)).thenReturn(pendingItem);
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));
        when(appUserRepository.findAllUsers()).thenReturn(List.of(currentUser));
        when(userCommandService.updateUser(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0, AppUser.class));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJobItem processed = service.processImportJobItem(5301L);

        assertThat(processed.status()).isEqualTo(ImportItemStatus.APPLIED);
        assertThat(processed.errorCode()).isNull();
        assertThat(processed.matchedEntityId()).isEqualTo("811");

        ArgumentCaptor<ImportJobItem> savedItemCaptor = ArgumentCaptor.forClass(ImportJobItem.class);
        verify(itemRepository, org.mockito.Mockito.times(2)).saveImportJobItem(savedItemCaptor.capture());
        assertThat(savedItemCaptor.getAllValues())
            .extracting(ImportJobItem::status)
            .containsExactly(ImportItemStatus.PROCESSING, ImportItemStatus.APPLIED);

        InOrder inOrder = inOrder(itemRepository, appUserRepository, userCommandService);
        inOrder.verify(itemRepository).saveImportJobItem(any(ImportJobItem.class));
        inOrder.verify(appUserRepository).findAllUsers();
        inOrder.verify(userCommandService).updateUser(any(AppUser.class));
        inOrder.verify(itemRepository).saveImportJobItem(any(ImportJobItem.class));
    }

    @Test
    void unsupportedTargetTypeDoesNotInvokeOwnerCommandAndCannotBecomeApplied() {
        ImportJobRepository jobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository itemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        AppUserRepository appUserRepository = org.mockito.Mockito.mock(AppUserRepository.class);
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        ImportJobItem pendingItem = new ImportJobItem(
            5302L,
            9302L,
            0,
            "COURSE",
            null,
            null,
            ImportItemStatus.PENDING,
            null,
            "{\"courseId\":\"C-1\"}",
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );

        when(itemRepository.findImportJobItemById(5302L)).thenReturn(pendingItem);
        when(itemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        ImportProcessingService service = new ImportProcessingServiceImpl(
            jobRepository,
            itemRepository,
            appUserRepository,
            userCommandService,
            utcClock
        );

        ImportJobItem processed = service.processImportJobItem(5302L);

        assertThat(processed.status()).isEqualTo(ImportItemStatus.REQUIRES_REVIEW);
        assertThat(processed.errorCode()).isEqualTo("UNSUPPORTED_TARGET_TYPE");

        verify(itemRepository, org.mockito.Mockito.times(2)).saveImportJobItem(any(ImportJobItem.class));
        verify(userCommandService, never()).createUser(any(AppUser.class));
        verify(userCommandService, never()).updateUser(any(AppUser.class));
        verifyNoInteractions(appUserRepository);
    }
}
