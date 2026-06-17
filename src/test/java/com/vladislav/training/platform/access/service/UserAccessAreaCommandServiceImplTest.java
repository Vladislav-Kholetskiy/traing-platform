package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.repository.UserAccessAreaRepository;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code UserAccessAreaCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class UserAccessAreaCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T10:00:00Z");

    @Mock
    private UserAccessAreaRepository userAccessAreaRepository;
    @Mock
    private AccessCommandValidationSupport accessCommandValidationSupport;
    @Mock
    private UtcClock utcClock;

    private UserAccessAreaCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserAccessAreaCommandServiceImpl(userAccessAreaRepository, accessCommandValidationSupport, utcClock);
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void findActiveUserAccessAreasByUserIdReturnsRepositorySnapshot() {
        when(userAccessAreaRepository.findActiveUserAccessAreasByUserId(1L, FIXED_INSTANT)).thenReturn(List.of(area(
            10L,
            1L,
            null,
            AccessScopeType.GLOBAL,
            FIXED_INSTANT.minusSeconds(3600),
            null
        )));

        assertThat(service.findActiveUserAccessAreasByUserId(1L, FIXED_INSTANT)).hasSize(1);
    }

    @Test
    void saveUserAccessAreaValidatesThenPersistsCanonicalRow() {
        when(userAccessAreaRepository.saveUserAccessArea(any(UserAccessArea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccessArea saved = service.saveUserAccessArea(area(
            null,
            1L,
            30L,
            AccessScopeType.UNIT_ONLY,
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));

        ArgumentCaptor<UserAccessArea> captor = ArgumentCaptor.forClass(UserAccessArea.class);
        InOrder inOrder = inOrder(accessCommandValidationSupport, userAccessAreaRepository);
        inOrder.verify(accessCommandValidationSupport).ensureUserAccessAreaAssignable(any(UserAccessArea.class));
        inOrder.verify(userAccessAreaRepository).saveUserAccessArea(captor.capture());

        UserAccessArea persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.userId()).isEqualTo(1L);
        assertThat(persisted.organizationalUnitId()).isEqualTo(30L);
        assertThat(persisted.validFrom()).isEqualTo(FIXED_INSTANT.minusSeconds(7200));
        assertThat(persisted.validTo()).isNull();
        assertThat(persisted.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(persisted.updatedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(saved.createdAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void saveUserAccessAreaRejectsPreExistingIdBeforeOwnerValidation() {
        assertThatThrownBy(() -> service.saveUserAccessArea(area(
            99L,
            1L,
            30L,
            AccessScopeType.UNIT_ONLY,
            FIXED_INSTANT,
            null
        )))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("userAccessArea.id must be null");

        verifyNoInteractions(accessCommandValidationSupport, userAccessAreaRepository);
    }

    @Test
    void closeActiveUserAccessAreasByUserIdClosesAllRowsWithSingleEffectiveAt() {
        UserAccessArea first = area(10L, 1L, null, AccessScopeType.GLOBAL, FIXED_INSTANT.minusSeconds(7200), null);
        UserAccessArea second = area(11L, 1L, 30L, AccessScopeType.UNIT_ONLY, FIXED_INSTANT.minusSeconds(3600), null);
        when(userAccessAreaRepository.findActiveUserAccessAreasByUserId(1L, FIXED_INSTANT)).thenReturn(List.of(first, second));
        when(userAccessAreaRepository.findUserAccessAreaById(10L)).thenReturn(area(10L, 1L, null, AccessScopeType.GLOBAL, first.validFrom(), FIXED_INSTANT));
        when(userAccessAreaRepository.findUserAccessAreaById(11L)).thenReturn(area(11L, 1L, 30L, AccessScopeType.UNIT_ONLY, second.validFrom(), FIXED_INSTANT));

        List<UserAccessArea> closedAreas = service.closeActiveUserAccessAreasByUserId(1L, FIXED_INSTANT);

        assertThat(closedAreas).hasSize(2);
        assertThat(closedAreas).allSatisfy(area -> assertThat(area.validTo()).isEqualTo(FIXED_INSTANT));
        verify(accessCommandValidationSupport).ensureUserAccessAreaClosable(10L, FIXED_INSTANT);
        verify(accessCommandValidationSupport).ensureUserAccessAreaClosable(11L, FIXED_INSTANT);
        verify(userAccessAreaRepository).revokeUserAccessArea(10L, FIXED_INSTANT);
        verify(userAccessAreaRepository).revokeUserAccessArea(11L, FIXED_INSTANT);
    }

    private UserAccessArea area(
        Long id,
        Long userId,
        Long organizationalUnitId,
        AccessScopeType accessScopeType,
        Instant validFrom,
        Instant validTo
    ) {
        return new UserAccessArea(id, userId, organizationalUnitId, accessScopeType, validFrom, validTo, FIXED_INSTANT, FIXED_INSTANT);
    }
}
