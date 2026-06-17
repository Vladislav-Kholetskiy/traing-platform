package com.vladislav.training.platform.userorg.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.service.UserAdministrationCard;
import com.vladislav.training.platform.userorg.service.UserAdministrationCommandService;
import com.vladislav.training.platform.userorg.service.UserAdministrationOrganizationAssignmentView;
import com.vladislav.training.platform.userorg.service.UserAdministrationQueryService;
import com.vladislav.training.platform.userorg.service.UserAdministrationRoleAssignmentView;
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
 * Проверяет поведение контроллера {@code UserAdministration}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAdministrationControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private UserAdministrationCommandService userAdministrationCommandService;
    @Mock
    private UserAdministrationQueryService userAdministrationQueryService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserAdministrationController(
                        userAdministrationCommandService,
                        userAdministrationQueryService,
                        utcClock
                ))
                .setControllerAdvice(new GlobalExceptionHandler(utcClock))
                .build();
    }

    @Test
    void getUsersReturnsQuerySnapshot() throws Exception {
        when(userAdministrationQueryService.listUsers(null)).thenReturn(List.of(user(1L, "EMP-1", UserStatus.ACTIVE)));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].employeeNumber").value("EMP-1"));
    }

    @Test
    void getUserCardReturnsActiveHistoryState() throws Exception {
        when(userAdministrationQueryService.getUserCard(1L)).thenReturn(new UserAdministrationCard(
                user(1L, "EMP-1", UserStatus.ACTIVE),
                List.of(roleAssignmentView(10L, 1L, 100L, "OPERATOR", "Operator")),
                List.of(orgAssignmentView(20L, 1L, 30L, "Root", "/root", OrganizationAssignmentType.PRIMARY))
        ));

        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.activeRoleAssignments[0].roleCode").value("OPERATOR"))
                .andExpect(jsonPath("$.activeOrganizationAssignments[0].organizationalUnitPath").value("/root"));
    }

    @Test
    void postCreateUserReturnsCreatedUser() throws Exception {
        when(userAdministrationCommandService.createUser(any(AppUser.class)))
                .thenReturn(user(1L, "EMP-1", UserStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "employeeNumber": "EMP-1",
                      "externalId": "EXT-1",
                      "lastName": "Last",
                      "firstName": "First",
                      "middleName": "Middle",
                      "status": "ACTIVE"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void postCreateUserRejectsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "employeeNumber": " ",
                      "lastName": "Last",
                      "firstName": "First",
                      "status": "ACTIVE"
                    }
                    """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userAdministrationCommandService);
    }

    @Test
    void patchUpdateUserReturnsUpdatedUserWithoutIdentityFieldsInRequest() throws Exception {
        when(userAdministrationCommandService.updateUser(1L, "Updated", "User", "Middle"))
                .thenReturn(new AppUser(1L, "EMP-1", "EXT-1", "Updated", "User", "Middle", UserStatus.ACTIVE, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(patch("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "lastName": "Updated",
                      "firstName": "User",
                      "middleName": "Middle"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value("EMP-1"))
                .andExpect(jsonPath("$.externalId").value("EXT-1"))
                .andExpect(jsonPath("$.lastName").value("Updated"))
                .andExpect(jsonPath("$.firstName").value("User"));
    }

    @Test
    void patchUpdateUserMapsConflictTo409() throws Exception {
        when(userAdministrationCommandService.updateUser(any(), any(), any(), any()))
                .thenThrow(new ConflictException("update conflict"));

        mockMvc.perform(patch("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "lastName": "Last",
                      "firstName": "First"
                    }
                    """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("update conflict"));
    }

    @Test
    void patchUpdateUserRejectsLegacyIdentityFieldsAtBoundary() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "employeeNumber": "EMP-5-NEW",
                      "externalId": "EXT-5",
                      "lastName": "Updated",
                      "firstName": "User"
                    }
                    """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userAdministrationCommandService);
    }

    @Test
    void postDeactivateMapsPolicyDenialTo403() throws Exception {
        when(userAdministrationCommandService.deactivateUser(1L)).thenThrow(new PolicyViolationException("denied"));

        mockMvc.perform(post("/api/v1/admin/users/1/deactivate"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("denied"));
    }

    @Test
    void getUserRolesMapsNotFoundTo404() throws Exception {
        when(userAdministrationQueryService.getRoleHistory(1L)).thenThrow(new NotFoundException("user not found"));

        mockMvc.perform(get("/api/v1/admin/users/1/roles"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("user not found"));
    }

    private AppUser user(Long id, String employeeNumber, UserStatus status) {
        return new AppUser(id, employeeNumber, null, "Last", "First", null, status, FIXED_INSTANT, FIXED_INSTANT);
    }

    private UserAdministrationRoleAssignmentView roleAssignmentView(
            Long id,
            Long userId,
            Long roleId,
            String roleCode,
            String roleName
    ) {
        return new UserAdministrationRoleAssignmentView(
                id,
                userId,
                roleId,
                roleCode,
                roleName,
                FIXED_INSTANT,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
    }

    private UserAdministrationOrganizationAssignmentView orgAssignmentView(
            Long id,
            Long userId,
            Long unitId,
            String organizationalUnitName,
            String organizationalUnitPath,
            OrganizationAssignmentType type
    ) {
        return new UserAdministrationOrganizationAssignmentView(
                id,
                userId,
                unitId,
                organizationalUnitName,
                organizationalUnitPath,
                type,
                FIXED_INSTANT,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
    }
}