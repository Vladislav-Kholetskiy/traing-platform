package com.vladislav.training.platform.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.domain.AuditPayload;
import com.vladislav.training.platform.common.context.CurrentRequestContext;
import com.vladislav.training.platform.common.context.RequestContextHolder;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Вспомогательный тип {@code CriticalCommandAuditSupport}.
 */
@Service
public class CriticalCommandAuditSupport {

    private final InteractiveActorResolver interactiveActorResolver;
    private final AuditEventFactory auditEventFactory;
    private final AuditService auditService;
    private final UtcClock utcClock;
    private final ObjectMapper objectMapper;

    public CriticalCommandAuditSupport(
        InteractiveActorResolver interactiveActorResolver,
        AuditEventFactory auditEventFactory,
        AuditService auditService,
        UtcClock utcClock,
        ObjectMapper objectMapper
    ) {
        this.interactiveActorResolver = interactiveActorResolver;
        this.auditEventFactory = auditEventFactory;
        this.auditService = auditService;
        this.utcClock = utcClock;
        this.objectMapper = objectMapper;
    }

    public Long resolveInteractiveActorUserId() {
        return interactiveActorResolver.resolveActorUserId();
    }

    public Long resolveSystemActorUserId(SystemActorResolver systemActorResolver) {
        Objects.requireNonNull(systemActorResolver, "systemActorResolver must not be null");
        return systemActorResolver.resolveSystemActorUserId();
    }

    public void recordAudit(
        Long actorUserId,
        AuditEventType eventType,
        String entityType,
        Long entityId,
        Object payloadBefore,
        Object payloadAfter,
        AuditContext contextPayload
    ) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null for synchronous audit companion");
        Instant now = utcClock.now();
        AuditEvent auditEvent = auditEventFactory.createAuditEvent(
            eventType,
            entityType,
            String.valueOf(entityId),
            actorUserId,
            now,
            toAuditPayload(payloadBefore),
            toAuditPayload(payloadAfter),
            contextPayload,
            currentCorrelationId(),
            currentRequestId(),
            now
        );
        auditService.recordAuditEvent(auditEvent);
    }

    public AuditContext buildAuditContext(String targetModule, String operationCode, Map<String, Object> details) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("operationCode", operationCode);
        context.put("targetModule", targetModule);
        context.put("details", details);
        return new AuditContext(writeJson(context));
    }

    public AuditContext buildAuditContext(
        String targetModule,
        com.vladislav.training.platform.application.policy.CapabilityOperationCode operationCode,
        Map<String, Object> details
    ) {
        return buildAuditContext(targetModule, operationCode.code(), details);
    }

    private AuditPayload toAuditPayload(Object payloadSource) {
        if (payloadSource == null) {
            return null;
        }
        return new AuditPayload(writeJson(payloadSource));
    }

    private String currentCorrelationId() {
        return RequestContextHolder.getCurrent()
            .map(CurrentRequestContext::correlationId)
            .orElse(null);
    }

    private String currentRequestId() {
        return RequestContextHolder.getCurrent()
            .map(CurrentRequestContext::requestId)
            .orElse(null);
    }

    private String writeJson(Object payloadSource) {
        try {
            return objectMapper.writeValueAsString(payloadSource);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit payload", exception);
        }
    }
}
