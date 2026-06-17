package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
/**
 * Проверяет поведение {@code ImportProcessingServiceFailClosedAnchor}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportProcessingServiceFailClosedAnchorTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T15:00:00Z");

    @Test
    void ambiguousAnchorMatchingFailsClosedToRequiresReview() {
        ImportProcessingService service = new ImportProcessingServiceImpl(
            org.mockito.Mockito.mock(ImportJobRepository.class),
            mockItemRepository(item(
                4101L,
                null,
                "EMP-500",
                "{\"employeeNumber\":\"EMP-500\",\"lastName\":\"Target\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}"
            )),
            mockUsers(
                user(101L, "EMP-500", "EXT-A"),
                user(102L, "EMP-500", "EXT-B")
            ),
            org.mockito.Mockito.mock(UserCommandService.class),
            fixedClock()
        );

        ImportJobItem processed = service.processImportJobItem(4101L);

        assertThat(processed.status()).isEqualTo(ImportItemStatus.REQUIRES_REVIEW);
        assertThat(processed.errorCode()).isEqualTo("AMBIGUOUS_ANCHOR");
        assertThat(processed.matchedEntityId()).isNull();
    }

    @Test
    void externalIdIsPreferredWherePresent() {
        ImportJobItemRepository itemRepository = mockItemRepository(item(
            4102L,
            "EXT-600",
            "EMP-OTHER",
            "{\"employeeNumber\":\"EMP-600\",\"externalId\":\"EXT-600\",\"lastName\":\"Updated\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}"
        ));
        AppUserRepository appUserRepository = mockUsers(
            user(103L, "EMP-600", "EXT-600"),
            user(104L, "EMP-OTHER", "EXT-OTHER")
        );
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        when(userCommandService.updateUser(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0, AppUser.class));

        ImportProcessingService service = new ImportProcessingServiceImpl(
            org.mockito.Mockito.mock(ImportJobRepository.class),
            itemRepository,
            appUserRepository,
            userCommandService,
            fixedClock()
        );

        ImportJobItem processed = service.processImportJobItem(4102L);

        assertThat(processed.status()).isEqualTo(ImportItemStatus.APPLIED);
        assertThat(processed.matchedEntityId()).isEqualTo("103");
        verify(userCommandService).updateUser(any(AppUser.class));
    }

    @Test
    void employeeNumberFallbackDoesNotApplyWhenExternalIdWasProvidedButUnmatched() {
        UserCommandService userCommandService = org.mockito.Mockito.mock(UserCommandService.class);
        ImportProcessingService service = new ImportProcessingServiceImpl(
            org.mockito.Mockito.mock(ImportJobRepository.class),
            mockItemRepository(item(
                4103L,
                "EXT-MISSING",
                "EMP-700",
                "{\"employeeNumber\":\"EMP-700\",\"externalId\":\"EXT-MISSING\",\"lastName\":\"Target\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}"
            )),
            mockUsers(user(105L, "EMP-700", "EXT-700")),
            userCommandService,
            fixedClock()
        );

        ImportJobItem processed = service.processImportJobItem(4103L);

        assertThat(processed.status()).isEqualTo(ImportItemStatus.FAILED);
        assertThat(processed.errorCode()).isEqualTo("ANCHOR_NOT_FOUND");
        verify(userCommandService, never()).createUser(any(AppUser.class));
        verify(userCommandService, never()).updateUser(any(AppUser.class));
    }

    private ImportJobItemRepository mockItemRepository(ImportJobItem item) {
        ImportJobItemRepository repository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        when(repository.findImportJobItemById(item.id())).thenReturn(item);
        when(repository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));
        return repository;
    }

    private AppUserRepository mockUsers(AppUser... users) {
        AppUserRepository repository = org.mockito.Mockito.mock(AppUserRepository.class);
        when(repository.findAllUsers()).thenReturn(List.of(users));
        return repository;
    }

    private UtcClock fixedClock() {
        UtcClock clock = org.mockito.Mockito.mock(UtcClock.class);
        when(clock.now()).thenReturn(FIXED_INSTANT);
        return clock;
    }

    private ImportJobItem item(Long id, String externalId, String employeeNumber, String payload) {
        return new ImportJobItem(
            id,
            999L,
            0,
            "APP_USER",
            externalId,
            employeeNumber,
            ImportItemStatus.PENDING,
            null,
            payload,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
    }

    private AppUser user(Long id, String employeeNumber, String externalId) {
        return new AppUser(
            id,
            employeeNumber,
            externalId,
            "Current",
            "Alice",
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(60)
        );
    }
}
