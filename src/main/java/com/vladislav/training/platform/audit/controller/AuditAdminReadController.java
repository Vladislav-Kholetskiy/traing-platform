package com.vladislav.training.platform.audit.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.audit.controller.dto.AuditEventReadResponse;
import com.vladislav.training.platform.audit.service.AuditAdminReadService;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code AuditAdminReadController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/admin/audit-events")
class AuditAdminReadController {

    private final AuditAdminReadService auditAdminReadService;
    private final InteractiveActorResolver interactiveActorResolver;

    AuditAdminReadController(
        AuditAdminReadService auditAdminReadService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.auditAdminReadService = Objects.requireNonNull(
            auditAdminReadService,
            "auditAdminReadService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @GetMapping
    List<AuditEventReadResponse> listAdminAuditEvents(
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String entityId,
        @RequestParam(required = false) Long actorUserId,
        @RequestParam(required = false) Instant occurredFrom,
        @RequestParam(required = false) Instant occurredTo
    ) {
        Long browsingActorUserId = interactiveActorResolver.resolveActorUserId();
        return auditAdminReadService.listAdminAuditEvents(
            browsingActorUserId,
            new AuditAdminReadService.AuditEventReadFilter(
                eventType,
                entityType,
                entityId,
                actorUserId,
                occurredFrom,
                occurredTo
            )
        ).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{auditEventId}")
    AuditEventReadResponse findAdminAuditEventById(@PathVariable @Positive Long auditEventId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(auditAdminReadService.findAdminAuditEventById(actorUserId, auditEventId));
    }

    private AuditEventReadResponse toResponse(AuditAdminReadService.AuditEventReadModel readModel) {
        return new AuditEventReadResponse(
            readModel.id(),
            readModel.eventType(),
            readModel.entityType(),
            readModel.entityId(),
            readModel.actorUserId(),
            readModel.occurredAt(),
            readModel.correlationId(),
            readModel.requestId(),
            readModel.createdAt()
        );
    }
}
