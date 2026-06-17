package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса {@code NotificationAdminDispatchServiceImpl}.
 */

@Service
@Transactional
class NotificationAdminDispatchServiceImpl implements NotificationAdminDispatchService {

    private static final AccessReadArea SUPPORTED_CONTOUR = AccessReadArea.NOTIFICATION_ADMINISTRATION;
    private static final String DENIAL_MESSAGE = "Notification dispatch is forbidden by AccessSpecificationPolicy";

    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;
    private final NotificationDispatchService notificationDispatchService;
    private final UtcClock utcClock;

    NotificationAdminDispatchServiceImpl(
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver queryContextResolver,
        NotificationDispatchService notificationDispatchService,
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
        this.notificationDispatchService = Objects.requireNonNull(
            notificationDispatchService,
            "notificationDispatchService must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @Override
    public DispatchPendingNotificationsResult dispatchPendingNotifications(Long actorUserId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        AccessPolicyQueryContext context = queryContextResolver.resolveNotificationAdministrationContext(actorUserId);
        if (context.contour() != SUPPORTED_CONTOUR || !accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException("NOTIFICATION_ADMINISTRATION_DENIED", DENIAL_MESSAGE);
        }
        int processedCount = notificationDispatchService.dispatchPendingNotifications(utcClock.now(), limit);
        return new DispatchPendingNotificationsResult(processedCount);
    }
}
