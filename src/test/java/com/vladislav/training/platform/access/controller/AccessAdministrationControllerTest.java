package com.vladislav.training.platform.access.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.AccessAdministrationQueryService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code AccessAdministration}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessAdministrationControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T10:00:00Z");

    @Mock
    private AccessAdministrationCommandService accessAdministrationCommandService;
    @Mock
    private AccessAdministrationQueryService accessAdministrationQueryService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(new AccessAdministrationController(
                accessAdministrationCommandService,
                accessAdministrationQueryService,
                utcClock
            ))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void getAccessAreasReturnsFilteredQuerySnapshot() throws Exception {
        when(accessAdministrationQueryService.listUserAccessAreas(any())).thenReturn(List.of(new UserAccessArea(
            10L,
            1L,
            30L,
            AccessScopeType.UNIT_ONLY,
            FIXED_INSTANT,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        )));

        mockMvc.perform(get("/api/v1/admin/access-areas").param("userId", "1").param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].accessScopeType").value("UNIT_ONLY"));
    }

    @Test
    void getTemporaryRoleAssignmentsReturnsFilteredQuerySnapshot() throws Exception {
        when(accessAdministrationQueryService.listTemporaryRoleAssignments(any())).thenReturn(List.of(
            new TemporaryRoleAssignment(20L, 1L, 900L, FIXED_INSTANT, null, FIXED_INSTANT, FIXED_INSTANT)
        ));

        mockMvc.perform(get("/api/v1/admin/temporary-role-assignments").param("roleId", "900").param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].roleId").value(900));
    }

    @Test
    void getTemporaryManagementDelegationsMapsPolicyDenialTo403() throws Exception {
        when(accessAdministrationQueryService.listTemporaryManagementDelegations(any()))
            .thenThrow(new PolicyViolationException("denied"));

        mockMvc.perform(get("/api/v1/admin/temporary-management-delegations"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("denied"));
    }

    @Test
    void getManagementRelationTypesReturnsDictionary() throws Exception {
        when(accessAdministrationQueryService.listManagementRelationTypes()).thenReturn(List.of(new ManagementRelationType(
            500L,
            "SUPERVISOR",
            "Supervisor",
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        )));

        mockMvc.perform(get("/api/v1/admin/management-relation-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("SUPERVISOR"));
    }

    @Test
    void postAssignUserAccessAreaReturnsCreatedRow() throws Exception {
        when(accessAdministrationCommandService.assignUserAccessArea(any(), any(), any(), any())).thenReturn(new UserAccessArea(
            10L,
            1L,
            null,
            AccessScopeType.GLOBAL,
            FIXED_INSTANT,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));

        mockMvc.perform(post("/api/v1/admin/access-areas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": 1,
                      "accessScopeType": "GLOBAL"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.accessScopeType").value("GLOBAL"));
    }

    @Test
    void postAssignTemporaryRoleReturnsCreatedRow() throws Exception {
        when(accessAdministrationCommandService.assignTemporaryRoleAssignment(any(), any(), any())).thenReturn(
            new TemporaryRoleAssignment(21L, 1L, 900L, FIXED_INSTANT, null, FIXED_INSTANT, FIXED_INSTANT)
        );

        mockMvc.perform(post("/api/v1/admin/temporary-role-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": 1,
                      "roleId": 900
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(21))
            .andExpect(jsonPath("$.roleId").value(900));
    }

    @Test
    void postAssignTemporaryAccessAreaRejectsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/temporary-access-areas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accessScopeType": "GLOBAL"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(accessAdministrationCommandService);
    }

    @Test
    void postAssignManagementRelationMapsConflictTo409() throws Exception {
        when(accessAdministrationCommandService.assignManagementRelation(any(), any(), any(), any()))
            .thenThrow(new ConflictException("overlap"));

        mockMvc.perform(post("/api/v1/admin/management-relations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": 1,
                      "organizationalUnitId": 30,
                      "managementRelationTypeId": 500
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("overlap"));
    }

    @Test
    void postAssignTemporaryManagementDelegationMapsConflictTo409() throws Exception {
        when(accessAdministrationCommandService.assignTemporaryManagementDelegation(any(), any(), any(), any()))
            .thenThrow(new ConflictException("overlap"));

        mockMvc.perform(post("/api/v1/admin/temporary-management-delegations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": 1,
                      "organizationalUnitId": 30,
                      "managementRelationTypeId": 500
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("overlap"));
    }

    @Test
    void postCloseManagementRelationMapsNotFoundTo404() throws Exception {
        when(accessAdministrationCommandService.closeManagementRelation(20L, FIXED_INSTANT))
            .thenThrow(new NotFoundException("not found"));

        mockMvc.perform(post("/api/v1/admin/management-relations/20/close"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("not found"));
    }

    @Test
    void postCloseTemporaryAccessAreaUsesDedicatedCloseCommandEndpoint() throws Exception {
        when(accessAdministrationCommandService.closeTemporaryAccessArea(10L, FIXED_INSTANT)).thenReturn(
            new TemporaryAccessArea(
                10L,
                1L,
                null,
                AccessScopeType.GLOBAL,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT,
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        );

        mockMvc.perform(post("/api/v1/admin/temporary-access-areas/10/close"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.validTo").value("2026-03-29T10:00:00Z"));
    }

    @Test
    void postCloseTemporaryManagementDelegationReturnsClosedRow() throws Exception {
        when(accessAdministrationCommandService.closeTemporaryManagementDelegation(30L, FIXED_INSTANT)).thenReturn(
            new TemporaryManagementDelegation(
                30L,
                1L,
                40L,
                500L,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT,
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        );

        mockMvc.perform(post("/api/v1/admin/temporary-management-delegations/30/close"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.validTo").value("2026-03-29T10:00:00Z"));
    }
}
