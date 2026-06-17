package com.vladislav.training.platform.notification.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.notification.controller.dto.NotificationSelfReadResponse;
import com.vladislav.training.platform.notification.service.NotificationSelfReadService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code NotificationSelfReadController}.
 */

@RestController
@Validated
@RequestMapping("/api/v1/self/notifications")
class NotificationSelfReadController {

    private final NotificationSelfReadService notificationSelfReadService;
    private final InteractiveActorResolver interactiveActorResolver;

    NotificationSelfReadController(
        NotificationSelfReadService notificationSelfReadService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.notificationSelfReadService = Objects.requireNonNull(
            notificationSelfReadService,
            "notificationSelfReadService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @GetMapping
    List<NotificationSelfReadResponse> listSelfNotifications() {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return notificationSelfReadService.listSelfNotifications(actorUserId).stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{notificationId}")
    NotificationSelfReadResponse getSelfNotification(
        @PathVariable @Positive Long notificationId
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(notificationSelfReadService.findSelfNotificationById(actorUserId, notificationId));
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
