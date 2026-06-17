package com.vladislav.training.platform.assignment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.service.NotificationCommandService;
import com.vladislav.training.platform.notification.service.NotificationQueryService;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AssignmentDeadlineNotificationSyncService {

    private static final NotificationChannel IN_APP_CHANNEL = new NotificationChannel("IN_APP");
    private static final NotificationStatus PENDING_STATUS = NotificationStatus.PENDING;
    private static final String SOURCE_ENTITY_TYPE = "ASSIGNMENT";
    private static final String MANAGER_ASSIGNED_TYPE = "assignment_assigned_manager_notice";
    private static final String OPERATOR_REMINDER_TYPE = "DEADLINE_REMINDER_7D";
    private static final String MANAGER_REMINDER_TYPE = "assignment_deadline_reminder_7d_manager_notice";
    private static final String OPERATOR_OVERDUE_TYPE = "assignment_overdue";
    private static final String MANAGER_OVERDUE_TYPE = "assignment_overdue_manager_notice";
    private static final Duration REMINDER_WINDOW = Duration.ofDays(7);

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final UserQueryService userQueryService;
    private final AssignmentNotificationAudienceResolver assignmentNotificationAudienceResolver;
    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final ObjectMapper objectMapper;

    public AssignmentDeadlineNotificationSyncService(
        AssignmentRepository assignmentRepository,
        CourseRepository courseRepository,
        UserQueryService userQueryService,
        AssignmentNotificationAudienceResolver assignmentNotificationAudienceResolver,
        NotificationQueryService notificationQueryService,
        NotificationCommandService notificationCommandService,
        ObjectMapper objectMapper
    ) {
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository, "assignmentRepository must not be null");
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

    public void materializeCurrentStateNotifications(Instant now) {
        Objects.requireNonNull(now, "now must not be null");

        for (Assignment assignment : assignmentRepository.findAllAssignments()) {
            if (!isActiveAssignment(assignment)) {
                continue;
            }

            materializeManagerAssignedNotifications(assignment, now);
            if (isOverdue(assignment, now)) {
                materializeOverdueNotifications(assignment, now);
                continue;
            }

            if (isReminderDue(assignment, now)) {
                materializeReminderNotifications(assignment, now);
            }
        }
    }

    private boolean isActiveAssignment(Assignment assignment) {
        return assignment.cancelledAt() == null
            && assignment.closedAt() == null
            && assignment.status() != AssignmentStatus.CANCELLED
            && assignment.status() != AssignmentStatus.COMPLETED;
    }

    private boolean isOverdue(Assignment assignment, Instant now) {
        return !assignment.deadlineAt().isAfter(now);
    }

    private boolean isReminderDue(Assignment assignment, Instant now) {
        return assignment.deadlineAt().isAfter(now)
            && !assignment.deadlineAt().isAfter(now.plus(REMINDER_WINDOW));
    }

    private void materializeReminderNotifications(Assignment assignment, Instant now) {
        AssignmentNotificationPayload payload = buildPayload(assignment);
        createIfAbsent(OPERATOR_REMINDER_TYPE, assignment.userId(), assignment, payload, now);
        for (Long managerUserId : resolveManagers(assignment, now)) {
            createIfAbsent(MANAGER_REMINDER_TYPE, managerUserId, assignment, payload, now);
        }
    }

    private void materializeOverdueNotifications(Assignment assignment, Instant now) {
        AssignmentNotificationPayload payload = buildPayload(assignment);
        createIfAbsent(OPERATOR_OVERDUE_TYPE, assignment.userId(), assignment, payload, now);
        for (Long managerUserId : resolveManagers(assignment, now)) {
            createIfAbsent(MANAGER_OVERDUE_TYPE, managerUserId, assignment, payload, now);
        }
    }

    private void materializeManagerAssignedNotifications(Assignment assignment, Instant now) {
        AssignmentNotificationPayload payload = buildPayload(assignment);
        for (Long managerUserId : resolveManagers(assignment, now)) {
            createIfAbsent(MANAGER_ASSIGNED_TYPE, managerUserId, assignment, payload, now);
        }
    }

    private Set<Long> resolveManagers(Assignment assignment, Instant now) {
        return new LinkedHashSet<>(assignmentNotificationAudienceResolver.resolveManagerUserIdsForUser(assignment.userId(), now));
    }

    private AssignmentNotificationPayload buildPayload(Assignment assignment) {
        Course course = courseRepository.findCourseById(assignment.courseId());
        AppUser subjectUser = userQueryService.findUserById(assignment.userId());
        return new AssignmentNotificationPayload(
            new AssignmentSnapshotPayload(
                assignment.id(),
                assignment.campaignId(),
                assignment.courseId(),
                course.name(),
                assignment.assignedAt(),
                assignment.deadlineAt()
            ),
            new SubjectUserPayload(
                subjectUser.id(),
                subjectUser.employeeNumber(),
                formatFullName(subjectUser)
            )
        );
    }

    private void createIfAbsent(
        String notificationType,
        Long recipientUserId,
        Assignment assignment,
        AssignmentNotificationPayload payload,
        Instant now
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
            now,
            null,
            null,
            0,
            null,
            null,
            serializePayload(payload),
            now,
            now
        );
        notificationCommandService.createNotification(notification);
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize assignment deadline notification payload", exception);
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

    private record AssignmentNotificationPayload(
        AssignmentSnapshotPayload assignment,
        SubjectUserPayload subjectUser
    ) {
    }

    private record AssignmentSnapshotPayload(
        Long assignmentId,
        Long campaignId,
        Long courseId,
        String courseName,
        Instant assignedAt,
        Instant deadlineAt
    ) {
    }

    private record SubjectUserPayload(
        Long userId,
        String employeeNumber,
        String fullName
    ) {
    }
}
