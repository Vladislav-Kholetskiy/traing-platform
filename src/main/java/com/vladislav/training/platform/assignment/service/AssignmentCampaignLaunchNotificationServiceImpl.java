package com.vladislav.training.platform.assignment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.service.NotificationCommandService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AssignmentCampaignLaunchNotificationServiceImpl implements AssignmentCampaignLaunchNotificationService {

    private static final NotificationChannel IN_APP_CHANNEL = new NotificationChannel("IN_APP");
    private static final NotificationStatus PENDING_STATUS = NotificationStatus.PENDING;
    private static final String SOURCE_ENTITY_TYPE = "assignment_campaign";
    private static final String OPERATOR_NOTIFICATION_TYPE = "assignment_campaign_assigned";
    private static final String MANAGER_NOTIFICATION_TYPE = "assignment_campaign_manager_notice";

    private final NotificationCommandService notificationCommandService;
    private final AssignmentNotificationAudienceResolver assignmentNotificationAudienceResolver;
    private final ObjectMapper objectMapper;

    AssignmentCampaignLaunchNotificationServiceImpl(
        NotificationCommandService notificationCommandService,
        AssignmentNotificationAudienceResolver assignmentNotificationAudienceResolver,
        ObjectMapper objectMapper
    ) {
        this.notificationCommandService = Objects.requireNonNull(
            notificationCommandService,
            "notificationCommandService must not be null"
        );
        this.assignmentNotificationAudienceResolver = Objects.requireNonNull(
            assignmentNotificationAudienceResolver,
            "assignmentNotificationAudienceResolver must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void createLaunchNotifications(
        AssignmentCampaign campaign,
        LaunchAssignmentCampaignCommand command,
        List<AssignmentCampaignRecipientSnapshot> recipientSnapshots,
        Instant launchedAt
    ) {
        Objects.requireNonNull(campaign, "campaign must not be null");
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(recipientSnapshots, "recipientSnapshots must not be null");
        Objects.requireNonNull(launchedAt, "launchedAt must not be null");

        if (recipientSnapshots.isEmpty()) {
            return;
        }

        NotificationCampaignPayload campaignPayload = new NotificationCampaignPayload(
            campaign.id(),
            campaign.name(),
            campaign.description(),
            campaign.sourceType(),
            campaign.sourceRef(),
            campaign.sourceNameSnapshot(),
            command.deadlinePolicy().deadlineAt(),
            List.copyOf(command.courseIds())
        );

        for (AssignmentCampaignRecipientSnapshot recipientSnapshot : recipientSnapshots) {
            createOperatorNotification(campaignPayload, recipientSnapshot, launchedAt);
        }

        Map<Long, LinkedHashMap<Long, RecipientSnapshotPayload>> recipientsByManagerUserId = new LinkedHashMap<>();

        for (AssignmentCampaignRecipientSnapshot recipientSnapshot : recipientSnapshots) {
            Set<Long> managerUserIds = resolveManagerUserIds(recipientSnapshot.organizationalUnitIdSnapshot(), launchedAt);
            for (Long managerUserId : managerUserIds) {
                recipientsByManagerUserId.computeIfAbsent(managerUserId, ignored -> new LinkedHashMap<>())
                    .put(
                        recipientSnapshot.userId(),
                        new RecipientSnapshotPayload(
                            recipientSnapshot.userId(),
                            recipientSnapshot.employeeNumberSnapshot(),
                            recipientSnapshot.fullNameSnapshot(),
                            recipientSnapshot.organizationalUnitIdSnapshot(),
                            recipientSnapshot.organizationalPathSnapshot()
                        )
                    );
            }
        }

        for (Map.Entry<Long, LinkedHashMap<Long, RecipientSnapshotPayload>> entry : recipientsByManagerUserId.entrySet()) {
            createManagerNotification(campaignPayload, entry.getKey(), List.copyOf(entry.getValue().values()), launchedAt);
        }
    }

    private void createOperatorNotification(
        NotificationCampaignPayload campaignPayload,
        AssignmentCampaignRecipientSnapshot recipientSnapshot,
        Instant launchedAt
    ) {
        Notification notification = new Notification(
            null,
            recipientSnapshot.userId(),
            OPERATOR_NOTIFICATION_TYPE,
            IN_APP_CHANNEL,
            PENDING_STATUS,
            buildDedupKey(OPERATOR_NOTIFICATION_TYPE, campaignPayload.campaignId(), recipientSnapshot.userId()),
            SOURCE_ENTITY_TYPE,
            String.valueOf(campaignPayload.campaignId()),
            launchedAt,
            null,
            null,
            0,
            null,
            null,
            serializePayload(new OperatorLaunchNotificationPayload(
                campaignPayload,
                new RecipientSnapshotPayload(
                    recipientSnapshot.userId(),
                    recipientSnapshot.employeeNumberSnapshot(),
                    recipientSnapshot.fullNameSnapshot(),
                    recipientSnapshot.organizationalUnitIdSnapshot(),
                    recipientSnapshot.organizationalPathSnapshot()
                )
            )),
            launchedAt,
            launchedAt
        );
        notificationCommandService.createNotification(notification);
    }

    private void createManagerNotification(
        NotificationCampaignPayload campaignPayload,
        Long managerUserId,
        List<RecipientSnapshotPayload> affectedRecipients,
        Instant launchedAt
    ) {
        Notification notification = new Notification(
            null,
            managerUserId,
            MANAGER_NOTIFICATION_TYPE,
            IN_APP_CHANNEL,
            PENDING_STATUS,
            buildDedupKey(MANAGER_NOTIFICATION_TYPE, campaignPayload.campaignId(), managerUserId),
            SOURCE_ENTITY_TYPE,
            String.valueOf(campaignPayload.campaignId()),
            launchedAt,
            null,
            null,
            0,
            null,
            null,
            serializePayload(new ManagerLaunchNotificationPayload(
                campaignPayload,
                affectedRecipients.size(),
                affectedRecipients
            )),
            launchedAt,
            launchedAt
        );
        notificationCommandService.createNotification(notification);
    }

    private Set<Long> resolveManagerUserIds(
        Long organizationalUnitId,
        Instant activeAt
    ) {
        return assignmentNotificationAudienceResolver.resolveManagerUserIdsForUnit(organizationalUnitId, activeAt);
    }

    private String buildDedupKey(String notificationType, Long campaignId, Long recipientUserId) {
        return notificationType + ":" + campaignId + ":" + recipientUserId;
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize assignment campaign notification payload", exception);
        }
    }

    private record NotificationCampaignPayload(
        Long campaignId,
        String campaignName,
        String campaignDescription,
        String sourceType,
        String sourceRef,
        String sourceNameSnapshot,
        Instant deadlineAt,
        List<Long> courseIds
    ) {
    }

    private record RecipientSnapshotPayload(
        Long userId,
        String employeeNumber,
        String fullName,
        Long organizationalUnitId,
        String organizationalPath
    ) {
    }

    private record OperatorLaunchNotificationPayload(
        NotificationCampaignPayload campaign,
        RecipientSnapshotPayload recipient
    ) {
    }

    private record ManagerLaunchNotificationPayload(
        NotificationCampaignPayload campaign,
        int affectedRecipientCount,
        List<RecipientSnapshotPayload> affectedRecipients
    ) {
    }
}
