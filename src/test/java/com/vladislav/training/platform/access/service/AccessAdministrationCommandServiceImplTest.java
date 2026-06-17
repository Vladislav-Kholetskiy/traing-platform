package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.AuditEventFactory;
import com.vladislav.training.platform.audit.service.AuditService;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет поведение {@code AccessAdministrationCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessAdministrationCommandServiceImplTest {

    private InteractiveActorResolver interactiveActorResolver;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T10:00:00Z");

    @Mock
    private UserAccessAreaCommandService userAccessAreaCommandService;
    @Mock
    private ManagementRelationCommandService managementRelationCommandService;
    @Mock
    private TemporaryRoleAssignmentCommandService temporaryRoleAssignmentCommandService;
    @Mock
    private TemporaryAccessAreaCommandService temporaryAccessAreaCommandService;
    @Mock
    private TemporaryManagementDelegationCommandService temporaryManagementDelegationCommandService;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private AuditService auditService;
    @Mock
    private AuditEventFactory auditEventFactory;
    @Mock
    private UtcClock utcClock;
    @Mock
    private ObjectMapper objectMapper;

    private AccessAdministrationCommandServiceImpl service;
    private CapabilityAdmissionRequest admissionRequest;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        interactiveActorResolver = new InteractiveActorResolver(new AuthenticatedActorAdapter());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            99L,
            "n/a",
            AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        ));
        service = new AccessAdministrationCommandServiceImpl(
            userAccessAreaCommandService,
            managementRelationCommandService,
            temporaryRoleAssignmentCommandService,
            temporaryAccessAreaCommandService,
            temporaryManagementDelegationCommandService,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            new CriticalCommandAuditSupport(
                interactiveActorResolver,
                auditEventFactory,
                auditService,
                utcClock,
                objectMapper
            ),
            utcClock
        );
        admissionRequest = new CapabilityAdmissionRequest(
            99L,
            "ACCESS_USER_ACCESS_AREA_ASSIGN",
            CapabilityTargetEntityType.USER_ACCESS_AREA,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(capabilityAdmissionRequestFactory.create(anyLong(), any(), any(), any())).thenReturn(admissionRequest);
        when(capabilityAdmissionRequestFactory.create(anyLong(), any(), any(), any(), any())).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditService.recordAuditEvent(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventFactory.createAuditEvent(
            any(AuditEventType.class),
            any(String.class),
            any(String.class),
            any(Long.class),
            any(Instant.class),
            any(),
            any(),
            any(AuditContext.class),
            any(),
            any(),
            any(Instant.class)
        )).thenAnswer(invocation -> new AuditEvent(
            null,
            invocation.getArgument(0),
            invocation.getArgument(1),
            invocation.getArgument(2),
            invocation.getArgument(3),
            invocation.getArgument(4),
            invocation.getArgument(5),
            invocation.getArgument(6),
            invocation.getArgument(7),
            invocation.getArgument(8),
            invocation.getArgument(9),
            invocation.getArgument(10)
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignUserAccessAreaUsesOwnerServiceThenWritesAudit() {
        when(userAccessAreaCommandService.saveUserAccessArea(any(UserAccessArea.class))).thenReturn(new UserAccessArea(
            10L,
            1L,
            null,
            AccessScopeType.GLOBAL,
            FIXED_INSTANT,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));

        service.assignUserAccessArea(1L, null, AccessScopeType.GLOBAL, FIXED_INSTANT);

        InOrder inOrder = inOrder(userAccessAreaCommandService, auditService);
        inOrder.verify(userAccessAreaCommandService).saveUserAccessArea(any(UserAccessArea.class));
        inOrder.verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void assignUserAccessAreaFailsWhenAuditWriteFails() {
        when(userAccessAreaCommandService.saveUserAccessArea(any(UserAccessArea.class))).thenReturn(new UserAccessArea(
            10L,
            1L,
            null,
            AccessScopeType.GLOBAL,
            FIXED_INSTANT,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(auditService.recordAuditEvent(any(AuditEvent.class))).thenThrow(new IllegalStateException("audit down"));

        assertThatThrownBy(() -> service.assignUserAccessArea(1L, null, AccessScopeType.GLOBAL, FIXED_INSTANT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit down");

        verify(userAccessAreaCommandService).saveUserAccessArea(any(UserAccessArea.class));
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void closeManagementRelationWritesAudit() {
        ManagementRelation current = new ManagementRelation(
            20L,
            1L,
            30L,
            500L,
            FIXED_INSTANT.minusSeconds(3600),
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        ManagementRelation closed = new ManagementRelation(
            20L,
            1L,
            30L,
            500L,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(managementRelationCommandService.findManagementRelationById(20L)).thenReturn(current, closed);

        service.closeManagementRelation(20L, FIXED_INSTANT);

        verify(managementRelationCommandService).endManagementRelation(20L, FIXED_INSTANT);
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void assignManagementRelationHasNoHiddenAccessSideEffect() {
        when(managementRelationCommandService.saveManagementRelation(any(ManagementRelation.class))).thenReturn(new ManagementRelation(
            20L,
            1L,
            30L,
            500L,
            FIXED_INSTANT,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));

        service.assignManagementRelation(1L, 30L, 500L, FIXED_INSTANT);

        verify(managementRelationCommandService).saveManagementRelation(any(ManagementRelation.class));
        verify(userAccessAreaCommandService, never()).saveUserAccessArea(any(UserAccessArea.class));
        verify(userAccessAreaCommandService, never()).revokeUserAccessArea(any(), any());
    }

    @Test
    void assignTemporaryRoleUsesOwnerServiceThenWritesAudit() {
        when(temporaryRoleAssignmentCommandService.saveTemporaryRoleAssignment(any(TemporaryRoleAssignment.class)))
            .thenReturn(new TemporaryRoleAssignment(30L, 1L, 900L, FIXED_INSTANT, null, FIXED_INSTANT, FIXED_INSTANT));

        service.assignTemporaryRoleAssignment(1L, 900L, FIXED_INSTANT);

        InOrder inOrder = inOrder(temporaryRoleAssignmentCommandService, auditService);
        inOrder.verify(temporaryRoleAssignmentCommandService).saveTemporaryRoleAssignment(any(TemporaryRoleAssignment.class));
        inOrder.verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void closeTemporaryManagementDelegationWritesAudit() {
        TemporaryManagementDelegation current = new TemporaryManagementDelegation(
            40L,
            1L,
            30L,
            500L,
            FIXED_INSTANT.minusSeconds(3600),
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        TemporaryManagementDelegation closed = new TemporaryManagementDelegation(
            40L,
            1L,
            30L,
            500L,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(temporaryManagementDelegationCommandService.findTemporaryManagementDelegationById(40L))
            .thenReturn(current, closed);

        service.closeTemporaryManagementDelegation(40L, FIXED_INSTANT);

        verify(temporaryManagementDelegationCommandService).endTemporaryManagementDelegation(40L, FIXED_INSTANT);
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void assignTemporaryAccessAreaHasNoPermanentAccessMutation() {
        when(temporaryAccessAreaCommandService.saveTemporaryAccessArea(any(TemporaryAccessArea.class))).thenReturn(
            new TemporaryAccessArea(50L, 1L, null, AccessScopeType.GLOBAL, FIXED_INSTANT, null, FIXED_INSTANT, FIXED_INSTANT)
        );

        service.assignTemporaryAccessArea(1L, null, AccessScopeType.GLOBAL, FIXED_INSTANT);

        verify(temporaryAccessAreaCommandService).saveTemporaryAccessArea(any(TemporaryAccessArea.class));
        verify(userAccessAreaCommandService, never()).saveUserAccessArea(any(UserAccessArea.class));
        verify(userAccessAreaCommandService, never()).revokeUserAccessArea(any(), any());
    }

    @Test
    void assignTemporaryManagementDelegationHasNoPermanentManagementOrAccessSideEffect() {
        when(temporaryManagementDelegationCommandService.saveTemporaryManagementDelegation(any(TemporaryManagementDelegation.class)))
            .thenReturn(new TemporaryManagementDelegation(
                60L,
                1L,
                30L,
                500L,
                FIXED_INSTANT,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));

        service.assignTemporaryManagementDelegation(1L, 30L, 500L, FIXED_INSTANT);

        verify(temporaryManagementDelegationCommandService).saveTemporaryManagementDelegation(any(TemporaryManagementDelegation.class));
        verify(managementRelationCommandService, never()).saveManagementRelation(any(ManagementRelation.class));
        verify(userAccessAreaCommandService, never()).saveUserAccessArea(any(UserAccessArea.class));
    }

    @Test
    void assignUserAccessAreaRejectsGlobalWithOrganizationalUnitId() {
        assertThatThrownBy(() -> service.assignUserAccessArea(1L, 30L, AccessScopeType.GLOBAL, FIXED_INSTANT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("organizationalUnitId must be null");
    }

    @Test
    void assignTemporaryAccessAreaRejectsScopedWithoutOrganizationalUnitId() {
        assertThatThrownBy(() -> service.assignTemporaryAccessArea(1L, null, AccessScopeType.UNIT_ONLY, FIXED_INSTANT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
    }
}



