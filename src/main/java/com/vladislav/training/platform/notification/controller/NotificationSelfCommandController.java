package com.vladislav.training.platform.notification.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.notification.controller.dto.NotificationMarkAllReadResponse;
import com.vladislav.training.platform.notification.controller.dto.NotificationSelfReadResponse;
import com.vladislav.training.platform.notification.service.NotificationSelfCommandService;
import com.vladislav.training.platform.notification.service.NotificationSelfReadService;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code NotificationSelfCommandController}.
 */

@RestController
@Validated
@RequestMapping("/api/v1/self/notifications")
class NotificationSelfCommandController {

    private final NotificationSelfCommandService notificationSelfCommandService;
    private final InteractiveActorResolver interactiveActorResolver;

    NotificationSelfCommandController(
        NotificationSelfCommandService notificationSelfCommandService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.notificationSelfCommandService = Objects.requireNonNull(
            notificationSelfCommandService,
            "notificationSelfCommandService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @PostMapping("/{notificationId}/read")
    NotificationSelfReadResponse markSelfNotificationRead(
        @PathVariable @Positive Long notificationId
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(notificationSelfCommandService.markSelfNotificationRead(actorUserId, notificationId));
    }

    @PostMapping("/read-all")
    NotificationMarkAllReadResponse markAllSelfNotificationsRead() {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        NotificationSelfCommandService.MarkAllSelfNotificationsReadResult result =
            notificationSelfCommandService.markAllSelfNotificationsRead(actorUserId);
        return new NotificationMarkAllReadResponse(result.updatedCount(), result.readAt());
    }

    private NotificationSelfReadResponse toResponse(NotificationSelfReadService.NotificationSelfReadModel readModel) {
        return new NotificationSelfReadResponse(
            readModel.id(),
            readModel.title(),
            readModel.message(),
            readModel.channelCode(),
            readModel.createdAt(),
            readModel.readAt(),
            readModel.read(),
            readModel.notificationType(),
            readModel.companyName(),
            readModel.assignmentRecipients().stream()
                .map(recipient -> new NotificationSelfReadResponse.NotificationAssignmentRecipientResponse(
                    recipient.userId(),
                    recipient.fullName(),
                    recipient.courseName(),
                    recipient.companyName(),
                    recipient.organizationalUnitName()
                ))
                .toList()
        );
    }
}
