package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация командного сервиса {@code NotificationSelfCommandServiceImpl}.
 */

@Service
@Transactional
class NotificationSelfCommandServiceImpl implements NotificationSelfCommandService {

    private static final AccessReadArea SUPPORTED_CONTOUR = AccessReadArea.NOTIFICATION_RECIPIENT_SELF;
    private static final String DENIAL_MESSAGE = "Self notification command is forbidden by AccessSpecificationPolicy";

    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;
    private final NotificationCommandService notificationCommandService;
    private final NotificationSelfReadService notificationSelfReadService;
    private final UtcClock utcClock;

    NotificationSelfCommandServiceImpl(
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver queryContextResolver,
        NotificationCommandService notificationCommandService,
        NotificationSelfReadService notificationSelfReadService,
        UtcClock utcClock
    ) {
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.queryContextResolver = Objects.requireNonNull(
            queryContextResolver,
            "queryContextResolver must not be null"
        );
        this.notificationCommandService = Objects.requireNonNull(
            notificationCommandService,
            "notificationCommandService must not be null"
        );
        this.notificationSelfReadService = Objects.requireNonNull(
            notificationSelfReadService,
            "notificationSelfReadService must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @Override
    public NotificationSelfReadService.NotificationSelfReadModel markSelfNotificationRead(
        Long actorUserId,
        Long notificationId
    ) {
        AccessPolicyQueryContext context = queryContextResolver.resolveNotificationRecipientSelfDetailContext(
            actorUserId,
            notificationId
        );
        ensureAllowed(context);
        notificationCommandService.markNotificationRead(notificationId, actorUserId, utcClock.now());
        return notificationSelfReadService.findSelfNotificationById(actorUserId, notificationId);
    }

    @Override
    public MarkAllSelfNotificationsReadResult markAllSelfNotificationsRead(Long actorUserId) {
        AccessPolicyQueryContext context = queryContextResolver.resolveNotificationRecipientSelfContext(actorUserId);
        ensureAllowed(context);
        Instant readAt = utcClock.now();
        int updatedCount = notificationCommandService.markAllNotificationsRead(actorUserId, readAt);
        return new MarkAllSelfNotificationsReadResult(updatedCount, readAt);
    }

    private void ensureAllowed(AccessPolicyQueryContext context) {
        if (context.contour() != SUPPORTED_CONTOUR || !accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException("NOTIFICATION_RECIPIENT_SELF_DENIED", DENIAL_MESSAGE);
        }
    }
}
