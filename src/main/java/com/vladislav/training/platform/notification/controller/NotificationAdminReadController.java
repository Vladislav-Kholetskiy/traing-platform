package com.vladislav.training.platform.notification.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.notification.controller.dto.NotificationAdminReadResponse;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.service.NotificationAdminReadService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code NotificationAdminReadController}.
 */

@RestController
@Validated
@RequestMapping("/api/v1/admin/notifications")
class NotificationAdminReadController {

    private final NotificationAdminReadService notificationAdminReadService;
    private final InteractiveActorResolver interactiveActorResolver;

    NotificationAdminReadController(
        NotificationAdminReadService notificationAdminReadService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.notificationAdminReadService = Objects.requireNonNull(
            notificationAdminReadService,
            "notificationAdminReadService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @GetMapping
    List<NotificationAdminReadResponse> listAdminNotifications(
        @RequestParam(required = false) NotificationStatus status,
        @RequestParam(required = false) String sourceEntityType,
        @RequestParam(required = false) String sourceEntityId,
        @RequestParam(required = false) String dedupKey
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return notificationAdminReadService.listAdminNotifications(
            actorUserId,
            new NotificationAdminReadService.NotificationAdminReadFilter(
                status,
                sourceEntityType,
                sourceEntityId,
                dedupKey
            )
        ).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{notificationId}")
    NotificationAdminReadResponse getAdminNotification(
        @PathVariable @Positive Long notificationId
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(notificationAdminReadService.findAdminNotificationById(actorUserId, notificationId));
    }

    private NotificationAdminReadResponse toResponse(NotificationAdminReadService.NotificationAdminReadModel readModel) {
        return new NotificationAdminReadResponse(
            readModel.id(),
            readModel.recipientUserId(),
            readModel.notificationType(),
            readModel.channelCode(),
            readModel.status(),
            readModel.sourceEntityType(),
            readModel.sourceEntityId(),
            readModel.scheduledAt(),
            readModel.sentAt(),
            readModel.readAt(),
            readModel.deliveryAttemptCount(),
            readModel.errorCode(),
            readModel.createdAt(),
            readModel.updatedAt()
        );
    }
}
