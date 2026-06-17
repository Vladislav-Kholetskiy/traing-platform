package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.repository.TemporaryRoleAssignmentRepository;
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
 * Проверяет поведение {@code TemporaryRoleAssignmentCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TemporaryRoleAssignmentCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T10:00:00Z");

    @Mock
    private TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository;
    @Mock
    private AccessCommandValidationSupport accessCommandValidationSupport;
    @Mock
    private UtcClock utcClock;

    private TemporaryRoleAssignmentCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TemporaryRoleAssignmentCommandServiceImpl(
            temporaryRoleAssignmentRepository,
            accessCommandValidationSupport,
            utcClock
        );
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void saveTemporaryRoleAssignmentValidatesThenPersistsCanonicalRow() {
        when(temporaryRoleAssignmentRepository.saveTemporaryRoleAssignment(any(TemporaryRoleAssignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TemporaryRoleAssignment saved = service.saveTemporaryRoleAssignment(assignment(
            null,
            1L,
            900L,
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));

        ArgumentCaptor<TemporaryRoleAssignment> captor = ArgumentCaptor.forClass(TemporaryRoleAssignment.class);
        InOrder inOrder = inOrder(accessCommandValidationSupport, temporaryRoleAssignmentRepository);
        inOrder.verify(accessCommandValidationSupport).ensureTemporaryRoleAssignable(any(TemporaryRoleAssignment.class));
        inOrder.verify(temporaryRoleAssignmentRepository).saveTemporaryRoleAssignment(captor.capture());

        TemporaryRoleAssignment persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.userId()).isEqualTo(1L);
        assertThat(persisted.roleId()).isEqualTo(900L);
        assertThat(persisted.validTo()).isNull();
        assertThat(saved.createdAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void saveTemporaryRoleAssignmentRejectsPreExistingIdBeforeOwnerValidation() {
        assertThatThrownBy(() -> service.saveTemporaryRoleAssignment(assignment(
            99L,
            1L,
            900L,
            FIXED_INSTANT,
            null
        )))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("temporaryRoleAssignment.id must be null");

        verifyNoInteractions(accessCommandValidationSupport, temporaryRoleAssignmentRepository);
    }

    @Test
    void closeActiveTemporaryRoleAssignmentsByUserIdClosesAllRowsWithSingleEffectiveAt() {
        TemporaryRoleAssignment first = assignment(10L, 1L, 900L, FIXED_INSTANT.minusSeconds(7200), null);
        TemporaryRoleAssignment second = assignment(11L, 1L, 901L, FIXED_INSTANT.minusSeconds(3600), null);
        when(temporaryRoleAssignmentRepository.findActiveTemporaryRoleAssignmentsByUserId(1L, FIXED_INSTANT))
            .thenReturn(List.of(first, second));
        when(temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentById(10L))
            .thenReturn(assignment(10L, 1L, 900L, first.validFrom(), FIXED_INSTANT));
        when(temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentById(11L))
            .thenReturn(assignment(11L, 1L, 901L, second.validFrom(), FIXED_INSTANT));

        List<TemporaryRoleAssignment> closedAssignments = service.closeActiveTemporaryRoleAssignmentsByUserId(1L, FIXED_INSTANT);

        assertThat(closedAssignments).hasSize(2);
        assertThat(closedAssignments).allSatisfy(assignment -> assertThat(assignment.validTo()).isEqualTo(FIXED_INSTANT));
        verify(accessCommandValidationSupport).ensureTemporaryRoleClosable(10L, FIXED_INSTANT);
        verify(accessCommandValidationSupport).ensureTemporaryRoleClosable(11L, FIXED_INSTANT);
        verify(temporaryRoleAssignmentRepository).endTemporaryRoleAssignment(10L, FIXED_INSTANT);
        verify(temporaryRoleAssignmentRepository).endTemporaryRoleAssignment(11L, FIXED_INSTANT);
    }

    private TemporaryRoleAssignment assignment(Long id, Long userId, Long roleId, Instant validFrom, Instant validTo) {
        return new TemporaryRoleAssignment(id, userId, roleId, validFrom, validTo, FIXED_INSTANT, FIXED_INSTANT);
    }
}
