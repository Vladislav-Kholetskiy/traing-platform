package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignCourseRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.OrganizationalTargetingQueryService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentCampaignCommandServiceImplRejectPath}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentCampaignCommandServiceImplRejectPathTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-09T12:00:00Z");

    @Mock private AssignmentCampaignRepository assignmentCampaignRepository;
    @Mock private AssignmentCampaignCourseRepository assignmentCampaignCourseRepository;
    @Mock private AssignmentCampaignRecipientSnapshotRepository recipientSnapshotRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentTestRepository assignmentTestRepository;
    @Mock private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock private OrganizationQueryService organizationQueryService;
    @Mock private OrganizationalTargetingQueryService organizationalTargetingQueryService;
    @Mock private UserQueryService userQueryService;
    @Mock private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock private TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService;
    @Mock private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private TestRepository testRepository;
    @Mock private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock private AssignmentCampaignLaunchNotificationService assignmentCampaignLaunchNotificationService;

    private AssignmentCampaignCommandServiceImpl service;
    private AssignmentCommandService assignmentCommandService;
    private AssignmentStatusRecalculationService assignmentStatusRecalculationService;

    @BeforeEach
    void setUp() {
        assignmentStatusRecalculationService = new AssignmentStatusRecalculationServiceImpl(
            assignmentRepository,
            assignmentTestRepository
        );
        assignmentCommandService = new AssignmentCommandServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            assignmentStatusRecalculationService
        );
        service = new AssignmentCampaignCommandServiceImpl(
            assignmentCampaignRepository,
            assignmentCampaignCourseRepository,
            recipientSnapshotRepository,
            assignmentRepository,
            assignmentCommandService,
            assignmentStatusRecalculationService,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            organizationQueryService,
            organizationalTargetingQueryService,
            new MandatoryAssignmentRecipientEligibilityService(
                organizationQueryService,
                userQueryService,
                userOrgFoundationStateReadService,
                temporaryRoleAssignmentReadService,
                userOrganizationAssignmentRepository
            ),
            new AssignmentCampaignRecipientSnapshotCaptureContract(),
            courseRepository,
            topicRepository,
            testRepository,
            criticalCommandAuditSupport,
            assignmentCampaignLaunchNotificationService
        );
        when(capabilityAdmissionRequestFactory.createAssignmentCampaignLaunch(any())).thenReturn(admissionRequest());
    }

    @Test
    void admissionDeniedStopsBeforeMutation() {
        doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "denied"))
            .when(capabilityAdmissionPolicy).check(admissionRequest());

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("denied");

        verifyNoInteractions(
            assignmentCampaignRepository,
            assignmentCampaignCourseRepository,
            recipientSnapshotRepository,
            organizationQueryService,
            organizationalTargetingQueryService,
            courseRepository,
            topicRepository,
            testRepository,
            criticalCommandAuditSupport
        );
    }

    @Test
    void archivedTargetFailsClosed() {
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops"))
            .thenReturn(unit(41L, 7L, OrganizationalUnitStatus.ARCHIVED, "/company/ops"));

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("41");

        assertNoMutation();
    }

    @Test
    void unsupportedTargetingBasisFailsClosed() {
        assertThatThrownBy(() -> service.launchAssignmentCampaign(new LaunchAssignmentCampaignCommand(
            "Запуск кампании назначений",
            null,
            "MANUAL",
            "launch-source-42",
            null,
            List.of(501L),
            new LaunchAssignmentCampaignCommand.Targeting("POSITION", "line-ops"),
            new LaunchAssignmentCampaignCommand.DeadlinePolicy(FIXED_INSTANT.plusSeconds(86400))
        )))
            .isInstanceOf(PolicyViolationException.class);

        assertNoMutation();
        verifyNoInteractions(organizationQueryService, organizationalTargetingQueryService);
    }

    @Test
    void emptyRecipientPoolFailsClosed() {
        stubValidTarget();
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of());

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ConflictException.class);

        assertNoMutation();
    }

    @Test
    void invalidRecipientOrgContextFailsClosed() {
        stubValidTarget();
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L));
        when(userQueryService.findUserById(201L)).thenReturn(user(201L, UserStatus.ACTIVE));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(201L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(201L, true, Set.of("OPERATOR"))
        );
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(201L, FIXED_INSTANT))
            .thenReturn(List.of(primary(201L, 301L)));
        when(organizationQueryService.findOrganizationalUnitById(301L))
            .thenReturn(unit(301L, 17L, OrganizationalUnitStatus.ACTIVE, "/company/other"));
        when(organizationQueryService.findOrganizationalUnitTypeById(17L)).thenReturn(type(17L, true, true));

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ConflictException.class);

        assertNoMutation();
    }

    @Test
    void inactiveRecipientFailsClosed() {
        stubValidTarget();
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L));
        when(userQueryService.findUserById(201L)).thenReturn(user(201L, UserStatus.INACTIVE));

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ConflictException.class);

        assertNoMutation();
    }

    @Test
    void recipientWithoutEffectiveOperatorRoleFailsClosed() {
        stubValidTarget();
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L));
        when(userQueryService.findUserById(201L)).thenReturn(user(201L, UserStatus.ACTIVE));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(201L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(201L, true, Set.of("EMPLOYEE"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(201L, FIXED_INSTANT))
            .thenReturn(List.of(901L));
        when(userOrgFoundationStateReadService.findRoleCodesByIds(List.of(901L))).thenReturn(Set.of("SUPERVISOR"));

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ConflictException.class);

        assertNoMutation();
    }

    @Test
    void invalidPrimaryHomeUnitStillFailsClosedForTemporaryOperator() {
        stubValidTarget();
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L));
        when(userQueryService.findUserById(201L)).thenReturn(user(201L, UserStatus.ACTIVE));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(201L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(201L, true, Set.of("EMPLOYEE"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(201L, FIXED_INSTANT))
            .thenReturn(List.of(901L));
        when(userOrgFoundationStateReadService.findRoleCodesByIds(List.of(901L))).thenReturn(Set.of("OPERATOR"));
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(201L, FIXED_INSTANT))
            .thenReturn(List.of(primary(201L, 301L)));
        when(organizationQueryService.findOrganizationalUnitById(301L))
            .thenReturn(unit(301L, 17L, OrganizationalUnitStatus.ACTIVE, "/company/ops/line-1"));
        when(organizationQueryService.findOrganizationalUnitTypeById(17L)).thenReturn(type(17L, false, true));

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ConflictException.class);

        assertNoMutation();
    }

    @Test
    void duplicateActiveAssignmentFailsClosed() {
        stubValidTarget();
        stubValidRecipient(201L, 301L, "/company/ops/line-1");
        stubValidCourse();
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L));
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(201L, 501L)).thenReturn(new Assignment(
            9001L,
            77L,
            201L,
            501L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.minusSeconds(1000),
            FIXED_INSTANT.plusSeconds(1000),
            null,
            null,
            FIXED_INSTANT.minusSeconds(1000),
            FIXED_INSTANT.minusSeconds(1000)
        ));

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ConflictException.class);

        assertNoMutation();
    }

    @Test
    void missingFinalControlTestFailsClosed() {
        stubValidTarget();
        stubValidRecipient(201L, 301L, "/company/ops/line-1");
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L));
        when(courseRepository.findCourseById(501L)).thenReturn(new Course(
            501L,
            "Course 501",
            null,
            ContentStatus.PUBLISHED,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(topicRepository.findTopicsByCourseId(501L)).thenReturn(List.of(new Topic(
            601L,
            501L,
            "Topic 601",
            null,
            ContentStatus.PUBLISHED,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        )));
        when(testRepository.findActiveFinalTestByTopicId(601L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ValidationException.class);

        assertNoMutation();
    }

    @Test
    void invalidDeadlineFailsClosedBeforeOwnerMutation() {
        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.minusSeconds(1))))
            .isInstanceOf(ValidationException.class);

        verifyNoInteractions(
            organizationQueryService,
            organizationalTargetingQueryService,
            courseRepository,
            topicRepository,
            testRepository
        );
        assertNoMutation();
    }

    @Test
    void fullyIneligibleBatchLeavesNoPartialArtifacts() {
        stubValidTarget();
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L, 202L));
        when(userQueryService.findUserById(201L)).thenReturn(user(201L, UserStatus.ACTIVE));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(201L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(201L, true, Set.of("EMPLOYEE"))
        );
        when(userQueryService.findUserById(202L)).thenReturn(user(202L, UserStatus.ACTIVE));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(202L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(202L, true, Set.of("EMPLOYEE"))
        );

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(ConflictException.class);

        assertNoMutation();
    }

    @Test
    void missingInteractiveAuditActorFailsClosedWithoutFallbackOnLaunch() {
        stubValidTarget();
        stubValidRecipient(201L, 301L, "/company/ops/line-1");
        stubValidCourse();
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L));
        when(assignmentCampaignRepository.saveAssignmentCampaign(any(AssignmentCampaign.class))).thenAnswer(invocation -> {
            AssignmentCampaign campaign = invocation.getArgument(0, AssignmentCampaign.class);
            return new AssignmentCampaign(
                1001L,
                campaign.name(),
                campaign.description(),
                campaign.sourceType(),
                campaign.sourceRef(),
                campaign.sourceNameSnapshot(),
                campaign.createdAt(),
                campaign.updatedAt()
            );
        });
        when(assignmentCampaignCourseRepository.saveAssignmentCampaignCourse(any(AssignmentCampaignCourse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, AssignmentCampaignCourse.class));
        when(recipientSnapshotRepository.saveAssignmentCampaignRecipientSnapshot(any(AssignmentCampaignRecipientSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, AssignmentCampaignRecipientSnapshot.class));
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0, Assignment.class);
            return new Assignment(
                4001L,
                assignment.campaignId(),
                assignment.userId(),
                assignment.courseId(),
                assignment.status(),
                assignment.assignedAt(),
                assignment.deadlineAt(),
                assignment.cancelledAt(),
                assignment.closedAt(),
                assignment.createdAt(),
                assignment.updatedAt()
            );
        });
        when(assignmentRepository.findAssignmentById(4001L)).thenAnswer(invocation -> new Assignment(
            4001L,
            1001L,
            201L,
            501L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(86400),
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(assignmentTestRepository.saveAssignmentTest(any(AssignmentTest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, AssignmentTest.class));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(4001L)).thenReturn(List.of());
        doThrow(new IllegalStateException("interactive actor required"))
            .when(criticalCommandAuditSupport).resolveInteractiveActorUserId();

        assertThatThrownBy(() -> service.launchAssignmentCampaign(command(FIXED_INSTANT.plusSeconds(86400))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("interactive actor required");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    private void assertNoMutation() {
        verify(assignmentCampaignRepository, never()).saveAssignmentCampaign(any(AssignmentCampaign.class));
        verify(assignmentCampaignCourseRepository, never()).saveAssignmentCampaignCourse(any(AssignmentCampaignCourse.class));
        verify(recipientSnapshotRepository, never()).saveAssignmentCampaignRecipientSnapshot(any(AssignmentCampaignRecipientSnapshot.class));
        verify(assignmentRepository, never()).saveAssignment(any(Assignment.class));
        verify(assignmentTestRepository, never()).saveAssignmentTest(any(AssignmentTest.class));
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    private void stubValidTarget() {
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops"))
            .thenReturn(unit(41L, 7L, OrganizationalUnitStatus.ACTIVE, "/company/ops"));
        when(organizationQueryService.findOrganizationalUnitTypeById(7L)).thenReturn(type(7L, true, true));
    }

    private void stubValidRecipient(Long userId, Long homeUnitId, String homeUnitPath) {
        when(userQueryService.findUserById(userId)).thenReturn(user(userId, UserStatus.ACTIVE));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(userId, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(userId, true, Set.of("OPERATOR"))
        );
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(userId, FIXED_INSTANT))
            .thenReturn(List.of(primary(userId, homeUnitId)));
        when(organizationQueryService.findOrganizationalUnitById(homeUnitId))
            .thenReturn(unit(homeUnitId, 17L, OrganizationalUnitStatus.ACTIVE, homeUnitPath));
        when(organizationQueryService.findOrganizationalUnitTypeById(17L)).thenReturn(type(17L, true, true));
    }

    private void stubValidCourse() {
        when(courseRepository.findCourseById(501L)).thenReturn(new Course(
            501L,
            "Course 501",
            null,
            ContentStatus.PUBLISHED,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(topicRepository.findTopicsByCourseId(501L)).thenReturn(List.of(new Topic(
            601L,
            501L,
            "Topic 601",
            null,
            ContentStatus.PUBLISHED,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        )));
        when(testRepository.findActiveFinalTestByTopicId(601L)).thenReturn(Optional.of(new com.vladislav.training.platform.content.domain.Test(
            701L,
            601L,
            "Final control 701",
            null,
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            BigDecimal.valueOf(80),
            "DEFAULT",
            true,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        )));
    }

    private LaunchAssignmentCampaignCommand command(Instant deadlineAt) {
        return new LaunchAssignmentCampaignCommand(
            "Запуск кампании назначений",
            null,
            "MANUAL",
            "launch-source-42",
            null,
            List.of(501L),
            new LaunchAssignmentCampaignCommand.Targeting("ORG_UNIT", "/company/ops"),
            new LaunchAssignmentCampaignCommand.DeadlinePolicy(deadlineAt)
        );
    }

    private CapabilityAdmissionRequest admissionRequest() {
        return new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CAMPAIGN_LAUNCH,
            CapabilityTargetEntityType.ASSIGNMENT_CAMPAIGN,
            null,
            new CapabilityAdmissionPayload.AssignmentCampaignLaunch("MANUAL", "launch-source-42"),
            FIXED_INSTANT
        );
    }

    private OrganizationalUnit unit(Long id, Long typeId, OrganizationalUnitStatus status, String path) {
        return new OrganizationalUnit(id, 11L, typeId, "Unit " + id, status, path, 2, "ou-" + id, FIXED_INSTANT, FIXED_INSTANT);
    }

    private OrganizationalUnitType type(Long id, boolean canBeOperatorHomeUnit, boolean canBeCampaignTarget) {
        return new OrganizationalUnitType(
            id,
            "LINE",
            "Line",
            null,
            OrganizationalNodeKind.LINEAR,
            canBeOperatorHomeUnit,
            canBeCampaignTarget,
            true,
            false,
            false,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AppUser user(Long userId, UserStatus status) {
        return new AppUser(userId, "E-" + userId, "user-" + userId, "User", "Name", null, status, FIXED_INSTANT, FIXED_INSTANT);
    }

    private UserOrganizationAssignment primary(Long userId, Long organizationalUnitId) {
        return new UserOrganizationAssignment(
            null,
            userId,
            organizationalUnitId,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(3600)
        );
    }
}






