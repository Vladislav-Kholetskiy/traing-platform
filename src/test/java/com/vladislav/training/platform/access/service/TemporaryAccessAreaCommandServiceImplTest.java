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
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.repository.TemporaryAccessAreaRepository;
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
 * Проверяет поведение {@code TemporaryAccessAreaCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TemporaryAccessAreaCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T10:00:00Z");

    @Mock
    private TemporaryAccessAreaRepository temporaryAccessAreaRepository;
    @Mock
    private AccessCommandValidationSupport accessCommandValidationSupport;
    @Mock
    private UtcClock utcClock;

    private TemporaryAccessAreaCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TemporaryAccessAreaCommandServiceImpl(temporaryAccessAreaRepository, accessCommandValidationSupport, utcClock);
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void saveTemporaryAccessAreaValidatesThenPersistsCanonicalRow() {
        when(temporaryAccessAreaRepository.saveTemporaryAccessArea(any(TemporaryAccessArea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TemporaryAccessArea saved = service.saveTemporaryAccessArea(area(
            null,
            1L,
            30L,
            AccessScopeType.UNIT_SUBTREE,
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));

        ArgumentCaptor<TemporaryAccessArea> captor = ArgumentCaptor.forClass(TemporaryAccessArea.class);
        InOrder inOrder = inOrder(accessCommandValidationSupport, temporaryAccessAreaRepository);
        inOrder.verify(accessCommandValidationSupport).ensureTemporaryAccessAreaAssignable(any(TemporaryAccessArea.class));
        inOrder.verify(temporaryAccessAreaRepository).saveTemporaryAccessArea(captor.capture());

        TemporaryAccessArea persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.userId()).isEqualTo(1L);
        assertThat(persisted.organizationalUnitId()).isEqualTo(30L);
        assertThat(persisted.accessScopeType()).isEqualTo(AccessScopeType.UNIT_SUBTREE);
        assertThat(persisted.validTo()).isNull();
        assertThat(saved.createdAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void saveTemporaryAccessAreaRejectsPreExistingIdBeforeOwnerValidation() {
        assertThatThrownBy(() -> service.saveTemporaryAccessArea(area(
            99L,
            1L,
            30L,
            AccessScopeType.UNIT_ONLY,
            FIXED_INSTANT,
            null
        )))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("temporaryAccessArea.id must be null");

        verifyNoInteractions(accessCommandValidationSupport, temporaryAccessAreaRepository);
    }

    @Test
    void closeActiveTemporaryAccessAreasByUserIdClosesAllRowsWithSingleEffectiveAt() {
        TemporaryAccessArea first = area(10L, 1L, null, AccessScopeType.GLOBAL, FIXED_INSTANT.minusSeconds(7200), null);
        TemporaryAccessArea second = area(11L, 1L, 30L, AccessScopeType.UNIT_ONLY, FIXED_INSTANT.minusSeconds(3600), null);
        when(temporaryAccessAreaRepository.findActiveTemporaryAccessAreasByUserId(1L, FIXED_INSTANT)).thenReturn(List.of(first, second));
        when(temporaryAccessAreaRepository.findTemporaryAccessAreaById(10L)).thenReturn(area(10L, 1L, null, AccessScopeType.GLOBAL, first.validFrom(), FIXED_INSTANT));
        when(temporaryAccessAreaRepository.findTemporaryAccessAreaById(11L)).thenReturn(area(11L, 1L, 30L, AccessScopeType.UNIT_ONLY, second.validFrom(), FIXED_INSTANT));

        List<TemporaryAccessArea> closedAreas = service.closeActiveTemporaryAccessAreasByUserId(1L, FIXED_INSTANT);

        assertThat(closedAreas).hasSize(2);
        assertThat(closedAreas).allSatisfy(area -> assertThat(area.validTo()).isEqualTo(FIXED_INSTANT));
        verify(accessCommandValidationSupport).ensureTemporaryAccessAreaClosable(10L, FIXED_INSTANT);
        verify(accessCommandValidationSupport).ensureTemporaryAccessAreaClosable(11L, FIXED_INSTANT);
        verify(temporaryAccessAreaRepository).endTemporaryAccessArea(10L, FIXED_INSTANT);
        verify(temporaryAccessAreaRepository).endTemporaryAccessArea(11L, FIXED_INSTANT);
    }

    private TemporaryAccessArea area(
        Long id,
        Long userId,
        Long organizationalUnitId,
        AccessScopeType accessScopeType,
        Instant validFrom,
        Instant validTo
    ) {
        return new TemporaryAccessArea(id, userId, organizationalUnitId, accessScopeType, validFrom, validTo, FIXED_INSTANT, FIXED_INSTANT);
    }
}
