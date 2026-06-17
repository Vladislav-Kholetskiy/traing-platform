package com.vladislav.training.platform.assignment.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.service.AssignmentQueryService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.service.CourseQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
/**
 * Проверяет поведение контроллера {@code AssignmentAdminRead}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
class AssignmentAdminReadControllerTest {

    @Mock
    private AssignmentQueryService assignmentQueryService;
    @Mock
    private CourseQueryService courseQueryService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(Instant.parse("2026-06-07T09:00:00Z"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AssignmentAdminReadController(assignmentQueryService, courseQueryService)
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void listEndpointReturnsAssignmentsUsingCampaignFilter() throws Exception {
        when(assignmentQueryService.findAssignmentsByCampaignId(77L)).thenReturn(List.of(
            assignment(15L, 77L, 700L, 300L, AssignmentStatus.ASSIGNED)
        ));
        when(courseQueryService.findCourseById(300L)).thenReturn(course(300L, "Safe course"));

        mockMvc.perform(get("/api/v1/admin/assignments").param("campaignId", "77"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(15))
            .andExpect(jsonPath("$[0].courseName").value("Safe course"))
            .andExpect(jsonPath("$[0].status").value("ASSIGNED"));

        verify(assignmentQueryService).findAssignmentsByCampaignId(77L);
    }

    @Test
    void detailEndpointReturnsAssignmentById() throws Exception {
        when(assignmentQueryService.findAssignmentById(21L)).thenReturn(
            assignment(21L, 90L, 701L, 301L, AssignmentStatus.OVERDUE)
        );
        when(courseQueryService.findCourseById(301L)).thenReturn(course(301L, "Ops course"));

        mockMvc.perform(get("/api/v1/admin/assignments/21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(21))
            .andExpect(jsonPath("$.campaignId").value(90))
            .andExpect(jsonPath("$.courseName").value("Ops course"));
    }

    private Assignment assignment(
        Long id,
        Long campaignId,
        Long userId,
        Long courseId,
        AssignmentStatus status
    ) {
        Instant now = Instant.parse("2026-06-07T09:00:00Z");
        return new Assignment(id, campaignId, userId, courseId, status, now, now.plusSeconds(3600), null, null, now, now);
    }

    private Course course(Long id, String name) {
        Instant now = Instant.parse("2026-06-07T09:00:00Z");
        return new Course(id, name, "desc", ContentStatus.PUBLISHED, 0, now, now);
    }
}
