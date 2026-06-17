package com.vladislav.training.platform.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.service.CreateCourseCommand;
import com.vladislav.training.platform.content.service.CourseCommandService;
import com.vladislav.training.platform.content.service.CourseQueryService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code Course}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private CourseCommandService commandService;
    @Mock private CourseQueryService queryService;
    @Mock private UtcClock utcClock;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseController(commandService, queryService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void createDelegatesToDraftCommandServiceAndIgnoresLifecycleField() throws Exception {
        when(commandService.createCourse(any())).thenReturn(new Course(10L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/courses")
                .contentType("application/json")
                .content("""
                    {
                      "name": "Course",
                      "description": "",
                      "sortOrder": 0,
                      "status": "PUBLISHED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        ArgumentCaptor<CreateCourseCommand> captor = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(commandService).createCourse(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Course");
        assertThat(captor.getValue().description()).isEmpty();
        assertThat(captor.getValue().sortOrder()).isEqualTo(0);
    }

    @Test
    void updateDelegatesWithoutControllerReadBeforeWrite() throws Exception {
        when(commandService.updateCourse(any(), any())).thenReturn(new Course(10L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(patch("/api/v1/expert/content/courses/10")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of("name", "Course", "description", "", "sortOrder", 0))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));

        verify(commandService).updateCourse(any(), any());
    }

    @Test
    void updateIgnoresLifecycleFieldInGenericSurface() throws Exception {
        when(commandService.updateCourse(any(), any())).thenReturn(new Course(10L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(patch("/api/v1/expert/content/courses/10")
                .contentType("application/json")
                .content("""
                    {
                      "name": "Course",
                      "description": "",
                      "sortOrder": 0,
                      "status": "PUBLISHED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(commandService).updateCourse(any(), any());
    }
}
