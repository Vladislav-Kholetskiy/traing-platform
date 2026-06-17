package com.vladislav.training.platform.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeActionService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.service.CourseQueryService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code AssignmentAdministrativeAction}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentAdministrativeActionControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-10T11:00:00Z");

    @Mock
    private AssignmentAdministrativeActionService assignmentAdministrativeActionService;
    @Mock
    private CourseQueryService courseQueryService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        lenient().when(courseQueryService.findCourseById(301L))
            .thenReturn(new Course(301L, "Course 301", "Course", ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AssignmentAdministrativeActionController(assignmentAdministrativeActionService, courseQueryService)
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void cancelEndpointCallsTypedOwnerServiceAndReturnsAssignmentRootResponse() throws Exception {
        when(assignmentAdministrativeActionService.cancelAssignment(any()))
            .thenReturn(assignment(17L, AssignmentStatus.CANCELLED, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600)));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/cancel/17")
                .contentType("application/json")
                .content("""
                    {
                      "note": "manual cancel"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.campaignId").value(44))
            .andExpect(jsonPath("$.userId").value(101))
            .andExpect(jsonPath("$.courseId").value(301))
            .andExpect(jsonPath("$.courseName").value("Course 301"));

        ArgumentCaptor<AssignmentAdministrativeActionService.CancelAssignmentCommand> commandCaptor =
            ArgumentCaptor.forClass(AssignmentAdministrativeActionService.CancelAssignmentCommand.class);
        verify(assignmentAdministrativeActionService).cancelAssignment(commandCaptor.capture());
        AssignmentAdministrativeActionService.CancelAssignmentCommand command = commandCaptor.getValue();
        assertThat(command.assignmentId()).isEqualTo(17L);
        assertThat(command.note()).isEqualTo("manual cancel");
    }

    @Test
    void deadlineExtendEndpointCallsTypedOwnerServiceAndReturnsAssignmentRootResponse() throws Exception {
        Instant newDeadlineAt = Instant.parse("2026-04-12T10:00:00Z");
        when(assignmentAdministrativeActionService.extendAssignmentDeadline(any()))
            .thenReturn(assignment(17L, AssignmentStatus.ASSIGNED, null, newDeadlineAt));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/deadline-extend/17")
                .contentType("application/json")
                .content("""
                    {
                      "newDeadlineAt": "2026-04-12T10:00:00Z",
                      "note": "extend"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.status").value("ASSIGNED"))
            .andExpect(jsonPath("$.deadlineAt").value("2026-04-12T10:00:00Z"));

        ArgumentCaptor<AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand> commandCaptor =
            ArgumentCaptor.forClass(AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand.class);
        verify(assignmentAdministrativeActionService).extendAssignmentDeadline(commandCaptor.capture());
        AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand command = commandCaptor.getValue();
        assertThat(command.assignmentId()).isEqualTo(17L);
        assertThat(command.newDeadlineAt()).isEqualTo(newDeadlineAt);
        assertThat(command.note()).isEqualTo("extend");
    }

    @Test
    void cancelEndpointPropagatesNotFoundThroughExistingExceptionMapping() throws Exception {
        when(assignmentAdministrativeActionService.cancelAssignment(any()))
            .thenThrow(new NotFoundException("Assignment not found: 17"));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/cancel/17")
                .contentType("application/json")
                .content("""
                    {
                      "note": "manual cancel"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Assignment not found: 17"));
    }

    @Test
    void deadlineExtendEndpointPropagatesConflictThroughExistingExceptionMapping() throws Exception {
        when(assignmentAdministrativeActionService.extendAssignmentDeadline(any()))
            .thenThrow(new ConflictException("Assignment deadline extension conflicts with active state"));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/deadline-extend/17")
                .contentType("application/json")
                .content("""
                    {
                      "newDeadlineAt": "2026-04-12T10:00:00Z",
                      "note": "extend"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Assignment deadline extension conflicts with active state"));
    }

    @Test
    void cancelEndpointPropagatesPolicyDeniedThroughExistingExceptionMapping() throws Exception {
        when(assignmentAdministrativeActionService.cancelAssignment(any()))
            .thenThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "Cancel denied by policy"));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/cancel/17")
                .contentType("application/json")
                .content("""
                    {
                      "note": "manual cancel"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Cancel denied by policy"));
    }

    @Test
    void invalidDeadlineExtendBodyFailsValidationBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/v1/assignment-administrative-actions/deadline-extend/17")
                .contentType("application/json")
                .content("""
                    {
                      "note": "extend"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void replaceWithNewEndpointCallsTypedOwnerServiceAndReturnsAssignmentRootResponse() throws Exception {
        when(assignmentAdministrativeActionService.replaceWithNewAssignment(any()))
            .thenReturn(assignment(17L, AssignmentStatus.ASSIGNED, FIXED_INSTANT, Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/replace-with-new/17")
                .contentType("application/json")
                .content("""
                    {
                      "campaignId": 44,
                      "newCycleDeadlineAt": "2026-04-12T10:00:00Z",
                      "note": "replace"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.campaignId").value(44))
            .andExpect(jsonPath("$.status").value("ASSIGNED"));

        ArgumentCaptor<AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand> commandCaptor =
            ArgumentCaptor.forClass(AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand.class);
        verify(assignmentAdministrativeActionService).replaceWithNewAssignment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().assignmentId()).isEqualTo(17L);
        assertThat(commandCaptor.getValue().campaignId()).isEqualTo(44L);
        assertThat(commandCaptor.getValue().newCycleDeadlineAt()).isEqualTo(Instant.parse("2026-04-12T10:00:00Z"));
        assertThat(commandCaptor.getValue().note()).isEqualTo("replace");
    }

    @Test
    void replaceWithNewEndpointPropagatesNotFoundThroughExistingExceptionMapping() throws Exception {
        when(assignmentAdministrativeActionService.replaceWithNewAssignment(any()))
            .thenThrow(new NotFoundException("Assignment not found: 17"));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/replace-with-new/17")
                .contentType("application/json")
                .content("""
                    {
                      "campaignId": 44,
                      "newCycleDeadlineAt": "2026-04-12T10:00:00Z",
                      "note": "replace"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Assignment not found: 17"));
    }

    @Test
    void replaceWithNewEndpointPropagatesConflictThroughExistingExceptionMapping() throws Exception {
        when(assignmentAdministrativeActionService.replaceWithNewAssignment(any()))
            .thenThrow(new ConflictException("Assignment replacement conflicts with active uniqueness"));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/replace-with-new/17")
                .contentType("application/json")
                .content("""
                    {
                      "campaignId": 44,
                      "newCycleDeadlineAt": "2026-04-12T10:00:00Z",
                      "note": "replace"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Assignment replacement conflicts with active uniqueness"));
    }

    @Test
    void replaceWithNewEndpointPropagatesPolicyDeniedThroughExistingExceptionMapping() throws Exception {
        when(assignmentAdministrativeActionService.replaceWithNewAssignment(any()))
            .thenThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "Replacement denied by policy"));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/replace-with-new/17")
                .contentType("application/json")
                .content("""
                    {
                      "campaignId": 44,
                      "newCycleDeadlineAt": "2026-04-12T10:00:00Z",
                      "note": "replace"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Replacement denied by policy"));
    }

    @Test
    void invalidReplaceBodyFailsValidationBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/v1/assignment-administrative-actions/replace-with-new/17")
                .contentType("application/json")
                .content("""
                    {
                      "campaignId": 0,
                      "newCycleDeadlineAt": "2026-04-12T10:00:00Z",
                      "note": "replace"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void replaceWithNewEndpointDoesNotRequireOccurredAtInRequest() throws Exception {
        when(assignmentAdministrativeActionService.replaceWithNewAssignment(any()))
            .thenReturn(assignment(17L, AssignmentStatus.ASSIGNED, FIXED_INSTANT, Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(post("/api/v1/assignment-administrative-actions/replace-with-new/17")
                .contentType("application/json")
                .content("""
                    {
                      "campaignId": 44,
                      "newCycleDeadlineAt": "2026-04-12T10:00:00Z",
                      "note": "replace"
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void administrativeControllerSurfaceRemainsTypedWithoutPatchCrudOrReadEndpoints() {
        assertThat(Arrays.stream(AssignmentAdministrativeActionController.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactlyInAnyOrder("cancelAssignment", "extendAssignmentDeadline", "replaceWithNewAssignment")
            .doesNotContain(
                "patchAssignment",
                "updateAssignment",
                "findAssignmentById",
                "listAssignments",
                "launchCampaign"
            );
    }

    private Assignment assignment(
        Long assignmentId,
        AssignmentStatus status,
        Instant cancelledAt,
        Instant deadlineAt
    ) {
        return new Assignment(
            assignmentId,
            44L,
            101L,
            301L,
            status,
            FIXED_INSTANT.minusSeconds(3600),
            deadlineAt,
            cancelledAt,
            null,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT
        );
    }
}
