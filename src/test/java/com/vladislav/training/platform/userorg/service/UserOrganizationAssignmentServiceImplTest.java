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
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
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
 * Проверяет поведение {@code UserOrganizationAssignmentServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class UserOrganizationAssignmentServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    @Mock
    private UserOrganizationAssignmentValidationSupport userOrganizationAssignmentValidationSupport;
    @Mock
    private UtcClock utcClock;

    private UserOrganizationAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserOrganizationAssignmentServiceImpl(
            userOrganizationAssignmentRepository,
            userOrganizationAssignmentValidationSupport,
            utcClock
        );
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void assignOrganizationAssignmentValidatesThenPersistsCanonicalHistoryRow() {
        when(userOrganizationAssignmentRepository.saveOrganizationAssignment(any(UserOrganizationAssignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserOrganizationAssignment saved = service.assignOrganizationAssignment(assignment(
            null,
            7L,
            30L,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));

        ArgumentCaptor<UserOrganizationAssignment> captor = ArgumentCaptor.forClass(UserOrganizationAssignment.class);
        InOrder inOrder = inOrder(userOrganizationAssignmentValidationSupport, userOrganizationAssignmentRepository);
        inOrder.verify(userOrganizationAssignmentValidationSupport).ensureAssignable(any(UserOrganizationAssignment.class));
        inOrder.verify(userOrganizationAssignmentRepository).saveOrganizationAssignment(captor.capture());

        UserOrganizationAssignment persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.userId()).isEqualTo(7L);
        assertThat(persisted.organizationalUnitId()).isEqualTo(30L);
        assertThat(persisted.assignmentType()).isEqualTo(OrganizationAssignmentType.PRIMARY);
        assertThat(persisted.validTo()).isNull();
        assertThat(saved.createdAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void assignOrganizationAssignmentRejectsPreExistingIdBeforeOwnerValidation() {
        assertThatThrownBy(() -> service.assignOrganizationAssignment(assignment(
            50L,
            7L,
            30L,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT,
            null
        )))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("userOrganizationAssignment.id must be null");

        verifyNoInteractions(userOrganizationAssignmentValidationSupport, userOrganizationAssignmentRepository);
    }

    @Test
    void closeActiveOrganizationAssignmentsByUserIdClosesAllRowsWithSingleEffectiveAt() {
        UserOrganizationAssignment first = assignment(50L, 7L, 30L, OrganizationAssignmentType.PRIMARY, FIXED_INSTANT.minusSeconds(7200), null);
        UserOrganizationAssignment second = assignment(51L, 7L, 31L, OrganizationAssignmentType.SECONDARY, FIXED_INSTANT.minusSeconds(3600), null);
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(first, second));
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentById(50L))
            .thenReturn(assignment(50L, 7L, 30L, OrganizationAssignmentType.PRIMARY, first.validFrom(), FIXED_INSTANT));
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentById(51L))
            .thenReturn(assignment(51L, 7L, 31L, OrganizationAssignmentType.SECONDARY, second.validFrom(), FIXED_INSTANT));

        List<UserOrganizationAssignment> closedAssignments = service.closeActiveOrganizationAssignmentsByUserId(7L, FIXED_INSTANT);

        assertThat(closedAssignments).hasSize(2);
        assertThat(closedAssignments).allSatisfy(assignment -> assertThat(assignment.validTo()).isEqualTo(FIXED_INSTANT));
        verify(userOrganizationAssignmentValidationSupport).ensureClosable(50L, FIXED_INSTANT);
        verify(userOrganizationAssignmentValidationSupport).ensureClosable(51L, FIXED_INSTANT);
        verify(userOrganizationAssignmentRepository).endOrganizationAssignment(50L, FIXED_INSTANT);
        verify(userOrganizationAssignmentRepository).endOrganizationAssignment(51L, FIXED_INSTANT);
    }

    @Test
    void replacePrimaryHomeUnitClosesCurrentPrimaryAndSavesReplacement() {
        UserOrganizationAssignment currentPrimary = assignment(
            50L,
            7L,
            30L,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(7200),
            null
        );
        when(userOrganizationAssignmentValidationSupport.ensurePrimaryHomeReplacementAllowed(7L, 31L, FIXED_INSTANT))
            .thenReturn(currentPrimary);
        when(userOrganizationAssignmentRepository.saveOrganizationAssignment(any(UserOrganizationAssignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserOrganizationAssignment replacement = service.replacePrimaryHomeUnit(7L, 31L, FIXED_INSTANT);

        ArgumentCaptor<UserOrganizationAssignment> captor = ArgumentCaptor.forClass(UserOrganizationAssignment.class);
        InOrder inOrder = inOrder(userOrganizationAssignmentValidationSupport, userOrganizationAssignmentRepository);
        inOrder.verify(userOrganizationAssignmentValidationSupport).ensurePrimaryHomeReplacementAllowed(7L, 31L, FIXED_INSTANT);
        inOrder.verify(userOrganizationAssignmentRepository).endOrganizationAssignment(50L, FIXED_INSTANT);
        inOrder.verify(userOrganizationAssignmentRepository).saveOrganizationAssignment(captor.capture());

        UserOrganizationAssignment persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.userId()).isEqualTo(7L);
        assertThat(persisted.organizationalUnitId()).isEqualTo(31L);
        assertThat(persisted.assignmentType()).isEqualTo(OrganizationAssignmentType.PRIMARY);
        assertThat(persisted.validFrom()).isEqualTo(FIXED_INSTANT);
        assertThat(replacement.organizationalUnitId()).isEqualTo(31L);
    }

    private UserOrganizationAssignment assignment(
        Long id,
        Long userId,
        Long organizationalUnitId,
        OrganizationAssignmentType assignmentType,
        Instant validFrom,
        Instant validTo
    ) {
        return new UserOrganizationAssignment(
            id,
            userId,
            organizationalUnitId,
            assignmentType,
            validFrom,
            validTo,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
