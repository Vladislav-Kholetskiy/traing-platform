package com.vladislav.training.platform.application.actor;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.application.actor.dev.DevFrontendBootstrapSecurityConfiguration;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.common.web.RequestContextFilter;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppRoleRepository;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CurrentActorController.class)
@ActiveProfiles("dev")
@Import({
    CurrentActorReadService.class,
    InteractiveActorResolver.class,
    AuthenticatedActorAdapter.class,
    GlobalExceptionHandler.class,
    RequestContextFilter.class,
    DevFrontendBootstrapSecurityConfiguration.class,
    CurrentActorControllerWebMvcTestConfig.class
})
/**
 * Проверяет поведение {@code CurrentActorControllerDevProfileWebMvc}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class CurrentActorControllerDevProfileWebMvcTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Autowired
    private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    @Autowired
    private OrganizationalUnitRepository organizationalUnitRepository;
    @Autowired
    private AppRoleRepository appRoleRepository;
    @Autowired
    private TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService;
    @Autowired
    private UtcClock utcClock;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(appUserRepository.findUserById(101L)).thenReturn(
            new AppUser(101L, "operator", "EXT-101", "Ivanov", "Ivan", "Ivanovich", UserStatus.ACTIVE, FIXED_INSTANT, FIXED_INSTANT)
        );
        when(userRoleAssignmentRepository.findActiveRoleAssignmentsByUserId(101L, FIXED_INSTANT))
            .thenReturn(List.of(new UserRoleAssignment(501L, 101L, 11L, FIXED_INSTANT.minusSeconds(60), null, FIXED_INSTANT, FIXED_INSTANT)));
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(101L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(appRoleRepository.findRoleById(11L))
            .thenReturn(new AppRole(11L, "OPERATOR", "Operator", null, FIXED_INSTANT, FIXED_INSTANT));
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(101L, FIXED_INSTANT))
            .thenReturn(List.of());
    }

    @Test
    void currentActorEndpointReturns200ForDemoActor() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("X-Demo-Actor-Id", "101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actorUserId").value(101))
            .andExpect(jsonPath("$.username").value("operator"))
            .andExpect(jsonPath("$.displayName").value("Ivanov Ivan Ivanovich"))
            .andExpect(jsonPath("$.roles[0]").value("OPERATOR"))
            .andExpect(jsonPath("$.enabledSections[0]").value("ASSIGNED_LEARNING"))
            .andExpect(jsonPath("$.enabledSections[1]").value("SELF_TESTING"))
            .andExpect(jsonPath("$.enabledSections[2]").value("SELF_RESULTS"));
    }

    @Test
    void currentActorEndpointNormalizesPersonnelRoleAliasesForEnabledSections() throws Exception {
        when(appRoleRepository.findRoleById(11L))
            .thenReturn(new AppRole(11L, "ROLE_OPERATIONS", "Role Operations", null, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(get("/api/v1/me").header("X-Demo-Actor-Id", "101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles[0]").value("OPERATOR"))
            .andExpect(jsonPath("$.enabledSections[0]").value("ASSIGNED_LEARNING"))
            .andExpect(jsonPath("$.enabledSections[1]").value("SELF_TESTING"))
            .andExpect(jsonPath("$.enabledSections[2]").value("SELF_RESULTS"));
    }

    @Test
    void currentActorEndpointDoesNotAcceptActorOverrideFromRequestBody() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                .header("X-Demo-Actor-Id", "101")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actorUserId\":999}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actorUserId").value(101))
            .andExpect(jsonPath("$.username").value("operator"));
    }

    @Test
    void currentActorEndpointFailsPredictablyWithoutAuthenticatedOrDemoActor() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Authenticated principal is required for interactive actor resolution"));
    }

    @Test
    void devCorsPreflightForViteOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Content-Type,X-Demo-Actor-Id"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")))
            .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("X-Demo-Actor-Id")));
    }
}
