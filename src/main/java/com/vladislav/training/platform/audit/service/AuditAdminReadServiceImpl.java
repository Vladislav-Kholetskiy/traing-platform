package com.vladislav.training.platform.audit.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code AuditAdminReadServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class AuditAdminReadServiceImpl implements AuditAdminReadService {

    private static final AccessReadArea SUPPORTED_CONTOUR = AccessReadArea.AUDIT_EVENT_ADMINISTRATION;
    private static final String DENIAL_MESSAGE = "Audit administration read is forbidden by AccessSpecificationPolicy";

    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;
    private final AuditQueryService auditQueryService;

    public AuditAdminReadServiceImpl(
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver queryContextResolver,
        AuditQueryService auditQueryService
    ) {
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.queryContextResolver = Objects.requireNonNull(
            queryContextResolver,
            "queryContextResolver must not be null"
        );
        this.auditQueryService = Objects.requireNonNull(auditQueryService, "auditQueryService must not be null");
    }

    @Override
    public List<AuditEventReadModel> listAdminAuditEvents(Long actorUserId, AuditEventReadFilter filter) {
        AuditEventReadFilter effectiveFilter = filter == null
            ? new AuditEventReadFilter(null, null, null, null, null, null)
            : filter;
        AccessPolicyQueryContext context = queryContextResolver.resolveAuditEventAdministrationContext(actorUserId);
        ensureReadAllowed(context);

        return materializeCandidates(effectiveFilter).stream()
            .filter(matches(effectiveFilter))
            .sorted(Comparator
                .comparing(AuditEvent::occurredAt, Comparator.reverseOrder())
                .thenComparing(AuditEvent::id, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toReadModel)
            .toList();
    }

    @Override
    public AuditEventReadModel findAdminAuditEventById(Long actorUserId, Long auditEventId) {
        AccessPolicyQueryContext context = queryContextResolver.resolveAuditEventAdministrationDetailContext(
            actorUserId,
            auditEventId
        );
        ensureReadAllowed(context);
        return toReadModel(auditQueryService.findAuditEventById(auditEventId));
    }

    private void ensureReadAllowed(AccessPolicyQueryContext context) {
        if (context.contour() != SUPPORTED_CONTOUR || !accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException("AUDIT_EVENT_ADMINISTRATION_DENIED", DENIAL_MESSAGE);
        }
    }

    private List<AuditEvent> materializeCandidates(AuditEventReadFilter filter) {
        if (filter.entityType() != null) {
            return auditQueryService.findAuditEventsByEntity(filter.entityType(), filter.entityId());
        }
        if (filter.actorUserId() != null) {
            return auditQueryService.findAuditEventsByActorUserId(filter.actorUserId());
        }
        if (filter.eventType() != null) {
            return auditQueryService.findAuditEventsByEventType(new AuditEventType(filter.eventType()));
        }
        return auditQueryService.findAllAuditEvents();
    }

    private Predicate<AuditEvent> matches(AuditEventReadFilter filter) {
        return event ->
            (filter.eventType() == null || filter.eventType().equals(event.eventType().value()))
                && (filter.entityType() == null || filter.entityType().equals(event.entityType()))
                && (filter.entityId() == null || filter.entityId().equals(event.entityId()))
                && (filter.actorUserId() == null || filter.actorUserId().equals(event.actorUserId()))
                && (filter.occurredFrom() == null || !event.occurredAt().isBefore(filter.occurredFrom()))
                && (filter.occurredTo() == null || !event.occurredAt().isAfter(filter.occurredTo()));
    }

    private AuditEventReadModel toReadModel(AuditEvent event) {
        return new AuditEventReadModel(
            event.id(),
            event.eventType().value(),
            event.entityType(),
            event.entityId(),
            event.actorUserId(),
            event.occurredAt(),
            event.correlationId(),
            event.requestId(),
            event.createdAt()
        );
    }
}
