package com.vladislav.training.platform.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.vladislav.training.platform.common.json.JacksonConfiguration;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.common.context.RequestContextHolder;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет вспомогательную логику {@code CriticalCommandAudit}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class CriticalCommandAuditSupportTest {

    private InteractiveActorResolver interactiveActorResolver;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-30T09:00:00Z");

    @Mock
    private AuditEventFactory auditEventFactory;
    @Mock
    private AuditService auditService;
    @Mock
    private UtcClock utcClock;
    @Mock
    private SystemActorResolver systemActorResolver;

    private CriticalCommandAuditSupport auditSupport;

    @BeforeEach
    void setUp() {
        interactiveActorResolver = new InteractiveActorResolver(new AuthenticatedActorAdapter());
        auditSupport = new CriticalCommandAuditSupport(
                interactiveActorResolver,
                auditEventFactory,
                auditService,
                utcClock,
                JsonMapper.builder().build()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.clear();
    }

    @Test
    void resolveInteractiveActorUserIdUsesAuthenticatedInteractiveActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_ADMIN"));

        assertThat(auditSupport.resolveInteractiveActorUserId()).isEqualTo(101L);
    }

    @Test
    void resolveSystemActorUserIdUsesExplicitTechnicalResolver() {
        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(900L);

        assertThat(auditSupport.resolveSystemActorUserId(systemActorResolver)).isEqualTo(900L);
    }

    @Test
    void recordAuditPersistsProvidedActorSynchronouslyForInteractiveFlow() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_ADMIN"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        AuditEvent auditEvent = new AuditEvent(
                null,
                new AuditEventType("userorg.app_user.updated"),
                "app_user",
                "15",
                101L,
                FIXED_INSTANT,
                null,
                null,
                new AuditContext("{\"operationCode\":\"USERORG_USER_UPDATE\"}"),
                null,
                null,
                FIXED_INSTANT
        );
        when(auditEventFactory.createAuditEvent(
                eq(auditEvent.eventType()),
                eq(auditEvent.entityType()),
                eq(auditEvent.entityId()),
                eq(auditEvent.actorUserId()),
                eq(auditEvent.occurredAt()),
                any(),
                any(),
                eq(auditEvent.contextPayload()),
                eq(auditEvent.correlationId()),
                eq(auditEvent.requestId()),
                eq(auditEvent.createdAt())
        )).thenReturn(auditEvent);
        when(auditService.recordAuditEvent(auditEvent)).thenReturn(auditEvent);

        auditSupport.recordAudit(
                101L,
                auditEvent.eventType(),
                auditEvent.entityType(),
                15L,
                Map.of("before", "state"),
                Map.of("after", "state"),
                auditEvent.contextPayload()
        );

        verify(auditService).recordAuditEvent(auditEvent);
    }

    @Test
    void resolveInteractiveActorUserIdRejectsMissingAuthenticationInsteadOfImplicitSystemFallback() {
        assertThatThrownBy(() -> auditSupport.resolveInteractiveActorUserId())
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("Authenticated principal is required");
    }

    @Test
    void recordAuditRejectsNullActorUserIdInsteadOfImplicitSystemFallback() {
        assertThatThrownBy(() -> auditSupport.recordAudit(
                null,
                new AuditEventType("userorg.app_user.updated"),
                "app_user",
                15L,
                null,
                null,
                new AuditContext("{}")
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("actorUserId must not be null");
    }

    @Test
    void recordAuditSerializesInstantPayloadWithProductionObjectMapper() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JacksonConfiguration.class)) {
            CriticalCommandAuditSupport productionAuditSupport = new CriticalCommandAuditSupport(
                    interactiveActorResolver,
                    auditEventFactory,
                    auditService,
                    utcClock,
                    context.getBean(com.fasterxml.jackson.databind.ObjectMapper.class)
            );
            when(utcClock.now()).thenReturn(FIXED_INSTANT);
            AuditEvent auditEvent = new AuditEvent(
                    null,
                    new AuditEventType("userorg.organizational_unit_type.created"),
                    "organizational_unit_type",
                    "10",
                    101L,
                    FIXED_INSTANT,
                    null,
                    null,
                    new AuditContext("{\"operationCode\":\"USERORG_ORG_UNIT_TYPE_CREATE\"}"),
                    null,
                    null,
                    FIXED_INSTANT
            );
            when(auditEventFactory.createAuditEvent(
                    eq(auditEvent.eventType()),
                    eq(auditEvent.entityType()),
                    eq(auditEvent.entityId()),
                    eq(auditEvent.actorUserId()),
                    eq(auditEvent.occurredAt()),
                    any(),
                    any(),
                    eq(auditEvent.contextPayload()),
                    eq(auditEvent.correlationId()),
                    eq(auditEvent.requestId()),
                    eq(auditEvent.createdAt())
            )).thenReturn(auditEvent);
            when(auditService.recordAuditEvent(auditEvent)).thenReturn(auditEvent);

            productionAuditSupport.recordAudit(
                    101L,
                    auditEvent.eventType(),
                    auditEvent.entityType(),
                    10L,
                    new InstantPayload(FIXED_INSTANT),
                    new InstantPayload(FIXED_INSTANT.plusSeconds(60)),
                    auditEvent.contextPayload()
            );

            verify(auditService).recordAuditEvent(auditEvent);
        }
    }

    private record InstantPayload(Instant recordedAt) {
    }
}
