package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.domain.AuditPayload;
import com.vladislav.training.platform.audit.service.AuditEventFactory;
import com.vladislav.training.platform.audit.service.AuditService;
import com.vladislav.training.platform.common.context.RequestContextHolder;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitTypeRepository;
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
 * Проверяет поведение {@code OrganizationCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationCommandServiceImplTest {

    private InteractiveActorResolver interactiveActorResolver;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private OrganizationalUnitRepository organizationalUnitRepository;
    @Mock
    private OrganizationalUnitTypeRepository organizationalUnitTypeRepository;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private AuditService auditService;
    @Mock
    private AuditEventFactory auditEventFactory;
    @Mock
    private UtcClock utcClock;
    @Mock
    private OrganizationalUnitSemanticMutationValidationSupport organizationalUnitSemanticMutationValidationSupport;
    @Mock
    private OrganizationalUnitStructuralMutationValidationSupport organizationalUnitStructuralMutationValidationSupport;

    private OrganizationCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        interactiveActorResolver = new InteractiveActorResolver(new AuthenticatedActorAdapter());
        service = new OrganizationCommandServiceImpl(
                organizationalUnitRepository,
                organizationalUnitTypeRepository,
                capabilityAdmissionPolicy,
                new CapabilityAdmissionRequestFactory(interactiveActorResolver, utcClock),
                new CriticalCommandAuditSupport(
                        interactiveActorResolver,
                        auditEventFactory,
                        auditService,
                        utcClock,
                        testObjectMapper()
                ),
                utcClock,
                new OrganizationalUnitTreePathBuilder(),
                new OrganizationalUnitSubtreeRebuilder(new OrganizationalUnitTreePathBuilder()),
                new OrganizationalUnitMoveValidator(),
                organizationalUnitSemanticMutationValidationSupport,
                organizationalUnitStructuralMutationValidationSupport
        );

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(auditService.recordAuditEvent(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> new AuditEvent(
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
        )).when(auditEventFactory).createAuditEvent(
                any(AuditEventType.class),
                anyString(),
                anyString(),
                anyLong(),
                any(Instant.class),
                nullable(AuditPayload.class),
                nullable(AuditPayload.class),
                nullable(AuditContext.class),
                nullable(String.class),
                nullable(String.class),
                any(Instant.class)
        );

        authenticateAs(101L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.clear();
    }

    @Test
    void createUnitTypeSuccessPerformsAdmissionBeforeOwnerStateBranchingAndWritesAudit() {
        OrganizationalUnitType command = unitType(null, "DIVISION", "Division");
        when(organizationalUnitTypeRepository.existsOrganizationalUnitTypeByCode("DIVISION")).thenReturn(false);
        when(organizationalUnitTypeRepository.saveOrganizationalUnitType(any(OrganizationalUnitType.class)))
                .thenAnswer(invocation -> {
                    OrganizationalUnitType toSave = invocation.getArgument(0, OrganizationalUnitType.class);
                    return new OrganizationalUnitType(
                            10L,
                            toSave.code(),
                            toSave.name(),
                            toSave.description(),
                            toSave.nodeKind(),
                            toSave.canBeOperatorHomeUnit(),
                            toSave.canBeCampaignTarget(),
                            toSave.participatesInSubtreeScope(),
                            toSave.canHaveManagementRelation(),
                            toSave.canHaveAccessArea(),
                            toSave.createdAt(),
                            toSave.updatedAt()
                    );
                });

        OrganizationalUnitType savedUnitType = service.createOrganizationalUnitType(command);

        assertThat(savedUnitType.id()).isEqualTo(10L);
        InOrder inOrder = inOrder(capabilityAdmissionPolicy, organizationalUnitTypeRepository);
        inOrder.verify(capabilityAdmissionPolicy).check(any());
        inOrder.verify(organizationalUnitTypeRepository).existsOrganizationalUnitTypeByCode("DIVISION");
        inOrder.verify(organizationalUnitTypeRepository).saveOrganizationalUnitType(any(OrganizationalUnitType.class));
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void createUnitTypeRejectsDuplicateCodeAfterAdmission() {
        when(organizationalUnitTypeRepository.existsOrganizationalUnitTypeByCode("DIVISION")).thenReturn(true);

        assertThatThrownBy(() -> service.createOrganizationalUnitType(unitType(null, "DIVISION", "Division")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("code already exists");

        InOrder inOrder = inOrder(capabilityAdmissionPolicy, organizationalUnitTypeRepository);
        inOrder.verify(capabilityAdmissionPolicy).check(any());
        inOrder.verify(organizationalUnitTypeRepository).existsOrganizationalUnitTypeByCode("DIVISION");
        verifyNoInteractions(auditService);
    }

    @Test
    void updateUnitTypeUpdatesDescriptiveFieldsAndRunsOwnerSemanticValidation() {
        OrganizationalUnitType currentUnitType = unitType(10L, "DIVISION", "Division");
        when(organizationalUnitTypeRepository.findOrganizationalUnitTypeById(10L)).thenReturn(currentUnitType);
        when(organizationalUnitTypeRepository.saveOrganizationalUnitType(any(OrganizationalUnitType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrganizationalUnitType.class));

        OrganizationalUnitType updatedUnitType = service.updateOrganizationalUnitType(
                new UpdateOrganizationalUnitTypeCommand(10L, " Division Updated ", "  New desc  ", null, null, null, null, null, null)
        );

        assertThat(updatedUnitType.code()).isEqualTo("DIVISION");
        assertThat(updatedUnitType.name()).isEqualTo("Division Updated");
        assertThat(updatedUnitType.description()).isEqualTo("New desc");
        verify(organizationalUnitSemanticMutationValidationSupport)
                .ensureUnitTypeMutationAllowed(currentUnitType, updatedUnitType, FIXED_INSTANT);
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void updateUnitTypeSupportsLiveCapabilityMutationAfterOwnerValidation() {
        OrganizationalUnitType currentUnitType = unitType(10L, "DIVISION", "Division");
        when(organizationalUnitTypeRepository.findOrganizationalUnitTypeById(10L)).thenReturn(currentUnitType);
        when(organizationalUnitTypeRepository.saveOrganizationalUnitType(any(OrganizationalUnitType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrganizationalUnitType.class));

        OrganizationalUnitType updatedUnitType = service.updateOrganizationalUnitType(
                new UpdateOrganizationalUnitTypeCommand(
                        10L,
                        "Division",
                        "Rebalanced",
                        OrganizationalNodeKind.FUNCTIONAL,
                        false,
                        false,
                        false,
                        false,
                        true
                )
        );

        assertThat(updatedUnitType.nodeKind()).isEqualTo(OrganizationalNodeKind.FUNCTIONAL);
        assertThat(updatedUnitType.canBeOperatorHomeUnit()).isFalse();
        verify(organizationalUnitSemanticMutationValidationSupport)
                .ensureUnitTypeMutationAllowed(eq(currentUnitType), eq(updatedUnitType), eq(FIXED_INSTANT));
    }

    @Test
    void createUnitWithValidParentAndTypeBuildsCanonicalPlacement() {
        OrganizationalUnit parentUnit = unit(1L, null, 10L, "Root", OrganizationalUnitStatus.ACTIVE, "/root", 0, null);
        OrganizationalUnitType unitType = unitType(10L, "DIVISION", "Division");
        OrganizationalUnit createCommand = unit(null, 1L, 10L, "Child Team", OrganizationalUnitStatus.ACTIVE, "/pending", 0, " ext-42 ");

        when(organizationalUnitRepository.existsOrganizationalUnitByExternalId("ext-42")).thenReturn(false);
        when(organizationalUnitRepository.findOrganizationalUnitById(1L)).thenReturn(parentUnit);
        when(organizationalUnitTypeRepository.findOrganizationalUnitTypeById(10L)).thenReturn(unitType);
        when(organizationalUnitRepository.existsOrganizationalUnitByPath("/root/child-team")).thenReturn(false);
        when(organizationalUnitRepository.saveOrganizationalUnit(any(OrganizationalUnit.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0, OrganizationalUnit.class), 20L));

        OrganizationalUnit savedUnit = service.createOrganizationalUnit(createCommand);

        assertThat(savedUnit.id()).isEqualTo(20L);
        assertThat(savedUnit.path()).isEqualTo("/root/child-team");
        assertThat(savedUnit.depth()).isEqualTo(1);
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void updateUnitUpdatesFieldsWithoutPathRebuildWhenNameNormalizationKeepsCanonicalPath() {
        OrganizationalUnit currentUnit = unit(20L, null, 10L, "Current", OrganizationalUnitStatus.ACTIVE, "/current", 0, null);
        OrganizationalUnitType currentUnitType = unitType(10L, "DIVISION", "Division");
        when(organizationalUnitRepository.findOrganizationalUnitById(20L)).thenReturn(currentUnit);
        when(organizationalUnitTypeRepository.findOrganizationalUnitTypeById(10L)).thenReturn(currentUnitType);
        when(organizationalUnitRepository.existsOrganizationalUnitByExternalIdAndIdNot("ext-1", 20L)).thenReturn(false);
        when(organizationalUnitRepository.existsOrganizationalUnitByPathAndIdNot("/current", 20L)).thenReturn(false);
        when(organizationalUnitRepository.saveOrganizationalUnit(any(OrganizationalUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrganizationalUnit.class));

        OrganizationalUnit updatedUnit = service.updateOrganizationalUnit(
                new UpdateOrganizationalUnitCommand(20L, " Current ", " ext-1 ", null)
        );

        assertThat(updatedUnit.path()).isEqualTo(currentUnit.path());
        assertThat(updatedUnit.externalId()).isEqualTo("ext-1");
        verify(organizationalUnitSemanticMutationValidationSupport)
                .ensureUnitTypeReassignmentAllowed(currentUnit, currentUnitType, FIXED_INSTANT);
        verify(organizationalUnitRepository, never()).findChildUnits(anyLong());
        verify(auditService).recordAuditEvent(any(AuditEvent.class));
    }

    @Test
    void updateUnitSupportsLiveTypeReassignmentAfterOwnerValidation() {
        OrganizationalUnit currentUnit = unit(20L, null, 10L, "Current", OrganizationalUnitStatus.ACTIVE, "/current", 0, null);
        OrganizationalUnitType newUnitType = new OrganizationalUnitType(
                11L,
                "FUNC",
                "Functional",
                null,
                OrganizationalNodeKind.FUNCTIONAL,
                false,
                true,
                true,
                true,
                true,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        when(organizationalUnitRepository.findOrganizationalUnitById(20L)).thenReturn(currentUnit);
        when(organizationalUnitTypeRepository.findOrganizationalUnitTypeById(11L)).thenReturn(newUnitType);
        when(organizationalUnitRepository.existsOrganizationalUnitByExternalIdAndIdNot("ext-2", 20L)).thenReturn(false);
        when(organizationalUnitRepository.existsOrganizationalUnitByPathAndIdNot("/current", 20L)).thenReturn(false);
        when(organizationalUnitRepository.saveOrganizationalUnit(any(OrganizationalUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrganizationalUnit.class));

        OrganizationalUnit updatedUnit = service.updateOrganizationalUnit(
                new UpdateOrganizationalUnitCommand(20L, "Current", "ext-2", 11L)
        );

        assertThat(updatedUnit.organizationalUnitTypeId()).isEqualTo(11L);
        verify(organizationalUnitSemanticMutationValidationSupport)
                .ensureUnitTypeReassignmentAllowed(currentUnit, newUnitType, FIXED_INSTANT);
    }

    @Test
    void updateUnitRenamesSubtreeAndRebuildsDerivedPathAndDepth() {
        OrganizationalUnit parentUnit = unit(1L, null, 10L, "Root", OrganizationalUnitStatus.ACTIVE, "/root", 0, null);
        OrganizationalUnit currentUnit = unit(20L, 1L, 10L, "Current", OrganizationalUnitStatus.ACTIVE, "/root/current", 1, null);
        OrganizationalUnit childUnit = unit(21L, 20L, 10L, "Leaf", OrganizationalUnitStatus.ACTIVE, "/root/current/leaf", 2, null);
        OrganizationalUnit grandChildUnit = unit(22L, 21L, 10L, "Deep", OrganizationalUnitStatus.ACTIVE, "/root/current/leaf/deep", 3, null);
        OrganizationalUnitType currentUnitType = unitType(10L, "DIVISION", "Division");

        when(organizationalUnitRepository.findOrganizationalUnitById(20L)).thenReturn(currentUnit);
        when(organizationalUnitRepository.findOrganizationalUnitById(1L)).thenReturn(parentUnit);
        when(organizationalUnitTypeRepository.findOrganizationalUnitTypeById(10L)).thenReturn(currentUnitType);
        when(organizationalUnitRepository.existsOrganizationalUnitByExternalIdAndIdNot("ext-1", 20L)).thenReturn(false);
        when(organizationalUnitRepository.findChildUnits(20L)).thenReturn(List.of(childUnit));
        when(organizationalUnitRepository.findChildUnits(21L)).thenReturn(List.of(grandChildUnit));
        when(organizationalUnitRepository.findChildUnits(22L)).thenReturn(List.of());
        when(organizationalUnitRepository.existsOrganizationalUnitByPathAndIdNot("/root/renamed-branch", 20L)).thenReturn(false);
        when(organizationalUnitRepository.existsOrganizationalUnitByPathAndIdNot("/root/renamed-branch/leaf", 21L)).thenReturn(false);
        when(organizationalUnitRepository.existsOrganizationalUnitByPathAndIdNot("/root/renamed-branch/leaf/deep", 22L)).thenReturn(false);
        when(organizationalUnitRepository.saveOrganizationalUnit(any(OrganizationalUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrganizationalUnit.class));

        OrganizationalUnit updatedUnit = service.updateOrganizationalUnit(
                new UpdateOrganizationalUnitCommand(20L, "Renamed Branch", "ext-1", null)
        );

        assertThat(updatedUnit.path()).isEqualTo("/root/renamed-branch");
        ArgumentCaptor<OrganizationalUnit> savedUnitsCaptor = ArgumentCaptor.forClass(OrganizationalUnit.class);
        verify(organizationalUnitRepository, times(3)).saveOrganizationalUnit(savedUnitsCaptor.capture());
        assertThat(savedUnitsCaptor.getAllValues())
                .extracting(OrganizationalUnit::path)
                .containsExactly(
                        "/root/renamed-branch",
                        "/root/renamed-branch/leaf",
                        "/root/renamed-branch/leaf/deep"
                );
    }

    @Test
    void moveUnitRejectsSelfParentAfterAdmission() {
        when(organizationalUnitRepository.findOrganizationalUnitById(2L))
                .thenReturn(unit(2L, 1L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/root/branch", 1, null));

        assertThatThrownBy(() -> service.moveOrganizationalUnit(2L, 2L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must not reference the same unit");

        InOrder inOrder = inOrder(capabilityAdmissionPolicy, organizationalUnitRepository);
        inOrder.verify(capabilityAdmissionPolicy).check(any());
        inOrder.verify(organizationalUnitRepository).findOrganizationalUnitById(2L);
        verifyNoInteractions(auditService, organizationalUnitStructuralMutationValidationSupport);
    }

    @Test
    void moveUnitRejectsCycleBeforeStructuralMutationValidation() {
        OrganizationalUnit branch = unit(2L, 1L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/root/branch", 1, null);
        OrganizationalUnit leaf = unit(3L, 2L, 10L, "Leaf", OrganizationalUnitStatus.ACTIVE, "/root/branch/leaf", 2, null);

        when(organizationalUnitRepository.findOrganizationalUnitById(2L)).thenReturn(branch);
        when(organizationalUnitRepository.findOrganizationalUnitById(3L)).thenReturn(leaf);
        when(organizationalUnitRepository.findChildUnits(2L)).thenReturn(List.of(leaf));
        when(organizationalUnitRepository.findChildUnits(3L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.moveOrganizationalUnit(2L, 3L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("create a cycle");

        verify(organizationalUnitRepository, never()).saveOrganizationalUnit(any());
        verifyNoInteractions(auditService, organizationalUnitStructuralMutationValidationSupport);
    }

    @Test
    void moveUnitRebuildsSubtreeAndWritesAudit() {
        OrganizationalUnit currentUnit = unit(2L, 1L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/root/branch", 1, null);
        OrganizationalUnit leaf = unit(3L, 2L, 10L, "Leaf", OrganizationalUnitStatus.ACTIVE, "/root/branch/leaf", 2, null);
        OrganizationalUnit newParent = unit(4L, null, 10L, "Other", OrganizationalUnitStatus.ACTIVE, "/other", 0, null);

        when(organizationalUnitRepository.findOrganizationalUnitById(2L)).thenReturn(currentUnit);
        when(organizationalUnitRepository.findOrganizationalUnitById(4L)).thenReturn(newParent);
        when(organizationalUnitRepository.findChildUnits(2L)).thenReturn(List.of(leaf));
        when(organizationalUnitRepository.findChildUnits(3L)).thenReturn(List.of());
        when(organizationalUnitRepository.existsOrganizationalUnitByPathAndIdNot("/other/branch", 2L)).thenReturn(false);
        when(organizationalUnitRepository.existsOrganizationalUnitByPathAndIdNot("/other/branch/leaf", 3L)).thenReturn(false);
        when(organizationalUnitRepository.saveOrganizationalUnit(any(OrganizationalUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrganizationalUnit.class));

        OrganizationalUnit movedUnit = service.moveOrganizationalUnit(2L, 4L);

        assertThat(movedUnit.parentId()).isEqualTo(4L);
        assertThat(movedUnit.path()).isEqualTo("/other/branch");
        assertThat(movedUnit.depth()).isEqualTo(1);
        ArgumentCaptor<CapabilityAdmissionRequest> admissionCaptor = ArgumentCaptor.forClass(CapabilityAdmissionRequest.class);
        verify(capabilityAdmissionPolicy).check(admissionCaptor.capture());
        assertThat(admissionCaptor.getValue().payloadContext())
                .isInstanceOf(CapabilityAdmissionPayload.OrganizationalUnitMutation.class);
        CapabilityAdmissionPayload.OrganizationalUnitMutation payload =
                (CapabilityAdmissionPayload.OrganizationalUnitMutation) admissionCaptor.getValue().payloadContext();
        assertThat(payload.newParentUnitId()).isEqualTo(4L);
        verify(organizationalUnitStructuralMutationValidationSupport).ensureMoveAllowed(anyList(), eq(newParent));
        ArgumentCaptor<OrganizationalUnit> savedUnitsCaptor = ArgumentCaptor.forClass(OrganizationalUnit.class);
        verify(organizationalUnitRepository, times(2)).saveOrganizationalUnit(savedUnitsCaptor.capture());
        assertThat(savedUnitsCaptor.getAllValues())
                .extracting(OrganizationalUnit::path)
                .containsExactly("/other/branch", "/other/branch/leaf");

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).recordAuditEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().eventType().value()).isEqualTo("userorg.organizational_unit.moved");
    }

    @Test
    void archiveUnitRejectsAlreadyArchivedSubtree() {
        OrganizationalUnit archivedRoot = unit(1L, null, 10L, "Root", OrganizationalUnitStatus.ARCHIVED, "/root", 0, null);
        OrganizationalUnit archivedChild = unit(2L, 1L, 10L, "Child", OrganizationalUnitStatus.ARCHIVED, "/root/child", 1, null);

        when(organizationalUnitRepository.findOrganizationalUnitById(1L)).thenReturn(archivedRoot);
        when(organizationalUnitRepository.findChildUnits(1L)).thenReturn(List.of(archivedChild));
        when(organizationalUnitRepository.findChildUnits(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.archiveOrganizationalUnit(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already archived");

        verifyNoInteractions(auditService, organizationalUnitStructuralMutationValidationSupport);
    }

    @Test
    void archiveUnitFailsClosedWhenStructuralValidationRejectsActiveDownstreamState() {
        OrganizationalUnit root = unit(1L, null, 10L, "Root", OrganizationalUnitStatus.ACTIVE, "/root", 0, null);
        OrganizationalUnit child = unit(2L, 1L, 10L, "Child", OrganizationalUnitStatus.ACTIVE, "/root/child", 1, null);

        when(organizationalUnitRepository.findOrganizationalUnitById(1L)).thenReturn(root);
        when(organizationalUnitRepository.findChildUnits(1L)).thenReturn(List.of(child));
        when(organizationalUnitRepository.findChildUnits(2L)).thenReturn(List.of());
        doThrow(new ConflictException("active user_access_area exists"))
                .when(organizationalUnitStructuralMutationValidationSupport)
                .ensureArchiveAllowed(anyList(), eq(FIXED_INSTANT));

        assertThatThrownBy(() -> service.archiveOrganizationalUnit(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("user_access_area");

        verify(organizationalUnitRepository, never()).saveOrganizationalUnit(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void archiveUnitArchivesActiveSubtreeAndWritesAudit() {
        OrganizationalUnit root = unit(1L, null, 10L, "Root", OrganizationalUnitStatus.ACTIVE, "/root", 0, null);
        OrganizationalUnit child = unit(2L, 1L, 10L, "Child", OrganizationalUnitStatus.ACTIVE, "/root/child", 1, null);

        when(organizationalUnitRepository.findOrganizationalUnitById(1L)).thenReturn(root);
        when(organizationalUnitRepository.findChildUnits(1L)).thenReturn(List.of(child));
        when(organizationalUnitRepository.findChildUnits(2L)).thenReturn(List.of());
        when(organizationalUnitRepository.saveOrganizationalUnit(any(OrganizationalUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrganizationalUnit.class));

        OrganizationalUnit archivedRoot = service.archiveOrganizationalUnit(1L);

        assertThat(archivedRoot.status()).isEqualTo(OrganizationalUnitStatus.ARCHIVED);
        ArgumentCaptor<OrganizationalUnit> savedUnitsCaptor = ArgumentCaptor.forClass(OrganizationalUnit.class);
        verify(organizationalUnitRepository, times(2)).saveOrganizationalUnit(savedUnitsCaptor.capture());
        assertThat(savedUnitsCaptor.getAllValues())
                .extracting(OrganizationalUnit::status)
                .containsOnly(OrganizationalUnitStatus.ARCHIVED);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).recordAuditEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().eventType().value()).isEqualTo("userorg.organizational_unit.archived");
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

    private OrganizationalUnitType unitType(Long id, String code, String name) {
        return new OrganizationalUnitType(
                id,
                code,
                name,
                null,
                OrganizationalNodeKind.LINEAR,
                true,
                true,
                true,
                true,
                true,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
    }

    private OrganizationalUnit unit(
            Long id,
            Long parentId,
            Long typeId,
            String name,
            OrganizationalUnitStatus status,
            String path,
            int depth,
            String externalId
    ) {
        return new OrganizationalUnit(id, parentId, typeId, name, status, path, depth, externalId, FIXED_INSTANT, FIXED_INSTANT);
    }

    private OrganizationalUnit withId(OrganizationalUnit source, Long id) {
        return new OrganizationalUnit(
                id,
                source.parentId(),
                source.organizationalUnitTypeId(),
                source.name(),
                source.status(),
                source.path(),
                source.depth(),
                source.externalId(),
                source.createdAt(),
                source.updatedAt()
        );
    }
    @Test
    void archiveUnitFailsClosedForMixedActiveAndArchivedSubtreeInsteadOfRepairingIt() {
        OrganizationalUnit archivedRoot = unit(1L, null, 10L, "Root", OrganizationalUnitStatus.ARCHIVED, "/root", 0, null);
        OrganizationalUnit activeChild = unit(2L, 1L, 10L, "Child", OrganizationalUnitStatus.ACTIVE, "/root/child", 1, null);

        when(organizationalUnitRepository.findOrganizationalUnitById(1L)).thenReturn(archivedRoot);
        when(organizationalUnitRepository.findChildUnits(1L)).thenReturn(List.of(activeChild));
        when(organizationalUnitRepository.findChildUnits(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.archiveOrganizationalUnit(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("mixed ACTIVE/ARCHIVED state");

        verify(organizationalUnitRepository, never()).saveOrganizationalUnit(any());
        verifyNoInteractions(auditService, organizationalUnitStructuralMutationValidationSupport);
    }
}





