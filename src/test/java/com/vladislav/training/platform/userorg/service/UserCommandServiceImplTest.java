package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code UserCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UtcClock utcClock;

    private UserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserCommandServiceImpl(appUserRepository, utcClock);
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void createUserNormalizesInputChecksUniquenessAndPersistsCanonicalRow() {
        when(appUserRepository.existsUserByEmployeeNumber("EMP-1")).thenReturn(false);
        when(appUserRepository.existsUserByExternalId("EXT-1")).thenReturn(false);
        when(appUserRepository.saveUser(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser created = service.createUser(new AppUser(
            null,
            " EMP-1 ",
            " EXT-1 ",
            " Last ",
            " First ",
            " Middle ",
            UserStatus.ACTIVE,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(10)
        ));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        InOrder inOrder = inOrder(appUserRepository);
        inOrder.verify(appUserRepository).existsUserByEmployeeNumber("EMP-1");
        inOrder.verify(appUserRepository).existsUserByExternalId("EXT-1");
        inOrder.verify(appUserRepository).saveUser(captor.capture());
        verifyNoMoreInteractions(appUserRepository);

        AppUser persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.employeeNumber()).isEqualTo("EMP-1");
        assertThat(persisted.externalId()).isEqualTo("EXT-1");
        assertThat(persisted.lastName()).isEqualTo("Last");
        assertThat(persisted.firstName()).isEqualTo("First");
        assertThat(persisted.middleName()).isEqualTo("Middle");
        assertThat(persisted.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(created.updatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void createUserRejectsDuplicateEmployeeNumberBeforePersistence() {
        when(appUserRepository.existsUserByEmployeeNumber("EMP-1")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(user(null, "EMP-1", null, "Last", "First", null, UserStatus.ACTIVE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("employeeNumber");

        verify(appUserRepository).existsUserByEmployeeNumber("EMP-1");
        verifyNoMoreInteractions(appUserRepository);
    }

    @Test
    void createUserRejectsBlankExternalIdWhenProvided() {
        assertThatThrownBy(() -> service.createUser(user(null, "EMP-1", "   ", "Last", "First", null, UserStatus.ACTIVE)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("externalId must not be blank");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    void updateUserRejectsEmployeeNumberMutationBeforePersistence() {
        AppUser currentUser = user(5L, "EMP-5", null, "Last", "First", null, UserStatus.ACTIVE);
        when(appUserRepository.findUserById(5L)).thenReturn(currentUser);

        assertThatThrownBy(() -> service.updateUser(user(5L, "EMP-5-NEW", null, "Updated", "User", null, UserStatus.ACTIVE)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("employeeNumber is create-only");

        verify(appUserRepository).findUserById(5L);
        verifyNoMoreInteractions(appUserRepository);
    }

    @Test
    void updateUserPreservesIdentifiersAndPersistsDescriptiveMutation() {
        AppUser currentUser = user(5L, "EMP-5", "EXT-5", "Last", "First", null, UserStatus.ACTIVE);
        when(appUserRepository.findUserById(5L)).thenReturn(currentUser);
        when(appUserRepository.saveUser(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser updated = service.updateUser(user(5L, " EMP-5 ", " EXT-5 ", " Updated ", " User ", " Middle ", UserStatus.ACTIVE));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).findUserById(5L);
        verify(appUserRepository).saveUser(captor.capture());
        AppUser persisted = captor.getValue();
        assertThat(persisted.employeeNumber()).isEqualTo("EMP-5");
        assertThat(persisted.externalId()).isEqualTo("EXT-5");
        assertThat(persisted.lastName()).isEqualTo("Updated");
        assertThat(persisted.firstName()).isEqualTo("User");
        assertThat(persisted.middleName()).isEqualTo("Middle");
        assertThat(updated.updatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void deactivateUserAfterAdmissionMarksUserInactiveUsingEffectiveTimestamp() {
        AppUser currentUser = user(5L, "EMP-5", "EXT-5", "Last", "First", null, UserStatus.ACTIVE);
        when(appUserRepository.findUserById(5L)).thenReturn(currentUser);
        when(appUserRepository.saveUser(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser deactivated = service.deactivateUserAfterAdmission(5L, FIXED_INSTANT.plusSeconds(60));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).findUserById(5L);
        verify(appUserRepository).saveUser(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(UserStatus.INACTIVE);
        assertThat(captor.getValue().updatedAt()).isEqualTo(FIXED_INSTANT.plusSeconds(60));
        assertThat(deactivated.status()).isEqualTo(UserStatus.INACTIVE);
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
        return new AppUser(id, employeeNumber, externalId, lastName, firstName, middleName, status, FIXED_INSTANT, FIXED_INSTANT);
    }
}
