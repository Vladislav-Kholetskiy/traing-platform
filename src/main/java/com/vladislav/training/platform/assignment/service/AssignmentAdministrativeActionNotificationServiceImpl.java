package com.vladislav.training.platform.assignment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.service.NotificationCommandService;
import com.vladislav.training.platform.notification.service.NotificationQueryService;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AssignmentAdministrativeActionNotificationServiceImpl implements AssignmentAdministrativeActionNotificationService {

    private static final NotificationChannel IN_APP_CHANNEL = new NotificationChannel("IN_APP");
    private static final NotificationStatus PENDING_STATUS = NotificationStatus.PENDING;
    private static final String SOURCE_ENTITY_TYPE = "ASSIGNMENT";
    private static final String OPERATOR_CANCELLED_TYPE = "assignment_cancelled";
    private static final String MANAGER_CANCELLED_TYPE = "assignment_cancelled_manager_notice";
    private static final String OPERATOR_DEADLINE_EXTENDED_TYPE = "assignment_deadline_extended";
    private static final String MANAGER_DEADLINE_EXTENDED_TYPE = "assignment_deadline_extended_manager_notice";
    private static final String OPERATOR_REPLACED_TYPE = "assignment_replaced";
    private static final String MANAGER_REPLACED_TYPE = "assignment_replaced_manager_notice";

    private final CourseRepository courseRepository;
    private final UserQueryService userQueryService;
    private final AssignmentNotificationAudienceResolver assignmentNotificationAudienceResolver;
    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final ObjectMapper objectMapper;

    AssignmentAdministrativeActionNotificationServiceImpl(
        CourseRepository courseRepository,
        UserQueryService userQueryService,
        AssignmentNotificationAudienceResolver assignmentNotificationAudienceResolver,
        NotificationQueryService notificationQueryService,
        NotificationCommandService notificationCommandService,
        ObjectMapper objectMapper
    ) {
        this.courseRepository = Objects.requireNonNull(courseRepository, "courseRepository must not be null");
        this.userQueryService = Objects.requireNonNull(userQueryService, "userQueryService must not be null");
        this.assignmentNotificationAudienceResolver = Objects.requireNonNull(
            assignmentNotificationAudienceResolver,
            "assignmentNotificationAudienceResolver must not be null"
        );
        this.notificationQueryService = Objects.requireNonNull(
            notificationQueryService,
            "notificationQueryService must not be null"
        );
        this.notificationCommandService = Objects.requireNonNull(
            notificationCommandService,
            "notificationCommandService must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void createCancellationNotifications(
        Assignment assignmentBefore,
        Assignment assignmentAfter,
        Instant occurredAt
    ) {
        AssignmentActionNotificationPayload payload = buildPayload(assignmentAfter, assignmentBefore, null);
        createForAssigneeAndManagers(OPERATOR_CANCELLED_TYPE, MANAGER_CANCELLED_TYPE, assignmentAfter, payload, occurredAt);
    }

    @Override
    public void createDeadlineExtendedNotifications(
        Assignment assignmentBefore,
        Assignment assignmentAfter,
        Instant occurredAt
    ) {
        AssignmentActionNotificationPayload payload = buildPayload(assignmentAfter, assignmentBefore, null);
        createForAssigneeAndManagers(
            OPERATOR_DEADLINE_EXTENDED_TYPE,
            MANAGER_DEADLINE_EXTENDED_TYPE,
            assignmentAfter,
            payload,
            occurredAt
        );
    }

    @Override
    public void createReplacementNotifications(
        Assignment assignmentBefore,
        Assignment cancelledAssignmentAfter,
        Assignment replacementAssignmentAfter,
        Instant occurredAt
    ) {
        AssignmentActionNotificationPayload payload = buildPayload(
            replacementAssignmentAfter,
            assignmentBefore,
            cancelledAssignmentAfter
        );
        createForAssigneeAndManagers(OPERATOR_REPLACED_TYPE, MANAGER_REPLACED_TYPE, replacementAssignmentAfter, payload, occurredAt);
    }

    private void createForAssigneeAndManagers(
        String operatorNotificationType,
        String managerNotificationType,
        Assignment assignment,
        AssignmentActionNotificationPayload payload,
        Instant occurredAt
    ) {
        createIfAbsent(operatorNotificationType, assignment.userId(), assignment, payload, occurredAt);
        for (Long managerUserId : resolveManagers(assignment, occurredAt)) {
            createIfAbsent(managerNotificationType, managerUserId, assignment, payload, occurredAt);
        }
    }

    private Set<Long> resolveManagers(Assignment assignment, Instant occurredAt) {
        return new LinkedHashSet<>(
            assignmentNotificationAudienceResolver.resolveManagerUserIdsForUser(assignment.userId(), occurredAt)
        );
    }

    private AssignmentActionNotificationPayload buildPayload(
        Assignment assignment,
        Assignment previousAssignment,
        Assignment cancelledAssignment
    ) {
        Course course = courseRepository.findCourseById(assignment.courseId());
        AppUser subjectUser = userQueryService.findUserById(assignment.userId());
        return new AssignmentActionNotificationPayload(
            toAssignmentSnapshot(assignment, course),
            previousAssignment == null ? null : toAssignmentSnapshot(previousAssignment, course),
            cancelledAssignment == null ? null : toAssignmentSnapshot(cancelledAssignment, course),
            new SubjectUserPayload(
                subjectUser.id(),
                subjectUser.employeeNumber(),
                formatFullName(subjectUser)
            )
        );
    }

    private AssignmentSnapshotPayload toAssignmentSnapshot(Assignment assignment, Course course) {
        return new AssignmentSnapshotPayload(
            assignment.id(),
            assignment.campaignId(),
            assignment.courseId(),
            course.name(),
            assignment.assignedAt(),
            assignment.deadlineAt(),
            assignment.cancelledAt(),
            assignment.closedAt()
        );
    }

    private void createIfAbsent(
        String notificationType,
        Long recipientUserId,
        Assignment assignment,
        AssignmentActionNotificationPayload payload,
        Instant occurredAt
    ) {
        String dedupKey = notificationType + ":" + assignment.id() + ":" + recipientUserId;
        if (!notificationQueryService.findNotificationsByDedupKey(dedupKey).isEmpty()) {
            return;
        }

        Notification notification = new Notification(
            null,
            recipientUserId,
            notificationType,
            IN_APP_CHANNEL,
            PENDING_STATUS,
            dedupKey,
            SOURCE_ENTITY_TYPE,
            String.valueOf(assignment.id()),
            occurredAt,
            null,
            null,
            0,
            null,
            null,
            serializePayload(payload),
            occurredAt,
            occurredAt
        );
        notificationCommandService.createNotification(notification);
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize assignment administrative notification payload", exception);
        }
    }

    private String formatFullName(AppUser user) {
        StringBuilder fullName = new StringBuilder();
        fullName.append(user.lastName()).append(' ').append(user.firstName());
        if (user.middleName() != null && !user.middleName().isBlank()) {
            fullName.append(' ').append(user.middleName());
        }
        return fullName.toString();
    }

    private record AssignmentActionNotificationPayload(
        AssignmentSnapshotPayload assignment,
        AssignmentSnapshotPayload previousAssignment,
        AssignmentSnapshotPayload cancelledAssignment,
        SubjectUserPayload subjectUser
    ) {
    }

    private record AssignmentSnapshotPayload(
        Long assignmentId,
        Long campaignId,
        Long courseId,
        String courseName,
        Instant assignedAt,
        Instant deadlineAt,
        Instant cancelledAt,
        Instant closedAt
    ) {
    }

    private record SubjectUserPayload(
        Long userId,
        String employeeNumber,
        String fullName
    ) {
    }
}
