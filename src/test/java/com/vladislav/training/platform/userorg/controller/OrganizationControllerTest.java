package com.vladislav.training.platform.userorg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.userorg.controller.dto.CreateOrganizationalUnitRequest;
import com.vladislav.training.platform.userorg.controller.dto.MoveOrganizationalUnitRequest;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.service.OrganizationCommandService;
import com.vladislav.training.platform.userorg.service.OrganizationPolicyReadFacade;
import com.vladislav.training.platform.userorg.service.UpdateOrganizationalUnitCommand;
import com.vladislav.training.platform.userorg.service.UpdateOrganizationalUnitTypeCommand;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code Organization}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private OrganizationPolicyReadFacade organizationPolicyReadFacade;
    @Mock
    private OrganizationCommandService organizationCommandService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = testObjectMapper();
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        OrganizationController controller = new OrganizationController(
                organizationPolicyReadFacade,
            organizationCommandService,
            utcClock
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    void getOrganizationalUnitTypesReturnsReadSnapshot() throws Exception {
        when(organizationPolicyReadFacade.findUnitTypesByNodeKind(OrganizationalNodeKind.LINEAR))
            .thenReturn(List.of(unitType(10L, "DIVISION", "Division")));

        mockMvc.perform(get("/api/v1/admin/org-unit-types").param("nodeKind", "LINEAR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].code").value("DIVISION"));
    }

    @Test
    void getOrganizationalUnitTreeReturnsHierarchicalResponse() throws Exception {
        OrganizationalUnit root = unit(1L, null, 10L, "Root", OrganizationalUnitStatus.ACTIVE, "/root", 0);
        OrganizationalUnit child = unit(2L, 1L, 10L, "Child", OrganizationalUnitStatus.ACTIVE, "/root/child", 1);
        when(organizationPolicyReadFacade.findOrganizationalUnitTree(null)).thenReturn(List.of(root, child));

        mockMvc.perform(get("/api/v1/admin/org-units/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].children[0].id").value(2))
            .andExpect(jsonPath("$[0].children[0].path").value("/root/child"));
    }

    @Test
    void getOrganizationalUnitByIdReturnsScopedSnapshot() throws Exception {
        when(organizationPolicyReadFacade.findOrganizationalUnitById(20L))
            .thenReturn(unit(20L, 1L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/root/branch", 1));

        mockMvc.perform(get("/api/v1/admin/org-units/20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(20))
            .andExpect(jsonPath("$.path").value("/root/branch"));
    }

    @Test
    void postOrganizationalUnitTypeRejectsMissingCapabilityFlag() throws Exception {
        mockMvc.perform(post("/api/v1/admin/org-unit-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "DIVISION",
                      "name": "Division",
                      "nodeKind": "LINEAR",
                      "canBeCampaignTarget": true,
                      "participatesInSubtreeScope": true,
                      "canHaveManagementRelation": true,
                      "canHaveAccessArea": true
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationCommandService);
    }

    @Test
    void postOrganizationalUnitReturnsCreatedSnapshot() throws Exception {
        when(organizationCommandService.createOrganizationalUnit(any(OrganizationalUnit.class)))
            .thenReturn(unit(20L, 1L, 10L, "Child Team", OrganizationalUnitStatus.ACTIVE, "/root/child-team", 1));

        CreateOrganizationalUnitRequest request = new CreateOrganizationalUnitRequest(1L, 10L, "Child Team", "ext-1");

        mockMvc.perform(post("/api/v1/admin/org-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(20))
            .andExpect(jsonPath("$.path").value("/root/child-team"));
    }

    @Test
    void patchOrganizationalUnitUsesDedicatedOwnerCommandWithoutPolicyAwareRead() throws Exception {
        when(organizationCommandService.updateOrganizationalUnit(any(UpdateOrganizationalUnitCommand.class)))
            .thenReturn(unit(20L, 1L, 11L, "Renamed", OrganizationalUnitStatus.ACTIVE, "/root/unit", 1));

        mockMvc.perform(patch("/api/v1/admin/org-units/20")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Renamed",
                      "externalId": "ext-1",
                      "organizationalUnitTypeId": 11
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(20))
            .andExpect(jsonPath("$.name").value("Renamed"));

        ArgumentCaptor<UpdateOrganizationalUnitCommand> commandCaptor = ArgumentCaptor.forClass(UpdateOrganizationalUnitCommand.class);
        verify(organizationCommandService).updateOrganizationalUnit(commandCaptor.capture());
        assertThat(commandCaptor.getValue().organizationalUnitTypeId()).isEqualTo(11L);
        verify(organizationCommandService, never()).moveOrganizationalUnit(any(), any());
        verify(organizationCommandService, never()).archiveOrganizationalUnit(anyLong());
        verifyNoInteractions(organizationPolicyReadFacade);
    }

    @Test
    void patchOrganizationalUnitTypeUsesDedicatedOwnerCommandWithoutQueryRead() throws Exception {
        when(organizationCommandService.updateOrganizationalUnitType(any(UpdateOrganizationalUnitTypeCommand.class)))
            .thenReturn(unitType(10L, "DIVISION", "Division Updated"));

        mockMvc.perform(patch("/api/v1/admin/org-unit-types/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Division Updated",
                      "description": "New desc",
                      "nodeKind": "FUNCTIONAL",
                      "canBeOperatorHomeUnit": false,
                      "canBeCampaignTarget": false,
                      "participatesInSubtreeScope": false,
                      "canHaveManagementRelation": false,
                      "canHaveAccessArea": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.name").value("Division Updated"));

        ArgumentCaptor<UpdateOrganizationalUnitTypeCommand> commandCaptor = ArgumentCaptor.forClass(UpdateOrganizationalUnitTypeCommand.class);
        verify(organizationCommandService).updateOrganizationalUnitType(commandCaptor.capture());
        assertThat(commandCaptor.getValue().nodeKind()).isEqualTo(OrganizationalNodeKind.FUNCTIONAL);
        assertThat(commandCaptor.getValue().canBeOperatorHomeUnit()).isFalse();
        verifyNoInteractions(organizationPolicyReadFacade);
    }

    @Test
    void postMoveReturnsMovedSnapshot() throws Exception {
        when(organizationCommandService.moveOrganizationalUnit(2L, 4L))
            .thenReturn(unit(2L, 4L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/other/branch", 1));

        mockMvc.perform(post("/api/v1/admin/org-units/2/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MoveOrganizationalUnitRequest(4L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parentId").value(4))
            .andExpect(jsonPath("$.path").value("/other/branch"));

        verify(organizationCommandService).moveOrganizationalUnit(2L, 4L);
    }

    @Test
    void postArchiveReturnsArchivedSnapshot() throws Exception {
        when(organizationCommandService.archiveOrganizationalUnit(2L))
            .thenReturn(unit(2L, 1L, 10L, "Branch", OrganizationalUnitStatus.ARCHIVED, "/root/branch", 1));

        mockMvc.perform(post("/api/v1/admin/org-units/2/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void postArchiveReturnsConflictWhenOwnerValidationRejectsCommand() throws Exception {
        when(organizationCommandService.archiveOrganizationalUnit(2L))
            .thenThrow(new ConflictException("active user_access_area exists"));

        mockMvc.perform(post("/api/v1/admin/org-units/2/archive"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("active user_access_area exists"));
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

    private OrganizationalUnit unit(
        Long id,
        Long parentId,
        Long typeId,
        String name,
        OrganizationalUnitStatus status,
        String path,
        int depth
    ) {
        return new OrganizationalUnit(id, parentId, typeId, name, status, path, depth, null, FIXED_INSTANT, FIXED_INSTANT);
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
}
