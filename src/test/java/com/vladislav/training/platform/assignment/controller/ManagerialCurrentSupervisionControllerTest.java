package com.vladislav.training.platform.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.controller.dto.ManagerialCurrentSupervisionResponse;
import com.vladislav.training.platform.assignment.repository.ManagerialCurrentSupervisionReadRepository;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.assignment.service.ManagerialCurrentSupervisionQueryService;
import com.vladislav.training.platform.assignment.service.ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery;
import com.vladislav.training.platform.assignment.service.ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
/**
 * Проверяет поведение контроллера {@code ManagerialCurrentSupervision}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class ManagerialCurrentSupervisionControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-28T09:15:00Z");

    @Mock
    private ManagerialCurrentSupervisionQueryService managerialCurrentSupervisionQueryService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ManagerialCurrentSupervisionController(
                    managerialCurrentSupervisionQueryService,
                    interactiveActorResolver,
                    utcClock
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void managerialCurrentSupervisionEndpointUsesTrustedActorAndClockAndReturnsRows() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(managerialCurrentSupervisionQueryService.findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
        )).thenReturn(List.of(
            new ManagerialCurrentSupervisionRow(
                77L,
                201L,
                "Ivanov Ivan Ivanovich",
                501L,
                "Labor Safety",
                3L,
                Instant.parse("2026-04-27T08:00:00Z"),
                Instant.parse("2026-04-30T18:00:00Z"),
                com.vladislav.training.platform.assignment.domain.AssignmentStatus.ASSIGNED
            ),
            new ManagerialCurrentSupervisionRow(
                78L,
                202L,
                "Petrov Petr Petrovich",
                502L,
                "Industrial Safety",
                1L,
                Instant.parse("2026-04-27T09:00:00Z"),
                Instant.parse("2026-05-01T18:00:00Z"),
                com.vladislav.training.platform.assignment.domain.AssignmentStatus.COMPLETED
            )
        ));

        mockMvc.perform(get("/api/v1/managerial/current-supervision"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].assignmentId").value(77))
            .andExpect(jsonPath("$[0].assignmentTestCount").value(3))
            .andExpect(jsonPath("$[0].userId").value(201))
            .andExpect(jsonPath("$[0].userDisplayName").value("Ivanov Ivan Ivanovich"))
            .andExpect(jsonPath("$[0].courseId").value(501))
            .andExpect(jsonPath("$[0].courseName").value("Labor Safety"))
            .andExpect(jsonPath("$[0].assignedAt").value("2026-04-27T08:00:00Z"))
            .andExpect(jsonPath("$[0].deadlineAt").value("2026-04-30T18:00:00Z"))
            .andExpect(jsonPath("$[0].assignmentStatus").value("ASSIGNED"))
            .andExpect(jsonPath("$[1].assignmentId").value(78))
            .andExpect(jsonPath("$[1].assignmentStatus").value("COMPLETED"))
            .andExpect(jsonPath("$[0].targetUserId").doesNotExist())
            .andExpect(jsonPath("$[0].managerUserId").doesNotExist());

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
        verify(managerialCurrentSupervisionQueryService).findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
        );
    }

    @Test
    void emptyServiceResponseIsReturnedAsEmptyJsonArray() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(managerialCurrentSupervisionQueryService.findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/managerial/current-supervision"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
        verify(managerialCurrentSupervisionQueryService).findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
        );
    }

    @Test
    void policyViolationFromServiceIsReturnedAsForbiddenWithoutFakeEmptyFallback() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(managerialCurrentSupervisionQueryService.findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
        )).thenThrow(new PolicyViolationException(
            "MANAGERIAL_CURRENT_SUPERVISION_DENIED",
            "Managerial current supervision denied"
        ));

        mockMvc.perform(get("/api/v1/managerial/current-supervision"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Managerial current supervision denied"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock, times(2)).now();
        verify(managerialCurrentSupervisionQueryService).findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
        );
    }

    @Test
    void requestSelectorsDoNotOverrideResolvedActorIdentity() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(managerialCurrentSupervisionQueryService.findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
        )).thenReturn(List.of(
            new ManagerialCurrentSupervisionRow(
                77L,
                201L,
                "Ivanov Ivan Ivanovich",
                501L,
                "Labor Safety",
                2L,
                Instant.parse("2026-04-27T08:00:00Z"),
                Instant.parse("2026-04-30T18:00:00Z"),
                com.vladislav.training.platform.assignment.domain.AssignmentStatus.ASSIGNED
            )
        ));

        mockMvc.perform(get("/api/v1/managerial/current-supervision")
                .queryParam("targetUserId", "999")
                .queryParam("actorUserId", "999")
                .queryParam("managerUserId", "999")
                .queryParam("scope", "FULL")
                .queryParam("scopeOverride", "FULL")
                .queryParam("organizationalUnitIds", "999,1000")
                .queryParam("subtreePaths", "/root/finance")
                .queryParam("organizationalUnitId", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].assignmentId").value(77));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
        verify(managerialCurrentSupervisionQueryService).findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
        );
        verify(managerialCurrentSupervisionQueryService, never()).findCurrentSupervision(
            new ManagerialCurrentSupervisionQuery(999L, FIXED_INSTANT)
        );
    }

    @Test
    void controllerReturnsPublicResponseDtoListInsteadOfJpaEntities() throws Exception {
        Method controllerMethod = ManagerialCurrentSupervisionController.class
            .getDeclaredMethod("findCurrentSupervision");

        assertThat(controllerMethod.getReturnType()).isEqualTo(List.class);
        assertThat(controllerMethod.getGenericReturnType()).isInstanceOf(ParameterizedType.class);

        Type responseType = ((ParameterizedType) controllerMethod.getGenericReturnType())
            .getActualTypeArguments()[0];

        assertThat(responseType).isEqualTo(ManagerialCurrentSupervisionResponse.class);
        assertThat(responseType.getTypeName())
            .doesNotContain("Entity")
            .doesNotContain("Jpa");
    }

    @Test
    void controllerMethodAcceptsNoClientSuppliedSelectorParameters() throws Exception {
        Method controllerMethod = ManagerialCurrentSupervisionController.class
            .getDeclaredMethod("findCurrentSupervision");

        assertThat(controllerMethod.getParameterCount()).isZero();
        assertThat(controllerMethod.isAnnotationPresent(RequestParam.class)).isFalse();
        assertThat(controllerMethod.isAnnotationPresent(PathVariable.class)).isFalse();
        assertThat(controllerMethod.isAnnotationPresent(RequestBody.class)).isFalse();
    }

    @Test
    void controllerDependsOnlyOnQueryServiceTrustedActorResolverAndClock() {
        assertThat(fieldTypes(ManagerialCurrentSupervisionController.class))
            .containsExactly(
                ManagerialCurrentSupervisionQueryService.class,
                InteractiveActorResolver.class,
                UtcClock.class
            )
            .doesNotContain(
                ManagerialCurrentSupervisionReadRepository.class,
                com.vladislav.training.platform.access.service.ManagerialReadScopeProjectionService.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContext.class,
                ResultRecordingService.class,
                AssignmentStatusRecalculationService.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class,
                CapabilityAdmissionPolicy.class,
                ActiveAttemptAnswerMutationService.class
            );
    }

    @Test
    void controllerSourceDoesNotDependOnRepositoryPolicyProjectionOrMutationImplementations() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/controller/ManagerialCurrentSupervisionController.java"
        ));

        assertThat(source)
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("ManagerialReadScopeProjectionService")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("AccessPolicyQueryContext")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("CapabilityAdmissionPolicy");
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
