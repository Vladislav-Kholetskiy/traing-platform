package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code SelfCompletionOrgSnapshotFactsResolver}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class SelfCompletionOrgSnapshotFactsResolverTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-22T12:00:00Z");

    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private UserOrganizationAssignmentService userOrganizationAssignmentService;
    @Mock
    private OrganizationQueryService organizationQueryService;

    private SelfCompletionOrgSnapshotFactsResolver reader;

    @BeforeEach
    void setUp() {
        reader = new SelfCompletionOrgSnapshotFactsResolver(
            testAttemptRepository,
            userOrganizationAssignmentService,
            organizationQueryService
        );
    }

    @Test
    void readsSelfCompletionOrgSnapshotFactsFromCanonicalPrimaryAssignmentAndOrganizationPath() {
        TestAttempt completedAttempt = completedSelfAttempt(9001L, 301L);
        UserOrganizationAssignment primaryAssignment = new UserOrganizationAssignment(
            501L,
            301L,
            701L,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(60)
        );
        OrganizationalUnit organizationalUnit = new OrganizationalUnit(
            701L,
            601L,
            801L,
            "Ops",
            OrganizationalUnitStatus.ACTIVE,
            "/company/ops",
            2,
            "OU-701",
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(60)
        );

        when(testAttemptRepository.findTestAttemptById(9001L)).thenReturn(completedAttempt);
        when(userOrganizationAssignmentService.findActiveOrganizationAssignmentsByUserId(301L, FIXED_INSTANT))
            .thenReturn(List.of(primaryAssignment));
        when(organizationQueryService.findOrganizationalUnitById(701L)).thenReturn(organizationalUnit);

        SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts facts =
            reader.readSelfCompletionOrgSnapshotFacts(9001L);

        assertThat(facts.organizationalUnitIdSnapshot()).isEqualTo(701L);
        assertThat(facts.organizationalPathSnapshot()).isEqualTo("/company/ops");
    }

    @Test
    void failsClosedWhenCanonicalPrimaryAssignmentIsMissingOrAmbiguous() {
        TestAttempt completedAttempt = completedSelfAttempt(9002L, 302L);
        when(testAttemptRepository.findTestAttemptById(9002L)).thenReturn(completedAttempt);
        when(userOrganizationAssignmentService.findActiveOrganizationAssignmentsByUserId(302L, FIXED_INSTANT))
            .thenReturn(List.of());

        assertThatThrownBy(() -> reader.readSelfCompletionOrgSnapshotFacts(9002L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("exactly one active PRIMARY org assignment");
    }

    private TestAttempt completedSelfAttempt(Long attemptId, Long userId) {
        return new TestAttempt(
            attemptId,
            userId,
            401L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT
        );
    }
}


