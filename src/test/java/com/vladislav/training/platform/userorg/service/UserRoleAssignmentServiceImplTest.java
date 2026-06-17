package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
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
 * Проверяет поведение {@code UserRoleAssignmentServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class UserRoleAssignmentServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock
    private UserRoleAssignmentValidationSupport userRoleAssignmentValidationSupport;
    @Mock
    private UtcClock utcClock;

    private UserRoleAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserRoleAssignmentServiceImpl(userRoleAssignmentRepository, userRoleAssignmentValidationSupport, utcClock);
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void assignRoleAssignmentValidatesThenPersistsCanonicalHistoryRow() {
        when(userRoleAssignmentRepository.saveRoleAssignment(any(UserRoleAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRoleAssignment saved = service.assignRoleAssignment(assignment(null, 7L, 900L, FIXED_INSTANT.minusSeconds(3600), null));

        ArgumentCaptor<UserRoleAssignment> captor = ArgumentCaptor.forClass(UserRoleAssignment.class);
        InOrder inOrder = inOrder(userRoleAssignmentValidationSupport, userRoleAssignmentRepository);
        inOrder.verify(userRoleAssignmentValidationSupport).ensureAssignable(any(UserRoleAssignment.class));
        inOrder.verify(userRoleAssignmentRepository).saveRoleAssignment(captor.capture());

        UserRoleAssignment persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.userId()).isEqualTo(7L);
        assertThat(persisted.roleId()).isEqualTo(900L);
        assertThat(persisted.validTo()).isNull();
        assertThat(persisted.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(saved.updatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void assignRoleAssignmentRejectsPreExistingIdBeforeOwnerValidation() {
        assertThatThrownBy(() -> service.assignRoleAssignment(assignment(50L, 7L, 900L, FIXED_INSTANT, null)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("userRoleAssignment.id must be null");

        verifyNoInteractions(userRoleAssignmentValidationSupport, userRoleAssignmentRepository);
    }

    @Test
    void closeActiveRoleAssignmentsByUserIdClosesAllRowsWithSingleEffectiveAt() {
        UserRoleAssignment first = assignment(50L, 7L, 900L, FIXED_INSTANT.minusSeconds(7200), null);
        UserRoleAssignment second = assignment(51L, 7L, 901L, FIXED_INSTANT.minusSeconds(3600), null);
        when(userRoleAssignmentRepository.findActiveRoleAssignmentsByUserId(7L, FIXED_INSTANT)).thenReturn(List.of(first, second));
        when(userRoleAssignmentRepository.findRoleAssignmentById(50L)).thenReturn(assignment(50L, 7L, 900L, first.validFrom(), FIXED_INSTANT));
        when(userRoleAssignmentRepository.findRoleAssignmentById(51L)).thenReturn(assignment(51L, 7L, 901L, second.validFrom(), FIXED_INSTANT));

        List<UserRoleAssignment> closedAssignments = service.closeActiveRoleAssignmentsByUserId(7L, FIXED_INSTANT);

        assertThat(closedAssignments).hasSize(2);
        assertThat(closedAssignments).allSatisfy(assignment -> assertThat(assignment.validTo()).isEqualTo(FIXED_INSTANT));
        verify(userRoleAssignmentValidationSupport).ensureClosable(50L, FIXED_INSTANT);
        verify(userRoleAssignmentValidationSupport).ensureClosable(51L, FIXED_INSTANT);
        verify(userRoleAssignmentRepository).endRoleAssignment(50L, FIXED_INSTANT);
        verify(userRoleAssignmentRepository).endRoleAssignment(51L, FIXED_INSTANT);
    }

    private UserRoleAssignment assignment(Long id, Long userId, Long roleId, Instant validFrom, Instant validTo) {
        return new UserRoleAssignment(id, userId, roleId, validFrom, validTo, FIXED_INSTANT, FIXED_INSTANT);
    }
}
