package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса {@code NotificationAdminReadServiceImpl}.
 */

@Service
@Transactional(readOnly = true)
public class NotificationAdminReadServiceImpl implements NotificationAdminReadService {

    private static final AccessReadArea SUPPORTED_CONTOUR = AccessReadArea.NOTIFICATION_ADMINISTRATION;
    private static final String DENIAL_MESSAGE = "Notification administration read is forbidden by AccessSpecificationPolicy";

    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;
    private final NotificationQueryService notificationQueryService;

    public NotificationAdminReadServiceImpl(
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver queryContextResolver,
        NotificationQueryService notificationQueryService
    ) {
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.queryContextResolver = Objects.requireNonNull(
            queryContextResolver,
            "queryContextResolver must not be null"
        );
        this.notificationQueryService = Objects.requireNonNull(
            notificationQueryService,
            "notificationQueryService must not be null"
        );
    }

    @Override
    public List<NotificationAdminReadModel> listAdminNotifications(
        Long actorUserId,
        NotificationAdminReadFilter filter
    ) {
        NotificationAdminReadFilter effectiveFilter = filter == null
            ? new NotificationAdminReadFilter(null, null, null, null)
            : filter;
        AccessPolicyQueryContext context = queryContextResolver.resolveNotificationAdministrationContext(actorUserId);
        ensureReadAllowed(context);

        return materializeCandidates(effectiveFilter).stream()
            .filter(matches(effectiveFilter))
            .sorted(Comparator
                .comparing(Notification::createdAt, Comparator.reverseOrder())
                .thenComparing(Notification::id, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toReadModel)
            .toList();
    }

    @Override
    public NotificationAdminReadModel findAdminNotificationById(Long actorUserId, Long notificationId) {
        AccessPolicyQueryContext context = queryContextResolver.resolveNotificationAdministrationDetailContext(
            actorUserId,
            notificationId
        );
        ensureReadAllowed(context);
        return toReadModel(notificationQueryService.findNotificationById(notificationId));
    }

    private void ensureReadAllowed(AccessPolicyQueryContext context) {
        if (context.contour() != SUPPORTED_CONTOUR || !accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException("NOTIFICATION_ADMINISTRATION_DENIED", DENIAL_MESSAGE);
        }
    }

    private List<Notification> materializeCandidates(NotificationAdminReadFilter filter) {
        List<Notification> baseSlice;
        if (filter.dedupKey() != null) {
            baseSlice = notificationQueryService.findNotificationsByDedupKey(filter.dedupKey());
        } else if (filter.sourceEntityType() != null) {
            baseSlice = notificationQueryService.findNotificationsBySourceEntity(
                filter.sourceEntityType(),
                filter.sourceEntityId()
            );
        } else if (filter.status() != null) {
            baseSlice = notificationQueryService.findNotificationsByStatus(filter.status());
        } else {
            baseSlice = Arrays.stream(NotificationStatus.values())
                .flatMap(status -> notificationQueryService.findNotificationsByStatus(status).stream())
                .toList();
        }

        Map<Long, Notification> deduplicated = new LinkedHashMap<>();
        for (Notification notification : baseSlice) {
            deduplicated.putIfAbsent(notification.id(), notification);
        }
        return List.copyOf(deduplicated.values());
    }

    private Predicate<Notification> matches(NotificationAdminReadFilter filter) {
        return notification ->
            (filter.status() == null || notification.status() == filter.status())
                && (filter.dedupKey() == null || filter.dedupKey().equals(notification.dedupKey()))
                && (filter.sourceEntityType() == null || filter.sourceEntityType().equals(notification.sourceEntityType()))
                && (filter.sourceEntityId() == null || filter.sourceEntityId().equals(notification.sourceEntityId()));
    }

    private NotificationAdminReadModel toReadModel(Notification notification) {
        return new NotificationAdminReadModel(
            notification.id(),
            notification.recipientUserId(),
            notification.notificationType(),
            notification.channelCode(),
            notification.status(),
            notification.sourceEntityType(),
            notification.sourceEntityId(),
            notification.scheduledAt(),
            notification.sentAt(),
            notification.readAt(),
            notification.deliveryAttemptCount(),
            notification.errorCode(),
            notification.createdAt(),
            notification.updatedAt()
        );
    }
}
