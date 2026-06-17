package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeAdmissionFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignLaunchFoundationStateReadService;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.testing.admission.SelfExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет поведение {@code DefaultCapabilityAdmissionPolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class DefaultCapabilityAdmissionPolicyTest {

    private final InteractiveActorResolver interactiveActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T12:00:00Z");

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private AssignmentAdministrativeAdmissionFoundationStateReadService assignmentAdministrativeAdmissionFoundationStateReadService;
    @Mock
    private AssignmentCampaignLaunchFoundationStateReadService assignmentCampaignLaunchFoundationStateReadService;
    @Mock
    private AssignmentAssignedExecutionAdmissionFoundationStateReadService
        assignmentAssignedExecutionAdmissionFoundationStateReadService;
    @Mock
    private SelfExecutionAdmissionFoundationStateReadService selfExecutionAdmissionFoundationStateReadService;
    @Mock
    private SystemActorResolver systemActorResolver;

    private DefaultCapabilityAdmissionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DefaultCapabilityAdmissionPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        policy.setAssignmentAdministrativeAdmissionFoundationStateReadService(assignmentAdministrativeAdmissionFoundationStateReadService);
        policy.setAssignmentCampaignLaunchFoundationStateReadService(assignmentCampaignLaunchFoundationStateReadService);
        policy.setAssignmentAssignedExecutionAdmissionFoundationStateReadService(
            assignmentAssignedExecutionAdmissionFoundationStateReadService
        );
        policy.setSelfExecutionAdmissionFoundationStateReadService(selfExecutionAdmissionFoundationStateReadService);
        policy.setSystemActorResolver(systemActorResolver);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_ADMIN"));
        lenient().when(userOrgFoundationStateReadService.findActorCommandFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(101L, true, Set.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessAreaAssignRejectsArchivedTargetAsEarlyAdmissionBan() {
        when(userOrgFoundationStateReadService.findTargetUserCommandFoundationState(7L)).thenReturn(
            new UserOrgFoundationStateReadService.TargetUserCommandFoundationState(7L, true)
        );
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(30L)).thenReturn(
            new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
                30L, false, "/archived", false, true, false, false, false, false
            )
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ACCESS_USER_ACCESS_AREA_ASSIGN,
            CapabilityTargetEntityType.USER_ACCESS_AREA,
            null,
            new CapabilityAdmissionPayload.AccessArea(7L, 30L, null, AccessScopeType.UNIT_ONLY, FIXED_INSTANT, null, false),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Archived organizational unit cannot be used as target");
    }

    @Test
    void permanentRoleAssignRejectsWhenRequiredFoundationFactIsMissing() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.USERORG_USER_ROLE_ASSIGN,
            CapabilityTargetEntityType.USER_ROLE_ASSIGNMENT,
            null,
            new CapabilityAdmissionPayload.RoleAssignment(null, 900L, null, FIXED_INSTANT, null, false),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("required foundation fact: userId");
    }

    @Test
    void permanentRoleAssignAdmissionDoesNotDecideOverlapOrResultingOwnerState() {
        when(userOrgFoundationStateReadService.findTargetUserCommandFoundationState(7L)).thenReturn(
            new UserOrgFoundationStateReadService.TargetUserCommandFoundationState(7L, true)
        );
        when(userOrgFoundationStateReadService.findRoleCodesByIds(Set.of(900L))).thenReturn(Set.of("OPERATOR"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.USERORG_USER_ROLE_ASSIGN,
            CapabilityTargetEntityType.USER_ROLE_ASSIGNMENT,
            null,
            new CapabilityAdmissionPayload.RoleAssignment(7L, 900L, null, FIXED_INSTANT, null, false),
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void organizationalUnitMoveAdmissionAllowsActiveCurrentAndParentUnits() {
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(20L)).thenReturn(
            new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
                20L, true, "/unit-20", true, false, false, false, false, false
            )
        );
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(30L)).thenReturn(
            new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
                30L, true, "/unit-30", true, false, false, false, false, false
            )
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.USERORG_ORGANIZATIONAL_UNIT_MOVE,
            CapabilityTargetEntityType.ORGANIZATIONAL_UNIT,
            20L,
            new CapabilityAdmissionPayload.OrganizationalUnitMutation(
                20L,
                1L,
                30L,
                null,
                null,
                null
            ),
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void organizationalUnitMoveAdmissionRejectsArchivedParentAsEarlyFoundationBan() {
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(20L)).thenReturn(
            new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
                20L, true, "/unit-20", true, false, false, false, false, false
            )
        );
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(30L)).thenReturn(
            new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
                30L, false, "/archived", true, false, false, false, false, false
            )
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.USERORG_ORGANIZATIONAL_UNIT_MOVE,
            CapabilityTargetEntityType.ORGANIZATIONAL_UNIT,
            20L,
            new CapabilityAdmissionPayload.OrganizationalUnitMutation(
                20L,
                1L,
                30L,
                null,
                null,
                null
            ),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Archived organizational unit cannot be used as target");
    }

    @Test
    void organizationalUnitArchiveAdmissionNoLongerUsesDownstreamNotReadyBan() {
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(20L)).thenReturn(
            new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
                20L, true, "/unit-20", true, false, false, false, false, false
            )
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.USERORG_ORGANIZATIONAL_UNIT_ARCHIVE,
            CapabilityTargetEntityType.ORGANIZATIONAL_UNIT,
            20L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void technicalSystemImportLaunchAdmissionAllowsCanonicalSystemActorWithoutInteractivePrincipal() {
        SecurityContextHolder.clearContext();
        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(9900L);
        when(userOrgFoundationStateReadService.findActorCommandFoundationState(9900L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(9900L, true, Set.of())
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCodes.IMPORT_JOB_LAUNCH,
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void technicalSystemImportLaunchRejectsWrongTechnicalActorIdWithoutInteractivePrincipal() {
        SecurityContextHolder.clearContext();
        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(9900L);

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            8800L,
            CapabilityOperationCodes.IMPORT_JOB_LAUNCH,
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Authenticated principal is required");
    }

    @Test
    void technicalSystemAdmissionBypassDoesNotApplyToNotificationRuleCreate() {
        SecurityContextHolder.clearContext();

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCodes.NOTIFICATION_RULE_CREATE,
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Authenticated principal is required");
    }

    @Test
    void technicalSystemAdmissionBypassDoesNotApplyToPersonnelExcelDryRun() {
        SecurityContextHolder.clearContext();

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCodes.PERSONNEL_EXCEL_DRY_RUN,
            CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Authenticated principal is required");
    }

    @Test
    void technicalSystemAdmissionBypassDoesNotApplyToPersonnelExcelApply() {
        SecurityContextHolder.clearContext();

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCodes.PERSONNEL_EXCEL_APPLY,
            CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Authenticated principal is required");
    }

    @Test
    void interactiveAdminAdmissionAllowsPersonnelExcelDryRunAndApply() {
        when(userOrgFoundationStateReadService.findActorCommandFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(101L, true, Set.of("ADMIN"))
        );

        CapabilityAdmissionRequest dryRunRequest = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.PERSONNEL_EXCEL_DRY_RUN,
            CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        CapabilityAdmissionRequest applyRequest = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.PERSONNEL_EXCEL_APPLY,
            CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(dryRunRequest)).doesNotThrowAnyException();
        assertThatCode(() -> policy.check(applyRequest)).doesNotThrowAnyException();
    }

    @Test
    void interactiveActorMismatchRemainsDeniedEvenWhenSystemResolverExists() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            202L,
            CapabilityOperationCodes.IMPORT_ITEM_REVIEW_APPLY,
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            777L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("does not match the current authenticated principal");
    }

    @Test
    void assignmentCampaignLaunchAdmissionAllowsExpertThroughCanonicalLaunchBranch() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));
        when(assignmentCampaignLaunchFoundationStateReadService.findAssignmentCampaignLaunchFoundationState(
            new AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchAdmissionAnchor("ORG_UNIT", "unit-42")
        )).thenReturn(new AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchFoundationState(false));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CAMPAIGN_LAUNCH,
            CapabilityTargetEntityType.ASSIGNMENT_CAMPAIGN,
            null,
            new CapabilityAdmissionPayload.AssignmentCampaignLaunch("ORG_UNIT", "unit-42"),
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void assignmentCampaignLaunchAdmissionRejectsAlreadyMaterializedCampaignId() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CAMPAIGN_LAUNCH,
            CapabilityTargetEntityType.ASSIGNMENT_CAMPAIGN,
            55L,
            new CapabilityAdmissionPayload.AssignmentCampaignLaunch("ORG_UNIT", "unit-42"),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("must not reference an already materialized assignment_campaign id");
    }

    @Test
    void assignmentCancelAdmissionAllowsExpertThroughCanonicalAdministrativeBranch() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));
        when(assignmentAdministrativeAdmissionFoundationStateReadService.findAssignmentAdministrativeAdmissionFoundationState(77L))
            .thenReturn(new AssignmentAdministrativeAdmissionFoundationStateReadService.AssignmentAdministrativeAdmissionFoundationState(
                77L, 900L, false, false
            ));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CANCEL,
            CapabilityTargetEntityType.ASSIGNMENT,
            77L,
            CapabilityAdmissionPayload.AssignmentCancel.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void assignmentDeadlineExtendAdmissionAllowsAdminThroughCanonicalAdministrativeBranch() {
        when(assignmentAdministrativeAdmissionFoundationStateReadService.findAssignmentAdministrativeAdmissionFoundationState(78L))
            .thenReturn(new AssignmentAdministrativeAdmissionFoundationStateReadService.AssignmentAdministrativeAdmissionFoundationState(
                78L, 901L, false, false
            ));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_DEADLINE_EXTEND,
            CapabilityTargetEntityType.ASSIGNMENT,
            78L,
            new CapabilityAdmissionPayload.AssignmentDeadlineExtend(FIXED_INSTANT.plusSeconds(3600)),
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void assignmentCancelAdmissionRejectsWrongTargetTypeAsFailClosedMismatch() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CANCEL,
            CapabilityTargetEntityType.ASSIGNMENT_CAMPAIGN,
            77L,
            CapabilityAdmissionPayload.AssignmentCancel.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("requires ASSIGNMENT target type");
    }

    @Test
    void assignmentCancelAdmissionRejectsMissingAssignmentIdAsFailClosedMismatch() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CANCEL,
            CapabilityTargetEntityType.ASSIGNMENT,
            null,
            CapabilityAdmissionPayload.AssignmentCancel.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("required foundation fact: assignmentId");
    }

    @Test
    void assignmentReplaceWithNewAdmissionRejectsMissingCurrentCampaignAnchor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));
        when(assignmentAdministrativeAdmissionFoundationStateReadService.findAssignmentAdministrativeAdmissionFoundationState(79L))
            .thenReturn(new AssignmentAdministrativeAdmissionFoundationStateReadService.AssignmentAdministrativeAdmissionFoundationState(
                79L, null, false, false
            ));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityTargetEntityType.ASSIGNMENT,
            79L,
            new CapabilityAdmissionPayload.AssignmentReplaceWithNew(902L),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("missing campaignId anchor");
    }

    @Test
    void assignmentReplaceWithNewAdmissionRejectsMismatchedPayloadCampaignAnchor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));
        when(assignmentAdministrativeAdmissionFoundationStateReadService.findAssignmentAdministrativeAdmissionFoundationState(79L))
            .thenReturn(new AssignmentAdministrativeAdmissionFoundationStateReadService.AssignmentAdministrativeAdmissionFoundationState(
                79L, 901L, false, false
            ));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityTargetEntityType.ASSIGNMENT,
            79L,
            new CapabilityAdmissionPayload.AssignmentReplaceWithNew(902L),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("must match current assignment campaignId");
    }

    @Test
    void contentDraftCreateAcceptsTopicWithCourseParentPayloadForExpert() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TOPIC,
            null,
            new CapabilityAdmissionPayload.ContentMutation(700L, CapabilityTargetEntityType.COURSE),
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void contentDraftCreateRejectsWrongParentTypeForTopic() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TOPIC,
            null,
            new CapabilityAdmissionPayload.ContentMutation(700L, CapabilityTargetEntityType.TOPIC),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("requires parent type COURSE");
    }

    @Test
    void contentFinalAssignRejectsMissingTestIdInMutationPayload() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_EXPERT"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.CONTENT_FINAL_ASSIGN,
            CapabilityTargetEntityType.TOPIC_FINAL_CONTROL,
            800L,
            new CapabilityAdmissionPayload.TopicFinalControlMutation(800L, null),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("required foundation fact: testId");
    }

    @Test
    void assignedAttemptStartAdmissionAllowsOwnedOpenAssignmentAnchorForRegularAuthenticatedActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(assignmentAssignedExecutionAdmissionFoundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(
            101L,
            77L,
            701L
        )).thenReturn(
            new AssignmentAssignedExecutionAdmissionFoundationStateReadService
                .AssignmentAssignedExecutionAdmissionFoundationState(77L, 701L, 501L, FIXED_INSTANT, false, false, false)
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
        verifyNoInteractions(selfExecutionAdmissionFoundationStateReadService);
    }

    @Test
    void assignedAttemptContinueAdmissionAllowsOwnedOpenAssignmentAnchorForRegularAuthenticatedActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(assignmentAssignedExecutionAdmissionFoundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(
            101L,
            77L,
            701L
        )).thenReturn(
            new AssignmentAssignedExecutionAdmissionFoundationStateReadService
                .AssignmentAssignedExecutionAdmissionFoundationState(77L, 701L, 501L, FIXED_INSTANT, false, false, false)
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_CONTINUE.code(),
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
        verifyNoInteractions(selfExecutionAdmissionFoundationStateReadService);
    }

    @Test
    void assignedAttemptStartAdmissionRejectsWrongTargetTypeAsFailClosedMismatch() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT,
            77L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("requires ASSIGNMENT_TEST target type");
    }

    @Test
    void assignedAttemptStartAdmissionRejectsMissingAssignmentAnchorAsFailClosedMismatch() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("requires payload type AssignedExecution");
    }

    @Test
    void assignedAttemptStartAdmissionRejectsForeignOrMissingAssignmentAnchorFailClosed() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(assignmentAssignedExecutionAdmissionFoundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(
            101L,
            77L,
            701L
        )).thenReturn(null);

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("Assigned execution foundation facts are unavailable");
    }

    @Test
    void assignedAttemptStartAdmissionRejectsClosedAssignmentTestAsEarlyAdmissionBan() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(assignmentAssignedExecutionAdmissionFoundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(
            101L,
            77L,
            701L
        )).thenReturn(
            new AssignmentAssignedExecutionAdmissionFoundationStateReadService
                .AssignmentAssignedExecutionAdmissionFoundationState(77L, 701L, 501L, FIXED_INSTANT, false, false, true)
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("CLOSED assignment test");
    }

    @Test
    void assignedAttemptSubmitAdmissionAllowsOwnedOpenAssignmentAnchorForRegularAuthenticatedActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(assignmentAssignedExecutionAdmissionFoundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(
            101L,
            77L,
            701L
        )).thenReturn(
            new AssignmentAssignedExecutionAdmissionFoundationStateReadService
                .AssignmentAssignedExecutionAdmissionFoundationState(77L, 701L, 501L, FIXED_INSTANT, false, false, false)
        );

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_SUBMIT,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void selfAttemptStartAdmissionAllowsVisibleSelfTestForRegularAuthenticatedActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(selfExecutionAdmissionFoundationStateReadService.findSelfExecutionAdmissionFoundationState(101L, 501L))
            .thenReturn(new SelfExecutionAdmissionFoundationStateReadService.SelfExecutionAdmissionFoundationState(501L));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.TEST,
            501L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
        verifyNoInteractions(assignmentAssignedExecutionAdmissionFoundationStateReadService);
    }

    @Test
    void selfAttemptContinueAdmissionAllowsVisibleSelfTestForRegularAuthenticatedActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(selfExecutionAdmissionFoundationStateReadService.findSelfExecutionAdmissionFoundationState(101L, 501L))
            .thenReturn(new SelfExecutionAdmissionFoundationStateReadService.SelfExecutionAdmissionFoundationState(501L));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_CONTINUE,
            CapabilityTargetEntityType.TEST,
            501L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
        verifyNoInteractions(assignmentAssignedExecutionAdmissionFoundationStateReadService);
    }

    @Test
    void selfAttemptStartAdmissionRejectsWrongTargetTypeAsFailClosedMismatch() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("requires TEST target type");
    }

    @Test
    void selfAttemptStartAdmissionRejectsAssignmentShapedPayloadAsFailClosedMismatch() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.TEST,
            501L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("requires payload type SelfExecution");
    }

    @Test
    void selfAttemptStartAdmissionRejectsHiddenOrMissingSelfVisibleTestFailClosed() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(selfExecutionAdmissionFoundationStateReadService.findSelfExecutionAdmissionFoundationState(101L, 501L))
            .thenReturn(null);

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.TEST,
            501L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Self execution foundation facts are unavailable");
    }

    @Test
    void selfAttemptSubmitAdmissionAllowsVisibleSelfTestForRegularAuthenticatedActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(selfExecutionAdmissionFoundationStateReadService.findSelfExecutionAdmissionFoundationState(101L, 501L))
            .thenReturn(new SelfExecutionAdmissionFoundationStateReadService.SelfExecutionAdmissionFoundationState(501L));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_SUBMIT,
            CapabilityTargetEntityType.TEST,
            501L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

    @Test
    void selfAttemptAbandonAdmissionAllowsVisibleSelfTestForRegularAuthenticatedActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));
        when(selfExecutionAdmissionFoundationStateReadService.findSelfExecutionAdmissionFoundationState(101L, 501L))
            .thenReturn(new SelfExecutionAdmissionFoundationStateReadService.SelfExecutionAdmissionFoundationState(501L));

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_ABANDON,
            CapabilityTargetEntityType.TEST,
            501L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
    }

}


