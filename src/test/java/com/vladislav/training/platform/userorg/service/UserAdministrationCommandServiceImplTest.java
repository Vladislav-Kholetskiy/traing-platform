package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.service.ManagementRelationCommandService;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaCommandService;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationCommandService;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentCommandService;
import com.vladislav.training.platform.access.service.UserAccessAreaCommandService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.domain.AuditPayload;
import com.vladislav.training.platform.audit.service.AuditEventFactory;
import com.vladislav.training.platform.audit.service.AuditService;
import com.vladislav.training.platform.common.context.CurrentRequestContext;
import com.vladislav.training.platform.common.context.RequestContextHolder;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет поведение {@code UserAdministrationCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAdministrationCommandServiceImplTest {

    private InteractiveActorResolver interactiveActorResolver;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private UserCommandService userCommandService;
    @Mock
    private UserQueryService userQueryService;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private UserRoleAssignmentService userRoleAssignmentService;
    @Mock
    private UserOrganizationAssignmentService userOrganizationAssignmentService;
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
    private AuditService auditService;
    @Mock
    private AuditEventFactory auditEventFactory;
    @Mock
    private UtcClock utcClock;

    private UserAdministrationCommandServiceImpl service;
    private CapabilityAdmissionRequest admissionRequest;

    @BeforeEach
    void setUp() {
        interactiveActorResolver = new InteractiveActorResolver(new AuthenticatedActorAdapter());
        service = new UserAdministrationCommandServiceImpl(
            userCommandService,
            userQueryService,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            userRoleAssignmentService,
            userOrganizationAssignmentService,
            userAccessAreaCommandService,
            managementRelationCommandService,
            temporaryRoleAssignmentCommandService,
            temporaryAccessAreaCommandService,
            temporaryManagementDelegationCommandService,
            new CriticalCommandAuditSupport(
                interactiveActorResolver,
                auditEventFactory,
                auditService,
                utcClock,
                testObjectMapper()
            ),
            utcClock
        );
        admissionRequest = new CapabilityAdmissionRequest(
            101L,
            "USERORG_USER_CREATE",
            CapabilityTargetEntityType.APP_USER,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(capabilityAdmissionRequestFactory.create(anyLong(), any(), any(), any())).thenReturn(admissionRequest);
        when(capabilityAdmissionRequestFactory.create(anyLong(), any(), any(), any(), any())).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(auditService.recordAuditEvent(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventFactory.createAuditEvent(
            any(AuditEventType.class),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenAnswer(invocation -> new AuditEvent(
            null,
            invocation.getArgument(0, AuditEventType.class),
            invocation.getArgument(1, String.class),
            invocation.getArgument(2, String.class),
            invocation.getArgument(3, Long.class),
            invocation.getArgument(4, Instant.class),
            invocation.getArgument(5, AuditPayload.class),
            invocation.getArgument(6, AuditPayload.class),
            invocation.getArgument(7, AuditContext.class),
            invocation.getArgument(8, String.class),
            invocation.getArgument(9, String.class),
            invocation.getArgument(10, Instant.class)
        ));
        authenticateAs(101L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.clear();
    }

    @Test
    void createUserWritesAuditAfterOwnerMutation() {
        RequestContextHolder.set(new CurrentRequestContext("corr-1", "req-1"));
        AppUser command = user(null, "EMP-1", UserStatus.ACTIVE);
        AppUser created = user(1L, "EMP-1", UserStatus.ACTIVE);
        when(userCommandService.createUser(any(AppUser.class))).thenReturn(created);

        AppUser result = service.createUser(command);

        assertThat(result.id()).isEqualTo(1L);
        InOrder inOrder = inOrder(userCommandService, auditService);
        inOrder.verify(userCommandService).createUser(any(AppUser.class));
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        inOrder.verify(auditService).recordAuditEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().actorUserId()).isEqualTo(101L);
        assertThat(auditCaptor.getValue().correlationId()).isEqualTo("corr-1");
        assertThat(auditCaptor.getValue().requestId()).isEqualTo("req-1");
    }

    @Test
    void createUserFailsWhenAuditWriteFails() {
        AppUser command = user(null, "EMP-1", UserStatus.ACTIVE);
        AppUser created = user(1L, "EMP-1", UserStatus.ACTIVE);
        when(userCommandService.createUser(any(AppUser.class))).thenReturn(created);
        when(auditService.recordAuditEvent(any(AuditEvent.class))).thenThrow(new IllegalStateException("audit down"));

        assertThatThrownBy(() -> service.createUser(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit down");

        verify(userCommandService).createUser(any(AppUser.class));
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void updateUserLoadsCurrentStateKeepsCreateOnlyIdentifiersAndWritesAudit() {
        AppUser current = new AppUser(
            5L,
            "EMP-5",
            "EXT-5",
            "Last",
            "First",
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(3600)
        );
        AppUser updated = new AppUser(
            5L,
            "EMP-5",
            "EXT-5",
            "Updated",
            "User",
            null,
            UserStatus.ACTIVE,
            current.createdAt(),
            FIXED_INSTANT
        );
        when(userQueryService.findUserById(5L)).thenReturn(current);
        when(userCommandService.updateUser(any(AppUser.class))).thenReturn(updated);

        AppUser result = service.updateUser(5L, "Updated", "User", null);

        assertThat(result.employeeNumber()).isEqualTo("EMP-5");
        assertThat(result.externalId()).isEqualTo("EXT-5");
        ArgumentCaptor<AppUser> updateCaptor = ArgumentCaptor.forClass(AppUser.class);
        InOrder inOrder = inOrder(userQueryService, userCommandService, auditService);
        inOrder.verify(userQueryService).findUserById(5L);
        inOrder.verify(userCommandService).updateUser(updateCaptor.capture());
        inOrder.verify(auditService).recordAuditEvent(any(AuditEvent.class));
        assertThat(updateCaptor.getValue().employeeNumber()).isEqualTo("EMP-5");
        assertThat(updateCaptor.getValue().externalId()).isEqualTo("EXT-5");
        assertThat(updateCaptor.getValue().lastName()).isEqualTo("Updated");
        assertThat(updateCaptor.getValue().firstName()).isEqualTo("User");
    }

    @Test
    void updateUserDoesNotWriteAuditWhenOwnerFlowRejectsDescriptiveMutation() {
        AppUser current = new AppUser(
            5L,
            "EMP-5",
            "EXT-5",
            "Last",
            "First",
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(3600)
        );
        when(userQueryService.findUserById(5L)).thenReturn(current);
        when(userCommandService.updateUser(any(AppUser.class)))
            .thenThrow(new ConflictException("update rejected"));

        assertThatThrownBy(() -> service.updateUser(5L, "Updated", "User", null))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("update rejected");

        verifyNoInteractions(auditService);
    }

    @Test
    void assignRoleWritesAudit() {
        UserRoleAssignment created = roleAssignment(10L, 7L, 100L, FIXED_INSTANT, null);
        when(userRoleAssignmentService.assignRoleAssignment(any(UserRoleAssignment.class))).thenReturn(created);

        UserRoleAssignment result = service.assignRole(7L, 100L, FIXED_INSTANT);

        assertThat(result.id()).isEqualTo(10L);
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void closeRoleRejectsAssignmentBelongingToAnotherUser() {
        when(userRoleAssignmentService.findRoleAssignmentById(11L)).thenReturn(roleAssignment(11L, 8L, 100L, FIXED_INSTANT, null));

        assertThatThrownBy(() -> service.closeRole(7L, 11L, FIXED_INSTANT.plusSeconds(60)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("assignmentId=11");

        verify(userRoleAssignmentService, never()).closeRoleAssignment(anyLong(), any(Instant.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void assignOrganizationAssignmentWritesAudit() {
        UserOrganizationAssignment created = organizationAssignment(20L, 7L, 30L, OrganizationAssignmentType.PRIMARY, FIXED_INSTANT, null);
        when(userOrganizationAssignmentService.assignOrganizationAssignment(any(UserOrganizationAssignment.class))).thenReturn(created);

        UserOrganizationAssignment result = service.assignOrganizationAssignment(
            7L,
            30L,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT
        );

        assertThat(result.id()).isEqualTo(20L);
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void closeOrganizationAssignmentWritesAudit() {
        UserOrganizationAssignment current = organizationAssignment(20L, 7L, 30L, OrganizationAssignmentType.SECONDARY, FIXED_INSTANT, null);
        UserOrganizationAssignment closed = organizationAssignment(
            20L,
            7L,
            30L,
            OrganizationAssignmentType.SECONDARY,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(120)
        );
        when(userOrganizationAssignmentService.findOrganizationAssignmentById(20L)).thenReturn(current);
        when(userOrganizationAssignmentService.closeOrganizationAssignment(20L, FIXED_INSTANT.plusSeconds(120))).thenReturn(closed);

        UserOrganizationAssignment result = service.closeOrganizationAssignment(7L, 20L, FIXED_INSTANT.plusSeconds(120));

        assertThat(result.validTo()).isEqualTo(FIXED_INSTANT.plusSeconds(120));
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void replacePrimaryHomeUnitUsesDedicatedOwnerFlowAndWritesAudit() {
        UserOrganizationAssignment currentPrimary = organizationAssignment(
            40L,
            7L,
            30L,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        );
        UserOrganizationAssignment replacement = organizationAssignment(
            41L,
            7L,
            31L,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT,
            null
        );
        when(userOrganizationAssignmentService.findActiveOrganizationAssignmentsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(currentPrimary));
        when(userOrganizationAssignmentService.replacePrimaryHomeUnit(7L, 31L, FIXED_INSTANT)).thenReturn(replacement);

        UserOrganizationAssignment result = service.replacePrimaryHomeUnit(7L, 31L, FIXED_INSTANT);

        assertThat(result.organizationalUnitId()).isEqualTo(31L);
        verify(userOrganizationAssignmentService).replacePrimaryHomeUnit(7L, 31L, FIXED_INSTANT);
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void deactivateUserPerformsSingleOuterAdmissionThenOwnerCloseBatchesThenAudit() {
        AppUser currentUser = user(7L, "EMP-7", UserStatus.ACTIVE);
        when(userQueryService.findUserById(7L)).thenReturn(currentUser);
        when(userRoleAssignmentService.closeActiveRoleAssignmentsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(roleAssignment(11L, 7L, 100L, FIXED_INSTANT.minusSeconds(3600), FIXED_INSTANT)));
        when(userOrganizationAssignmentService.closeActiveOrganizationAssignmentsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(organizationAssignment(13L, 7L, 30L, OrganizationAssignmentType.PRIMARY, FIXED_INSTANT.minusSeconds(3600), FIXED_INSTANT)));
        when(userAccessAreaCommandService.closeActiveUserAccessAreasByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(userAccessArea(17L, 7L)));
        when(managementRelationCommandService.closeActiveManagementRelationsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(managementRelation(18L, 7L)));
        when(temporaryRoleAssignmentCommandService.closeActiveTemporaryRoleAssignmentsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(tempRoleAssignment(19L, 7L, 101L)));
        when(temporaryAccessAreaCommandService.closeActiveTemporaryAccessAreasByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(tempAccessArea(20L, 7L)));
        when(temporaryManagementDelegationCommandService.closeActiveTemporaryManagementDelegationsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(tempManagementDelegation(21L, 7L)));
        when(userCommandService.deactivateUserAfterAdmission(7L, FIXED_INSTANT)).thenReturn(user(7L, "EMP-7", UserStatus.INACTIVE));

        AppUser result = service.deactivateUser(7L);

        assertThat(result.status()).isEqualTo(UserStatus.INACTIVE);
        InOrder inOrder = inOrder(
            capabilityAdmissionPolicy,
            userRoleAssignmentService,
            userOrganizationAssignmentService,
            userAccessAreaCommandService,
            managementRelationCommandService,
            temporaryRoleAssignmentCommandService,
            temporaryAccessAreaCommandService,
            temporaryManagementDelegationCommandService,
            userCommandService,
            auditService
        );
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        inOrder.verify(userRoleAssignmentService).closeActiveRoleAssignmentsByUserId(7L, FIXED_INSTANT);
        inOrder.verify(userOrganizationAssignmentService).closeActiveOrganizationAssignmentsByUserId(7L, FIXED_INSTANT);
        inOrder.verify(userAccessAreaCommandService).closeActiveUserAccessAreasByUserId(7L, FIXED_INSTANT);
        inOrder.verify(managementRelationCommandService).closeActiveManagementRelationsByUserId(7L, FIXED_INSTANT);
        inOrder.verify(temporaryRoleAssignmentCommandService).closeActiveTemporaryRoleAssignmentsByUserId(7L, FIXED_INSTANT);
        inOrder.verify(temporaryAccessAreaCommandService).closeActiveTemporaryAccessAreasByUserId(7L, FIXED_INSTANT);
        inOrder.verify(temporaryManagementDelegationCommandService)
            .closeActiveTemporaryManagementDelegationsByUserId(7L, FIXED_INSTANT);
        inOrder.verify(userCommandService).deactivateUserAfterAdmission(7L, FIXED_INSTANT);
        inOrder.verify(auditService).recordAuditEvent(any(AuditEvent.class));
        verify(userRoleAssignmentService, never()).closeRoleAssignment(anyLong(), any(Instant.class));
        verify(userOrganizationAssignmentService, never()).closeOrganizationAssignment(anyLong(), any(Instant.class));
        verify(temporaryRoleAssignmentCommandService, never()).endTemporaryRoleAssignment(anyLong(), any(Instant.class));
        verify(userAccessAreaCommandService, never()).revokeUserAccessArea(anyLong(), any(Instant.class));
        verify(managementRelationCommandService, never()).endManagementRelation(anyLong(), any(Instant.class));
        verify(temporaryAccessAreaCommandService, never()).endTemporaryAccessArea(anyLong(), any(Instant.class));
        verify(temporaryManagementDelegationCommandService, never()).endTemporaryManagementDelegation(anyLong(), any(Instant.class));
    }

    @Test
    void deactivateUserStopsBeforeOwnerCloseWhenOuterAdmissionFails() {
        AppUser currentUser = user(7L, "EMP-7", UserStatus.ACTIVE);
        when(userQueryService.findUserById(7L)).thenReturn(currentUser);
        org.mockito.Mockito.doThrow(new ConflictException("blocked by policy")).when(capabilityAdmissionPolicy).check(admissionRequest);

        assertThatThrownBy(() -> service.deactivateUser(7L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("blocked by policy");

        verify(userRoleAssignmentService, never()).closeActiveRoleAssignmentsByUserId(anyLong(), any(Instant.class));
        verify(userOrganizationAssignmentService, never()).closeActiveOrganizationAssignmentsByUserId(anyLong(), any(Instant.class));
        verify(userAccessAreaCommandService, never()).closeActiveUserAccessAreasByUserId(anyLong(), any(Instant.class));
        verify(managementRelationCommandService, never()).closeActiveManagementRelationsByUserId(anyLong(), any(Instant.class));
        verify(temporaryRoleAssignmentCommandService, never()).closeActiveTemporaryRoleAssignmentsByUserId(anyLong(), any(Instant.class));
        verify(temporaryAccessAreaCommandService, never()).closeActiveTemporaryAccessAreasByUserId(anyLong(), any(Instant.class));
        verify(temporaryManagementDelegationCommandService, never())
            .closeActiveTemporaryManagementDelegationsByUserId(anyLong(), any(Instant.class));
        verify(userCommandService, never()).deactivateUserAfterAdmission(anyLong(), any(Instant.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void deactivateUserPassingAdmissionDoesNotGuaranteeSuccessWhenOwnerCloseFails() {
        AppUser currentUser = user(7L, "EMP-7", UserStatus.ACTIVE);
        when(userQueryService.findUserById(7L)).thenReturn(currentUser);
        when(userRoleAssignmentService.closeActiveRoleAssignmentsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(roleAssignment(11L, 7L, 100L, FIXED_INSTANT.minusSeconds(3600), FIXED_INSTANT)));
        when(userOrganizationAssignmentService.closeActiveOrganizationAssignmentsByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of(organizationAssignment(13L, 7L, 30L, OrganizationAssignmentType.PRIMARY, FIXED_INSTANT.minusSeconds(3600), FIXED_INSTANT)));
        when(userAccessAreaCommandService.closeActiveUserAccessAreasByUserId(7L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(managementRelationCommandService.closeActiveManagementRelationsByUserId(7L, FIXED_INSTANT))
            .thenThrow(new ConflictException("management close failed"));

        assertThatThrownBy(() -> service.deactivateUser(7L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("management close failed");

        verify(capabilityAdmissionPolicy).check(admissionRequest);
        verify(userCommandService, never()).deactivateUserAfterAdmission(anyLong(), any(Instant.class));
        verifyNoInteractions(auditService);
    }

    private ObjectMapper testObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
                jsonGenerator.writeString(value.toString());
            }
        });
        return JsonMapper.builder().addModule(module).build();
    }

    private void authenticateAs(Long actorUserId) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(actorUserId, null, "ROLE_ADMIN"));
    }

    private AppUser user(Long id, String employeeNumber, UserStatus status) {
        return new AppUser(id, employeeNumber, null, "Last", "First", null, status, FIXED_INSTANT, FIXED_INSTANT);
    }

    private UserRoleAssignment roleAssignment(Long id, Long userId, Long roleId, Instant validFrom, Instant validTo) {
        return new UserRoleAssignment(id, userId, roleId, validFrom, validTo, FIXED_INSTANT, FIXED_INSTANT);
    }

    private UserOrganizationAssignment organizationAssignment(
        Long id,
        Long userId,
        Long unitId,
        OrganizationAssignmentType assignmentType,
        Instant validFrom,
        Instant validTo
    ) {
        return new UserOrganizationAssignment(id, userId, unitId, assignmentType, validFrom, validTo, FIXED_INSTANT, FIXED_INSTANT);
    }

    private TemporaryRoleAssignment tempRoleAssignment(Long id, Long userId, Long roleId) {
        return new TemporaryRoleAssignment(id, userId, roleId, FIXED_INSTANT.minusSeconds(3600), null, FIXED_INSTANT, FIXED_INSTANT);
    }

    private UserAccessArea userAccessArea(Long id, Long userId) {
        return new UserAccessArea(id, userId, null, com.vladislav.training.platform.access.domain.AccessScopeType.GLOBAL, FIXED_INSTANT.minusSeconds(3600), FIXED_INSTANT, FIXED_INSTANT, FIXED_INSTANT);
    }

    private ManagementRelation managementRelation(Long id, Long userId) {
        return new ManagementRelation(id, userId, 30L, 500L, FIXED_INSTANT.minusSeconds(3600), FIXED_INSTANT, FIXED_INSTANT, FIXED_INSTANT);
    }

    private TemporaryAccessArea tempAccessArea(Long id, Long userId) {
        return new TemporaryAccessArea(id, userId, null, com.vladislav.training.platform.access.domain.AccessScopeType.GLOBAL, FIXED_INSTANT.minusSeconds(3600), FIXED_INSTANT, FIXED_INSTANT, FIXED_INSTANT);
    }

    private TemporaryManagementDelegation tempManagementDelegation(Long id, Long userId) {
        return new TemporaryManagementDelegation(id, userId, 30L, 500L, FIXED_INSTANT.minusSeconds(3600), FIXED_INSTANT, FIXED_INSTANT, FIXED_INSTANT);
    }
}










