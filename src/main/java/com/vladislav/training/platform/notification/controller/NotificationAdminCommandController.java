package com.vladislav.training.platform.notification.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.notification.controller.dto.NotificationDispatchResponse;
import com.vladislav.training.platform.notification.service.NotificationAdminDispatchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code NotificationAdminCommandController}.
 */

@RestController
@Validated
@RequestMapping("/api/v1/admin/notifications")
class NotificationAdminCommandController {

    private static final int DEFAULT_LIMIT = 100;

    private final NotificationAdminDispatchService notificationAdminDispatchService;
    private final InteractiveActorResolver interactiveActorResolver;

    NotificationAdminCommandController(
        NotificationAdminDispatchService notificationAdminDispatchService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.notificationAdminDispatchService = Objects.requireNonNull(
            notificationAdminDispatchService,
            "notificationAdminDispatchService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @PostMapping("/dispatch-pending")
    NotificationDispatchResponse dispatchPendingNotifications(
        @RequestParam(defaultValue = "" + DEFAULT_LIMIT) @Positive @Max(500) int limit
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        NotificationAdminDispatchService.DispatchPendingNotificationsResult result =
            notificationAdminDispatchService.dispatchPendingNotifications(actorUserId, limit);
        return new NotificationDispatchResponse(result.processedCount());
    }
}
