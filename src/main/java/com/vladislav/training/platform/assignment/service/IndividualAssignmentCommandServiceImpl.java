package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignCourseRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code IndividualAssignmentCommandServiceImpl}.
 */
@Service
@Transactional
class IndividualAssignmentCommandServiceImpl implements IndividualAssignmentCommandService {

    private static final String SOURCE_TYPE_INDIVIDUAL_USER = "INDIVIDUAL_USER";
    private static final String RECIPIENT_INCLUSION_BASIS_CODE = "INDIVIDUAL_USER_TARGETING";

    private final AssignmentCampaignRepository assignmentCampaignRepository;
    private final AssignmentCampaignCourseRepository assignmentCampaignCourseRepository;
    private final AssignmentCampaignRecipientSnapshotRepository recipientSnapshotRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentCommandService assignmentCommandService;
    private final AssignmentStatusRecalculationService assignmentStatusRecalculationService;
    private final MandatoryAssignmentRecipientEligibilityService mandatoryRecipientEligibilitySeam;
    private final AssignmentCampaignRecipientSnapshotCaptureContract recipientSnapshotCaptureContract;
    private final UserOrganizationAssignmentService userOrganizationAssignmentService;
    private final OrganizationQueryService organizationQueryService;
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final AssignmentCampaignLaunchNotificationService assignmentCampaignLaunchNotificationService;
    private final AssignmentCriticalAuditPlanner AssignmentCriticalAuditPlanner = new AssignmentCriticalAuditPlannerImpl();
    private final AssignmentCriticalAuditPayloadFactory assignmentCriticalAuditPayloadFactory =
        new AssignmentCriticalAuditPayloadFactory();

    IndividualAssignmentCommandServiceImpl(
        AssignmentCampaignRepository assignmentCampaignRepository,
        AssignmentCampaignCourseRepository assignmentCampaignCourseRepository,
        AssignmentCampaignRecipientSnapshotRepository recipientSnapshotRepository,
        AssignmentRepository assignmentRepository,
        AssignmentCommandService assignmentCommandService,
        AssignmentStatusRecalculationService assignmentStatusRecalculationService,
        MandatoryAssignmentRecipientEligibilityService mandatoryRecipientEligibilitySeam,
        AssignmentCampaignRecipientSnapshotCaptureContract recipientSnapshotCaptureContract,
        UserOrganizationAssignmentService userOrganizationAssignmentService,
        OrganizationQueryService organizationQueryService,
        CourseRepository courseRepository,
        TopicRepository topicRepository,
        TestRepository testRepository,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        AssignmentCampaignLaunchNotificationService assignmentCampaignLaunchNotificationService
    ) {
        this.assignmentCampaignRepository = assignmentCampaignRepository;
        this.assignmentCampaignCourseRepository = assignmentCampaignCourseRepository;
        this.recipientSnapshotRepository = recipientSnapshotRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentCommandService = assignmentCommandService;
        this.assignmentStatusRecalculationService = assignmentStatusRecalculationService;
        this.mandatoryRecipientEligibilitySeam = mandatoryRecipientEligibilitySeam;
        this.recipientSnapshotCaptureContract = recipientSnapshotCaptureContract;
        this.userOrganizationAssignmentService = userOrganizationAssignmentService;
        this.organizationQueryService = organizationQueryService;
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.testRepository = testRepository;
        this.criticalCommandAuditSupport = criticalCommandAuditSupport;
        this.assignmentCampaignLaunchNotificationService = assignmentCampaignLaunchNotificationService;
    }

    @Override
    public AssignmentCampaign launchIndividualAssignment(LaunchIndividualAssignmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        validateDistinctCourseIds(command.courseIds());

        Instant commandTime = Instant.now();
        validateDeadlinePolicy(command, commandTime);

        String targetUnitPath = resolvePrimaryTargetUnitPath(command.userId(), commandTime);
        MandatoryAssignmentRecipientEligibilityService.MandatoryRecipientEligibility eligibility =
            mandatoryRecipientEligibilitySeam.evaluateRecipient(command.userId(), targetUnitPath, commandTime);
        eligibility.requireEligibleForLaunch();

        TargetUnit targetUnit = new TargetUnit(
            eligibility.organizationalUnitIdSnapshot(),
            eligibility.organizationalPathSnapshot(),
            organizationQueryService.findOrganizationalUnitById(eligibility.organizationalUnitIdSnapshot()).name()
        );
        List<CourseLaunchPlan> courseLaunchPlans = resolveCourseLaunchPlans(command.courseIds());
        validateNoActiveAssignmentConflicts(eligibility.userId(), courseLaunchPlans);

        AssignmentCampaign campaign = assignmentCampaignRepository.saveAssignmentCampaign(new AssignmentCampaign(
            null,
            command.name(),
            command.description(),
            SOURCE_TYPE_INDIVIDUAL_USER,
            String.valueOf(command.userId()),
            eligibility.fullNameSnapshot(),
            commandTime,
            commandTime
        ));

        for (CourseLaunchPlan courseLaunchPlan : courseLaunchPlans) {
            assignmentCampaignCourseRepository.saveAssignmentCampaignCourse(new AssignmentCampaignCourse(
                null,
                campaign.id(),
                courseLaunchPlan.courseId(),
                commandTime,
                commandTime
            ));
        }

        AssignmentCampaignRecipientSnapshot recipientSnapshot =
            recipientSnapshotRepository.saveAssignmentCampaignRecipientSnapshot(recipientSnapshotCaptureContract.capture(
                campaign.id(),
                eligibility.userId(),
                eligibility.organizationalUnitIdSnapshot(),
                eligibility.organizationalPathSnapshot(),
                eligibility.employeeNumber(),
                eligibility.fullNameSnapshot(),
                RECIPIENT_INCLUSION_BASIS_CODE,
                commandTime
            ));

        int assignmentCount = 0;
        int assignmentTestCount = 0;
        for (CourseLaunchPlan courseLaunchPlan : courseLaunchPlans) {
            Assignment assignment = assignmentCommandService.createAssignment(new Assignment(
                null,
                campaign.id(),
                eligibility.userId(),
                courseLaunchPlan.courseId(),
                AssignmentStatus.ASSIGNED,
                commandTime,
                command.deadlinePolicy().deadlineAt(),
                null,
                null,
                commandTime,
                commandTime
            ));
            assignmentCount++;

            for (TopicFinalControlPlan topicFinalControlPlan : courseLaunchPlan.topicFinalControlPlans()) {
                assignmentCommandService.createAssignmentTest(new AssignmentTest(
                    null,
                    assignment.id(),
                    topicFinalControlPlan.testId(),
                    AssignmentTestRole.FINAL_TOPIC_CONTROL,
                    null,
                    null,
                    false,
                    commandTime,
                    commandTime
                ));
                assignmentTestCount++;
            }

            assignmentStatusRecalculationService.refreshAssignmentStatusCache(assignment.id(), commandTime);
        }

        LaunchAssignmentCampaignCommand notificationCommand = new LaunchAssignmentCampaignCommand(
            command.name(),
            command.description(),
            SOURCE_TYPE_INDIVIDUAL_USER,
            String.valueOf(command.userId()),
            eligibility.fullNameSnapshot(),
            command.courseIds(),
            new LaunchAssignmentCampaignCommand.Targeting("ORG_UNIT", targetUnit.path()),
            new LaunchAssignmentCampaignCommand.DeadlinePolicy(command.deadlinePolicy().deadlineAt())
        );

        assignmentCampaignLaunchNotificationService.createLaunchNotifications(
            campaign,
            notificationCommand,
            List.of(recipientSnapshot),
            commandTime
        );
        recordLaunchAudit(
            campaign,
            notificationCommand,
            targetUnit,
            assignmentCount,
            assignmentTestCount
        );
        return campaign;
    }

    private void validateDistinctCourseIds(List<Long> courseIds) {
        Set<Long> distinctCourseIds = new LinkedHashSet<>(courseIds);
        if (distinctCourseIds.size() != courseIds.size()) {
            throw new ValidationException("LaunchIndividualAssignment must not contain duplicate course ids");
        }
    }

    private void validateDeadlinePolicy(LaunchIndividualAssignmentCommand command, Instant commandTime) {
        if (command.deadlinePolicy().deadlineAt().isBefore(commandTime)) {
            throw new ValidationException("LaunchIndividualAssignment deadlineAt must not be earlier than assignedAt");
        }
    }

    private String resolvePrimaryTargetUnitPath(Long userId, Instant effectiveAt) {
        List<UserOrganizationAssignment> primaryAssignments = userOrganizationAssignmentService
            .findActiveOrganizationAssignmentsByUserId(userId, effectiveAt).stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .toList();
        if (primaryAssignments.size() != 1) {
            throw new ConflictException("Target user must have exactly one active PRIMARY home unit: " + userId);
        }
        return organizationQueryService.findOrganizationalUnitById(primaryAssignments.getFirst().organizationalUnitId()).path();
    }

    private List<CourseLaunchPlan> resolveCourseLaunchPlans(List<Long> courseIds) {
        List<CourseLaunchPlan> courseLaunchPlans = new ArrayList<>(courseIds.size());
        for (Long courseId : courseIds) {
            Course course = courseRepository.findCourseById(courseId);
            if (course.status() != ContentStatus.PUBLISHED) {
                throw new ValidationException("РљСѓСЂСЃ РЅРµРґРѕСЃС‚СѓРїРµРЅ РґР»СЏ РёРЅРґРёРІРёРґСѓР°Р»СЊРЅРѕРіРѕ РЅР°Р·РЅР°С‡РµРЅРёСЏ: " + courseId);
            }

            List<Topic> topics = topicRepository.findTopicsByCourseId(courseId);
            if (topics.isEmpty()) {
                throw new ValidationException("РЈ РєСѓСЂСЃР° РЅРµС‚ С‚РµРј РґР»СЏ РёРЅРґРёРІРёРґСѓР°Р»СЊРЅРѕРіРѕ РЅР°Р·РЅР°С‡РµРЅРёСЏ: " + courseId);
            }

            List<TopicFinalControlPlan> finalControls = new ArrayList<>(topics.size());
            for (Topic topic : topics) {
                if (topic.status() != ContentStatus.PUBLISHED) {
                    throw new ValidationException(
                        "РљСѓСЂСЃ СЃРѕРґРµСЂР¶РёС‚ РЅРµРѕРїСѓР±Р»РёРєРѕРІР°РЅРЅСѓСЋ С‚РµРјСѓ Рё РЅРµРґРѕСЃС‚СѓРїРµРЅ РґР»СЏ РёРЅРґРёРІРёРґСѓР°Р»СЊРЅРѕРіРѕ РЅР°Р·РЅР°С‡РµРЅРёСЏ: "
                            + courseId
                    );
                }

                Test finalControlTest = testRepository.findActiveFinalTestByTopicId(topic.id())
                    .orElseThrow(() -> new ValidationException(
                        "Missing current active final control test for topic: " + topic.id()
                    ));
                if (finalControlTest.testType() != TestType.CONTROL
                    || finalControlTest.status() != ContentStatus.PUBLISHED
                    || !finalControlTest.isActiveFinalForTopic()) {
                    throw new ValidationException(
                        "Topic does not have a valid active final control test: " + topic.id()
                    );
                }
                finalControls.add(new TopicFinalControlPlan(topic.id(), finalControlTest.id()));
            }

            courseLaunchPlans.add(new CourseLaunchPlan(course.id(), List.copyOf(finalControls)));
        }
        return List.copyOf(courseLaunchPlans);
    }

    private void validateNoActiveAssignmentConflicts(Long userId, List<CourseLaunchPlan> courseLaunchPlans) {
        for (CourseLaunchPlan courseLaunchPlan : courseLaunchPlans) {
            Assignment activeAssignment = assignmentRepository.findActiveAssignmentByUserIdAndCourseId(
                userId,
                courseLaunchPlan.courseId()
            );
            if (activeAssignment != null) {
                throw new ConflictException(
                    "Active assignment already exists for userId=" + userId + ", courseId=" + courseLaunchPlan.courseId()
                );
            }
        }
    }

    private void recordLaunchAudit(
        AssignmentCampaign campaign,
        LaunchAssignmentCampaignCommand command,
        TargetUnit targetUnit,
        int assignmentCount,
        int assignmentTestCount
    ) {
        AssignmentCriticalAuditPlanner.LaunchAuditPlan launchAuditPlan =
            AssignmentCriticalAuditPlanner.planLaunchAudit(campaign.id());

        Map<String, Object> details = assignmentCriticalAuditPayloadFactory.launchDetails(
            command,
            targetUnit.id(),
            1,
            assignmentCount,
            assignmentTestCount
        );
        Map<String, Object> payloadAfter = assignmentCriticalAuditPayloadFactory.launchPayloadAfter(
            campaign,
            command,
            1,
            assignmentCount,
            assignmentTestCount
        );

        criticalCommandAuditSupport.recordAudit(
            criticalCommandAuditSupport.resolveInteractiveActorUserId(),
            launchAuditPlan.auditCatalog().auditEventType(),
            launchAuditPlan.auditEntityType(),
            launchAuditPlan.auditEntityId(),
            null,
            payloadAfter,
            criticalCommandAuditSupport.buildAuditContext(
                "Assignment",
                launchAuditPlan.auditCatalog().operationCode(),
                details
            )
        );
    }

    private record TargetUnit(
        Long id,
        String path,
        String name
    ) {
    }

    private record CourseLaunchPlan(
        Long courseId,
        List<TopicFinalControlPlan> topicFinalControlPlans
    ) {
    }

    private record TopicFinalControlPlan(
        Long topicId,
        Long testId
    ) {
    }
}


