package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
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
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentCampaignCommandServiceImplHappyPath}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentCampaignCommandServiceImplHappyPathTest {

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

    private final Map<Long, Assignment> assignmentsById = new LinkedHashMap<>();
    private final Map<Long, List<AssignmentTest>> assignmentTestsByAssignmentId = new LinkedHashMap<>();
    private final List<AssignmentCampaignCourse> savedCampaignCourses = new ArrayList<>();
    private final List<AssignmentCampaignRecipientSnapshot> savedRecipientSnapshots = new ArrayList<>();
    private final AtomicLong campaignIdSequence = new AtomicLong(1000L);
    private final AtomicLong campaignCourseIdSequence = new AtomicLong(2000L);
    private final AtomicLong snapshotIdSequence = new AtomicLong(3000L);
    private final AtomicLong assignmentIdSequence = new AtomicLong(4000L);
    private final AtomicLong assignmentTestIdSequence = new AtomicLong(5000L);

    private AssignmentCommandService assignmentCommandService;
    private AssignmentStatusRecalculationService assignmentStatusRecalculationService;
    private AssignmentCampaignCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        assignmentStatusRecalculationService = spy(
            new AssignmentStatusRecalculationServiceImpl(assignmentRepository, assignmentTestRepository)
        );
        assignmentCommandService = spy(new AssignmentCommandServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            assignmentStatusRecalculationService
        ));
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
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_CAMPAIGN_LAUNCH),
            anyMap()
        )).thenReturn(new AuditContext("{\"operation\":\"launch\"}"));
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(anyLong(), any()))
            .thenReturn(List.of());
        when(assignmentCampaignRepository.saveAssignmentCampaign(any())).thenAnswer(invocation -> {
            AssignmentCampaign campaign = invocation.getArgument(0, AssignmentCampaign.class);
            return new AssignmentCampaign(
                campaignIdSequence.incrementAndGet(),
                campaign.name(),
                campaign.description(),
                campaign.sourceType(),
                campaign.sourceRef(),
                campaign.sourceNameSnapshot(),
                campaign.createdAt(),
                campaign.updatedAt()
            );
        });
        when(assignmentCampaignCourseRepository.saveAssignmentCampaignCourse(any())).thenAnswer(invocation -> {
            AssignmentCampaignCourse course = invocation.getArgument(0, AssignmentCampaignCourse.class);
            AssignmentCampaignCourse saved = new AssignmentCampaignCourse(
                campaignCourseIdSequence.incrementAndGet(),
                course.campaignId(),
                course.courseId(),
                course.createdAt(),
                course.updatedAt()
            );
            savedCampaignCourses.add(saved);
            return saved;
        });
        when(recipientSnapshotRepository.saveAssignmentCampaignRecipientSnapshot(any())).thenAnswer(invocation -> {
            AssignmentCampaignRecipientSnapshot snapshot = invocation.getArgument(0, AssignmentCampaignRecipientSnapshot.class);
            AssignmentCampaignRecipientSnapshot saved = new AssignmentCampaignRecipientSnapshot(
                snapshotIdSequence.incrementAndGet(),
                snapshot.campaignId(),
                snapshot.userId(),
                snapshot.organizationalUnitIdSnapshot(),
                snapshot.organizationalPathSnapshot(),
                snapshot.inclusionBasisCode(),
                snapshot.employeeNumberSnapshot(),
                snapshot.fullNameSnapshot(),
                snapshot.capturedAt(),
                snapshot.createdAt()
            );
            savedRecipientSnapshots.add(saved);
            return saved;
        });
        when(assignmentRepository.saveAssignment(any())).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0, Assignment.class);
            Assignment saved = new Assignment(
                assignmentIdSequence.incrementAndGet(),
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
            assignmentsById.put(saved.id(), saved);
            return saved;
        });
        when(assignmentRepository.findAssignmentById(anyLong()))
            .thenAnswer(invocation -> assignmentsById.get(invocation.getArgument(0, Long.class)));
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(anyLong(), anyLong())).thenReturn(null);
        when(assignmentTestRepository.saveAssignmentTest(any())).thenAnswer(invocation -> {
            AssignmentTest assignmentTest = invocation.getArgument(0, AssignmentTest.class);
            AssignmentTest saved = new AssignmentTest(
                assignmentTestIdSequence.incrementAndGet(),
                assignmentTest.assignmentId(),
                assignmentTest.testId(),
                assignmentTest.assignmentTestRole(),
                assignmentTest.countedResultId(),
                assignmentTest.closedAt(),
                assignmentTest.isClosed(),
                assignmentTest.createdAt(),
                assignmentTest.updatedAt()
            );
            assignmentTestsByAssignmentId.computeIfAbsent(saved.assignmentId(), ignored -> new ArrayList<>()).add(saved);
            return saved;
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(anyLong())).thenAnswer(invocation ->
            List.copyOf(assignmentTestsByAssignmentId.getOrDefault(invocation.getArgument(0, Long.class), List.of()))
        );
    }

    @Test
    void launchCreatesCampaignCompositionSnapshotsAssignmentsTestsRefreshAndAudit() {
        assertThat(AssignmentCampaignTargetingSupport.supportedBasisTypes()).containsExactly("ORG_UNIT");
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops"))
            .thenReturn(unit(41L, 7L, "Operations", "/company/ops"));
        when(organizationQueryService.findOrganizationalUnitTypeById(7L)).thenReturn(type(7L, true, true));
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L, 202L));
        stubRecipient(201L, 301L, "/company/ops/line-1");
        stubRecipient(202L, 302L, "/company/ops/line-2");
        stubCourse(501L, List.of(601L, 602L), List.of(701L, 702L));
        stubCourse(502L, List.of(603L), List.of(703L));

        AssignmentCampaign launchedCampaign = service.launchAssignmentCampaign(new LaunchAssignmentCampaignCommand(
            "Запуск кампании назначений",
            "first runtime launch",
            "MANUAL",
            "launch-source-42",
            "Manual source snapshot",
            List.of(501L, 502L),
            new LaunchAssignmentCampaignCommand.Targeting("ORG_UNIT", "/company/ops"),
            new LaunchAssignmentCampaignCommand.DeadlinePolicy(FIXED_INSTANT.plusSeconds(86400))
        ));

        assertThat(launchedCampaign.id()).isEqualTo(1001L);
        assertThat(savedCampaignCourses).extracting(AssignmentCampaignCourse::courseId).containsExactly(501L, 502L);
        assertThat(savedRecipientSnapshots).extracting(AssignmentCampaignRecipientSnapshot::userId)
            .containsExactly(201L, 202L);
        assertThat(savedRecipientSnapshots).extracting(AssignmentCampaignRecipientSnapshot::organizationalPathSnapshot)
            .containsExactly("/company/ops/line-1", "/company/ops/line-2");
        assertThat(savedRecipientSnapshots).extracting(AssignmentCampaignRecipientSnapshot::organizationalUnitIdSnapshot)
            .containsExactly(301L, 302L);
        assertThat(savedRecipientSnapshots).extracting(AssignmentCampaignRecipientSnapshot::inclusionBasisCode)
            .containsOnly("ORG_UNIT_TARGETING");
        assertThat(savedRecipientSnapshots).extracting(AssignmentCampaignRecipientSnapshot::capturedAt)
            .containsOnly(FIXED_INSTANT);
        assertThat(savedRecipientSnapshots).extracting(AssignmentCampaignRecipientSnapshot::createdAt)
            .containsOnly(FIXED_INSTANT);
        assertThat(assignmentsById.values()).hasSize(4).extracting(Assignment::status)
            .containsOnly(AssignmentStatus.ASSIGNED);
        assertThat(assignmentsById.values()).extracting(Assignment::deadlineAt)
            .containsOnly(FIXED_INSTANT.plusSeconds(86400));
        assertThat(assignmentTestsByAssignmentId.values().stream().mapToInt(List::size).sum()).isEqualTo(6);
        verify(capabilityAdmissionRequestFactory).createAssignmentCampaignLaunch(
            new AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchAdmissionAnchor(
                "MANUAL",
                "launch-source-42"
            )
        );
        verify(capabilityAdmissionPolicy).check(admissionRequest());
        verify(assignmentCommandService, times(4)).createAssignment(any());
        verify(assignmentCommandService, times(6)).createAssignmentTest(any());
        verify(assignmentStatusRecalculationService, times(4)).refreshAssignmentStatusCache(anyLong(), eq(FIXED_INSTANT));
        verify(assignmentCampaignLaunchNotificationService).createLaunchNotifications(
            eq(launchedCampaign),
            any(LaunchAssignmentCampaignCommand.class),
            any(),
            eq(FIXED_INSTANT)
        );
        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment_campaign"),
            eq(launchedCampaign.id()),
            eq(null),
            any(),
            any()
        );
        ArgumentCaptor<Map<String, Object>> launchDetailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(criticalCommandAuditSupport).buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_CAMPAIGN_LAUNCH),
            launchDetailsCaptor.capture()
        );
        assertThat(launchDetailsCaptor.getValue())
            .containsEntry("commandType", "launch")
            .containsEntry("statusRefreshIntegrated", true)
            .containsEntry("targetingBasisType", "ORG_UNIT")
            .containsEntry("materializedRecipientCount", 2)
            .containsEntry("materializedAssignmentCount", 4)
            .containsEntry("materializedAssignmentTestCount", 6);
        InOrder inOrder = inOrder(capabilityAdmissionPolicy, assignmentCampaignRepository, criticalCommandAuditSupport);
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest());
        inOrder.verify(assignmentCampaignRepository).saveAssignmentCampaign(any());
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment_campaign"),
            eq(launchedCampaign.id()),
            eq(null),
            any(),
            any()
        );

        ArgumentCaptor<Object> payloadAfterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment_campaign"),
            eq(launchedCampaign.id()),
            eq(null),
            payloadAfterCaptor.capture(),
            any()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadAfter = (Map<String, Object>) payloadAfterCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> campaignPayload = (Map<String, Object>) payloadAfter.get("campaign");
        @SuppressWarnings("unchecked")
        Map<String, Object> commandPayload = (Map<String, Object>) payloadAfter.get("command");
        @SuppressWarnings("unchecked")
        Map<String, Object> materializationPayload = (Map<String, Object>) payloadAfter.get("materialization");
        assertThat(payloadAfter.keySet()).containsExactly("campaign", "command", "materialization");
        assertThat(campaignPayload).containsEntry("campaignId", launchedCampaign.id());
        assertThat(commandPayload)
            .containsEntry("targetingBasisType", "ORG_UNIT")
            .containsEntry("deadlineAt", FIXED_INSTANT.plusSeconds(86400));
        assertThat(materializationPayload)
            .containsEntry("recipientCount", 2)
            .containsEntry("assignmentCount", 4)
            .containsEntry("assignmentTestCount", 6);
    }

    @Test
    void launchAcceptsRecipientWithActiveTemporaryOperatorRole() {
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops"))
            .thenReturn(unit(41L, 7L, "Operations", "/company/ops"));
        when(organizationQueryService.findOrganizationalUnitTypeById(7L)).thenReturn(type(7L, true, true));
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L));
        when(userQueryService.findUserById(201L)).thenReturn(new AppUser(
            201L,
            "E-201",
            "user-201",
            "Temp",
            "Operator",
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(201L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(201L, true, Set.of("EMPLOYEE"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(201L, FIXED_INSTANT))
            .thenReturn(List.of(901L));
        when(userOrgFoundationStateReadService.findRoleCodesByIds(List.of(901L))).thenReturn(Set.of("OPERATOR"));
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(201L, FIXED_INSTANT))
            .thenReturn(List.of(new UserOrganizationAssignment(
                null,
                201L,
                301L,
                OrganizationAssignmentType.PRIMARY,
                FIXED_INSTANT.minusSeconds(3600),
                null,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT.minusSeconds(3600)
            )));
        when(organizationQueryService.findOrganizationalUnitById(301L))
            .thenReturn(unit(301L, 17L, "Line 301", "/company/ops/line-1"));
        when(organizationQueryService.findOrganizationalUnitTypeById(17L)).thenReturn(type(17L, true, true));
        stubCourse(501L, List.of(601L), List.of(701L));

        AssignmentCampaign launchedCampaign = service.launchAssignmentCampaign(new LaunchAssignmentCampaignCommand(
            "Запуск кампании назначений",
            null,
            "MANUAL",
            "launch-source-42",
            null,
            List.of(501L),
            new LaunchAssignmentCampaignCommand.Targeting("ORG_UNIT", "/company/ops"),
            new LaunchAssignmentCampaignCommand.DeadlinePolicy(FIXED_INSTANT.plusSeconds(86400))
        ));

        assertThat(launchedCampaign.id()).isEqualTo(1001L);
        assertThat(savedRecipientSnapshots).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.userId()).isEqualTo(201L);
            assertThat(snapshot.organizationalUnitIdSnapshot()).isEqualTo(301L);
            assertThat(snapshot.organizationalPathSnapshot()).isEqualTo("/company/ops/line-1");
            assertThat(snapshot.inclusionBasisCode()).isEqualTo("ORG_UNIT_TARGETING");
            assertThat(snapshot.capturedAt()).isEqualTo(FIXED_INSTANT);
            assertThat(snapshot.createdAt()).isEqualTo(FIXED_INSTANT);
        });
        assertThat(assignmentsById.values()).singleElement().extracting(Assignment::userId).isEqualTo(201L);
    }

    @Test
    void launchSkipsIneligibleRecipientsAndMaterializesOnlyEligibleOperators() {
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops"))
            .thenReturn(unit(41L, 7L, "Operations", "/company/ops"));
        when(organizationQueryService.findOrganizationalUnitTypeById(7L)).thenReturn(type(7L, true, true));
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .thenReturn(Set.of(201L, 202L));
        stubRecipient(201L, 301L, "/company/ops/line-1");
        when(userQueryService.findUserById(202L)).thenReturn(new AppUser(
            202L,
            "E-202",
            "user-202",
            "Ineligible",
            "User",
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(202L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(202L, true, Set.of("EMPLOYEE"))
        );
        stubCourse(501L, List.of(601L), List.of(701L));

        AssignmentCampaign launchedCampaign = service.launchAssignmentCampaign(new LaunchAssignmentCampaignCommand(
            "Запуск кампании назначений",
            null,
            "MANUAL",
            "launch-source-42",
            null,
            List.of(501L),
            new LaunchAssignmentCampaignCommand.Targeting("ORG_UNIT", "/company/ops"),
            new LaunchAssignmentCampaignCommand.DeadlinePolicy(FIXED_INSTANT.plusSeconds(86400))
        ));

        assertThat(launchedCampaign.id()).isEqualTo(1001L);
        assertThat(savedRecipientSnapshots).singleElement().extracting(AssignmentCampaignRecipientSnapshot::userId)
            .isEqualTo(201L);
        assertThat(assignmentsById.values()).singleElement().extracting(Assignment::userId).isEqualTo(201L);
        assertThat(assignmentTestsByAssignmentId.values().stream().mapToInt(List::size).sum()).isEqualTo(1);
    }

    private void stubRecipient(Long userId, Long homeUnitId, String homeUnitPath) {
        when(userQueryService.findUserById(userId)).thenReturn(new AppUser(
            userId,
            "E-" + userId,
            "user-" + userId,
            "User",
            "Name",
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(userId, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(userId, true, Set.of("OPERATOR"))
        );
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(userId, FIXED_INSTANT))
            .thenReturn(List.of(new UserOrganizationAssignment(
                null,
                userId,
                homeUnitId,
                OrganizationAssignmentType.PRIMARY,
                FIXED_INSTANT.minusSeconds(3600),
                null,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT.minusSeconds(3600)
            )));
        when(organizationQueryService.findOrganizationalUnitById(homeUnitId))
            .thenReturn(unit(homeUnitId, 17L, "Line " + homeUnitId, homeUnitPath));
        when(organizationQueryService.findOrganizationalUnitTypeById(17L)).thenReturn(type(17L, true, true));
    }

    private void stubCourse(Long courseId, List<Long> topicIds, List<Long> testIds) {
        when(courseRepository.findCourseById(courseId)).thenReturn(new Course(
            courseId,
            "Course " + courseId,
            null,
            ContentStatus.PUBLISHED,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        List<Topic> topics = new ArrayList<>();
        for (int i = 0; i < topicIds.size(); i++) {
            Long topicId = topicIds.get(i);
            topics.add(new Topic(
                topicId,
                courseId,
                "Topic " + topicId,
                null,
                ContentStatus.PUBLISHED,
                i + 1,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));
            when(testRepository.findActiveFinalTestByTopicId(topicId)).thenReturn(Optional.of(
                new com.vladislav.training.platform.content.domain.Test(
                    testIds.get(i),
                    topicId,
                    "Final control " + testIds.get(i),
                    null,
                    TestType.CONTROL,
                    ContentStatus.PUBLISHED,
                    BigDecimal.valueOf(80),
                    "DEFAULT",
                    true,
                    1,
                    FIXED_INSTANT,
                    FIXED_INSTANT
                )
            ));
        }
        when(topicRepository.findTopicsByCourseId(courseId)).thenReturn(topics);
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

    private OrganizationalUnit unit(Long id, Long typeId, String name, String path) {
        return new OrganizationalUnit(id, 11L, typeId, name, OrganizationalUnitStatus.ACTIVE, path, 2, "ou-" + id, FIXED_INSTANT, FIXED_INSTANT);
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
}

