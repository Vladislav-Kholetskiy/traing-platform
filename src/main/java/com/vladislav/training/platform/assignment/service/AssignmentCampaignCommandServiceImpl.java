package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
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
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.OrganizationalTargetingQueryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code AssignmentCampaignCommandServiceImpl}.
 */
@Service
@Transactional
class AssignmentCampaignCommandServiceImpl implements AssignmentCampaignCommandService {

    private static final String RECIPIENT_INCLUSION_BASIS_CODE = "ORG_UNIT_TARGETING";

    private final AssignmentCampaignRepository assignmentCampaignRepository;
    private final AssignmentCampaignCourseRepository assignmentCampaignCourseRepository;
    private final AssignmentCampaignRecipientSnapshotRepository recipientSnapshotRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentCommandService assignmentCommandService;
    private final AssignmentStatusRecalculationService assignmentStatusRecalculationService;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final OrganizationQueryService organizationQueryService;
    private final OrganizationalTargetingQueryService organizationalTargetingQueryService;
    private final MandatoryAssignmentRecipientEligibilityService mandatoryRecipientEligibilityService;
    private final AssignmentCampaignRecipientSnapshotCaptureContract recipientSnapshotCaptureContract;
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final AssignmentCampaignLaunchNotificationService assignmentCampaignLaunchNotificationService;
    private final AssignmentCriticalAuditPlanner assignmentCriticalAuditPlanner = new AssignmentCriticalAuditPlannerImpl();
    private final AssignmentCriticalAuditPayloadFactory assignmentCriticalAuditPayloadFactory =
        new AssignmentCriticalAuditPayloadFactory();

    AssignmentCampaignCommandServiceImpl(
        AssignmentCampaignRepository assignmentCampaignRepository,
        AssignmentCampaignCourseRepository assignmentCampaignCourseRepository,
        AssignmentCampaignRecipientSnapshotRepository recipientSnapshotRepository,
        AssignmentRepository assignmentRepository,
        AssignmentCommandService assignmentCommandService,
        AssignmentStatusRecalculationService assignmentStatusRecalculationService,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        OrganizationQueryService organizationQueryService,
        OrganizationalTargetingQueryService organizationalTargetingQueryService,
        MandatoryAssignmentRecipientEligibilityService mandatoryRecipientEligibilityService,
        AssignmentCampaignRecipientSnapshotCaptureContract recipientSnapshotCaptureContract,
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
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
        this.organizationQueryService = organizationQueryService;
        this.organizationalTargetingQueryService = organizationalTargetingQueryService;
        this.mandatoryRecipientEligibilityService = mandatoryRecipientEligibilityService;
        this.recipientSnapshotCaptureContract = recipientSnapshotCaptureContract;
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.testRepository = testRepository;
        this.criticalCommandAuditSupport = criticalCommandAuditSupport;
        this.assignmentCampaignLaunchNotificationService = assignmentCampaignLaunchNotificationService;
    }

    @Override
    public AssignmentCampaign launchAssignmentCampaign(LaunchAssignmentCampaignCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        validateDistinctCourseIds(command.courseIds());

        CapabilityAdmissionRequest admissionRequest = capabilityAdmissionRequestFactory.createAssignmentCampaignLaunch(
            new AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchAdmissionAnchor(
                command.sourceType(),
                command.sourceRef()
            )
        );
        capabilityAdmissionPolicy.check(admissionRequest);

        Instant commandTime = admissionRequest.requestedAt();
        validateDeadlinePolicy(command, commandTime);

        TargetUnit targetUnit = resolveTargetUnit(command.targeting());
        List<ResolvedRecipient> recipients = resolveRecipients(targetUnit, commandTime);
        List<CourseLaunchPlan> courseLaunchPlans = resolveCourseLaunchPlans(command.courseIds());
        validateNoActiveAssignmentConflicts(recipients, courseLaunchPlans);

        AssignmentCampaign campaign = persistLaunchCampaignRoot(new AssignmentCampaign(
            null,
            command.name(),
            command.description(),
            command.sourceType(),
            normalizeOptional(command.sourceRef()),
            normalizeOptional(command.sourceNameSnapshot()),
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

        List<AssignmentCampaignRecipientSnapshot> recipientSnapshots = new ArrayList<>();
        for (ResolvedRecipient recipient : recipients) {
            recipientSnapshots.add(persistLaunchRecipientSnapshot(recipientSnapshotCaptureContract.capture(
                campaign.id(),
                recipient.userId(),
                recipient.organizationalUnitIdSnapshot(),
                recipient.organizationalPathSnapshot(),
                recipient.employeeNumberSnapshot(),
                recipient.fullNameSnapshot(),
                RECIPIENT_INCLUSION_BASIS_CODE,
                commandTime
            )));
        }

        int assignmentCount = 0;
        int assignmentTestCount = 0;
        for (ResolvedRecipient recipient : recipients) {
            for (CourseLaunchPlan courseLaunchPlan : courseLaunchPlans) {
                Assignment assignment = assignmentCommandService.createAssignment(new Assignment(
                    null,
                    campaign.id(),
                    recipient.userId(),
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
        }

        assignmentCampaignLaunchNotificationService.createLaunchNotifications(
            campaign,
            command,
            recipientSnapshots,
            commandTime
        );
        recordLaunchAudit(campaign, command, targetUnit, recipients.size(), assignmentCount, assignmentTestCount, admissionRequest);
        return campaign;
    }

    private AssignmentCampaign persistLaunchCampaignRoot(AssignmentCampaign assignmentCampaign) {
        Objects.requireNonNull(assignmentCampaign, "assignmentCampaign must not be null");
        return assignmentCampaignRepository.saveAssignmentCampaign(assignmentCampaign);
    }

    private AssignmentCampaignRecipientSnapshot persistLaunchRecipientSnapshot(
        AssignmentCampaignRecipientSnapshot assignmentCampaignRecipientSnapshot
    ) {
        Objects.requireNonNull(
            assignmentCampaignRecipientSnapshot,
            "assignmentCampaignRecipientSnapshot must not be null"
        );
        return recipientSnapshotRepository.saveAssignmentCampaignRecipientSnapshot(assignmentCampaignRecipientSnapshot);
    }

    private void validateDistinctCourseIds(List<Long> courseIds) {
        Set<Long> distinctCourseIds = new LinkedHashSet<>(courseIds);
        if (distinctCourseIds.size() != courseIds.size()) {
            throw new ValidationException("В запуске назначения не должно быть повторяющихся курсов");
        }
    }

    private void validateDeadlinePolicy(LaunchAssignmentCampaignCommand command, Instant commandTime) {
        if (command.deadlinePolicy().deadlineAt().isBefore(commandTime)) {
            throw new ValidationException("Срок выполнения не может быть раньше даты назначения");
        }
    }

    private TargetUnit resolveTargetUnit(LaunchAssignmentCampaignCommand.Targeting targeting) {
        AssignmentCampaignTargetingSupport.requireSupportedLaunchBasis(targeting.basisType());

        OrganizationalUnit organizationalUnit = organizationQueryService.findOrganizationalUnitByPath(targeting.basisRef());
        if (organizationalUnit.status() != OrganizationalUnitStatus.ACTIVE) {
            throw new ConflictException(
                "Нельзя назначать обучение на архивное подразделение: " + organizationalUnit.id()
            );
        }

        OrganizationalUnitType unitType = organizationQueryService.findOrganizationalUnitTypeById(
            organizationalUnit.organizationalUnitTypeId()
        );
        if (!unitType.canBeCampaignTarget()) {
            throw new ValidationException(
                "Тип подразделения не подходит для назначения обучения: " + organizationalUnit.id()
            );
        }

        return new TargetUnit(organizationalUnit.id(), organizationalUnit.path(), organizationalUnit.name());
    }

    private List<ResolvedRecipient> resolveRecipients(TargetUnit targetUnit, Instant commandTime) {
        Set<Long> candidateUserIds = organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree(
            targetUnit.path(),
            commandTime
        );
        if (candidateUserIds.isEmpty()) {
            throw new ConflictException(
                "Для выбранного подразделения не найдено ни одного получателя: " + targetUnit.path()
            );
        }

        List<Long> orderedCandidateIds = candidateUserIds.stream().sorted().toList();
        List<ResolvedRecipient> resolvedRecipients = new ArrayList<>(orderedCandidateIds.size());
        for (Long candidateUserId : orderedCandidateIds) {
            MandatoryAssignmentRecipientEligibilityService.MandatoryRecipientEligibility eligibility =
                mandatoryRecipientEligibilityService.evaluateRecipient(candidateUserId, targetUnit.path(), commandTime);
            if (!eligibility.eligible()) {
                continue;
            }

            resolvedRecipients.add(new ResolvedRecipient(
                eligibility.userId(),
                eligibility.organizationalUnitIdSnapshot(),
                eligibility.organizationalPathSnapshot(),
                eligibility.employeeNumber(),
                eligibility.fullNameSnapshot()
            ));
        }

        if (resolvedRecipients.isEmpty()) {
            throw new ConflictException(
                "Для выбранного подразделения не найдено подходящих получателей: " + targetUnit.path()
            );
        }

        return List.copyOf(resolvedRecipients);
    }

    private List<CourseLaunchPlan> resolveCourseLaunchPlans(List<Long> courseIds) {
        List<CourseLaunchPlan> courseLaunchPlans = new ArrayList<>(courseIds.size());
        for (Long courseId : courseIds) {
            Course course = courseRepository.findCourseById(courseId);
            if (course.status() != ContentStatus.PUBLISHED) {
                throw new ValidationException("Курс нельзя использовать для обязательного назначения: " + courseId);
            }

            List<Topic> topics = topicRepository.findTopicsByCourseId(courseId);
            if (topics.isEmpty()) {
                throw new ValidationException("У курса нет тем, поэтому его нельзя назначить: " + courseId);
            }

            List<TopicFinalControlPlan> finalControls = new ArrayList<>(topics.size());
            for (Topic topic : topics) {
                if (topic.status() != ContentStatus.PUBLISHED) {
                    throw new ValidationException(
                        "В курсе есть неопубликованная тема, поэтому его нельзя назначить: "
                            + courseId
                    );
                }

                Test finalControlTest = testRepository.findActiveFinalTestByTopicId(topic.id())
                    .orElseThrow(() -> new ValidationException(
                        "Для темы не найден активный итоговый тест: " + topic.id()
                    ));
                if (finalControlTest.testType() != TestType.CONTROL
                    || finalControlTest.status() != ContentStatus.PUBLISHED
                    || !finalControlTest.isActiveFinalForTopic()) {
                    throw new ValidationException(
                        "У темы нет корректного активного итогового теста: " + topic.id()
                    );
                }
                finalControls.add(new TopicFinalControlPlan(topic.id(), finalControlTest.id()));
            }

            courseLaunchPlans.add(new CourseLaunchPlan(course.id(), List.copyOf(finalControls)));
        }
        return List.copyOf(courseLaunchPlans);
    }

    private void validateNoActiveAssignmentConflicts(
        List<ResolvedRecipient> recipients,
        List<CourseLaunchPlan> courseLaunchPlans
    ) {
        for (ResolvedRecipient recipient : recipients) {
            for (CourseLaunchPlan courseLaunchPlan : courseLaunchPlans) {
                Assignment activeAssignment = assignmentRepository.findActiveAssignmentByUserIdAndCourseId(
                    recipient.userId(),
                    courseLaunchPlan.courseId()
                );
                if (activeAssignment != null) {
                    throw new ConflictException(
                        "Для пользователя уже есть активное назначение: userId=" + recipient.userId()
                            + ", courseId=" + courseLaunchPlan.courseId()
                    );
                }
            }
        }
    }

    private void recordLaunchAudit(
        AssignmentCampaign campaign,
        LaunchAssignmentCampaignCommand command,
        TargetUnit targetUnit,
        int recipientCount,
        int assignmentCount,
        int assignmentTestCount,
        CapabilityAdmissionRequest admissionRequest
    ) {
        AssignmentCriticalAuditPlanner.LaunchAuditPlan launchAuditPlan =
            assignmentCriticalAuditPlanner.planLaunchAudit(campaign.id());

        Map<String, Object> details = assignmentCriticalAuditPayloadFactory.launchDetails(
            command,
            targetUnit.id(),
            recipientCount,
            assignmentCount,
            assignmentTestCount
        );
        Map<String, Object> payloadAfter = assignmentCriticalAuditPayloadFactory.launchPayloadAfter(
            campaign,
            command,
            recipientCount,
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

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private record TargetUnit(
        Long id,
        String path,
        String name
    ) {
    }

    private record ResolvedRecipient(
        Long userId,
        Long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot,
        String employeeNumberSnapshot,
        String fullNameSnapshot
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


