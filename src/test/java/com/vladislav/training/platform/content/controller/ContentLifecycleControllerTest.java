package com.vladislav.training.platform.content.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.service.ContentLifecycleQueryService;
import com.vladislav.training.platform.content.service.CourseLifecycleService;
import com.vladislav.training.platform.content.service.MaterialLifecycleService;
import com.vladislav.training.platform.content.service.QuestionLifecycleService;
import com.vladislav.training.platform.content.service.TestLifecycleService;
import com.vladislav.training.platform.content.service.TopicLifecycleService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code ContentLifecycle}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class ContentLifecycleControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private ContentLifecycleQueryService lifecycleQueryService;
    @Mock private CourseLifecycleService courseLifecycleService;
    @Mock private TopicLifecycleService topicLifecycleService;
    @Mock private MaterialLifecycleService materialLifecycleService;
    @Mock private QuestionLifecycleService questionLifecycleService;
    @Mock private TestLifecycleService testLifecycleService;
    @Mock private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ContentLifecycleController controller = new ContentLifecycleController(
            lifecycleQueryService,
            courseLifecycleService,
            topicLifecycleService,
            materialLifecycleService,
            questionLifecycleService,
            testLifecycleService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void publishCourseDelegatesToLifecycleService() throws Exception {
        when(courseLifecycleService.publish(10L)).thenReturn(new Course(10L, "Course", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/courses/10/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(courseLifecycleService).publish(10L);
    }

    @Test
    void archiveCourseDelegatesToLifecycleService() throws Exception {
        when(courseLifecycleService.archive(10L)).thenReturn(new Course(10L, "Course", null, ContentStatus.ARCHIVED, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/courses/10/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("ARCHIVED"));

        verify(courseLifecycleService).archive(10L);
    }

    @Test
    void publishTopicDelegatesToLifecycleService() throws Exception {
        when(topicLifecycleService.publish(20L)).thenReturn(new Topic(20L, 10L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/topics/20/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(20))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(topicLifecycleService).publish(20L);
    }

    @Test
    void archiveTopicDelegatesToLifecycleService() throws Exception {
        when(topicLifecycleService.archive(20L)).thenReturn(new Topic(20L, 10L, "Topic", null, ContentStatus.ARCHIVED, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/topics/20/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(20))
            .andExpect(jsonPath("$.status").value("ARCHIVED"));

        verify(topicLifecycleService).archive(20L);
    }

    @Test
    void publishMaterialDelegatesToLifecycleService() throws Exception {
        when(materialLifecycleService.publish(30L)).thenReturn(new Material(30L, 20L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/materials/30/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(30))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(materialLifecycleService).publish(30L);
    }

    @Test
    void archiveMaterialDelegatesToLifecycleService() throws Exception {
        when(materialLifecycleService.archive(30L)).thenReturn(new Material(30L, 20L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.ARCHIVED, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/materials/30/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(30))
            .andExpect(jsonPath("$.status").value("ARCHIVED"));

        verify(materialLifecycleService).archive(30L);
    }

    @Test
    void publishQuestionDelegatesToLifecycleService() throws Exception {
        when(questionLifecycleService.publish(40L)).thenReturn(new Question(40L, 20L, "Body", QuestionType.SINGLE_CHOICE, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/questions/40/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(40))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(questionLifecycleService).publish(40L);
    }

    @Test
    void archiveQuestionDelegatesToLifecycleService() throws Exception {
        when(questionLifecycleService.archive(40L)).thenReturn(new Question(40L, 20L, "Body", QuestionType.SINGLE_CHOICE, ContentStatus.ARCHIVED, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/questions/40/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(40))
            .andExpect(jsonPath("$.status").value("ARCHIVED"));

        verify(questionLifecycleService).archive(40L);
    }

    @Test
    void publishTestReturnsLifecycleSnapshot() throws Exception {
        when(testLifecycleService.publish(70L)).thenReturn(new com.vladislav.training.platform.content.domain.Test(
            70L, 20L, "Final", null, TestType.CONTROL, ContentStatus.PUBLISHED,
            BigDecimal.valueOf(80), "DEFAULT", false, 0, FIXED_INSTANT, FIXED_INSTANT
        ));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/tests/70/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(70))
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.isActiveFinalForTopic").doesNotExist());

        verify(testLifecycleService).publish(70L);
    }

    @Test
    void archiveTestDelegatesToLifecycleService() throws Exception {
        when(testLifecycleService.archive(70L)).thenReturn(new com.vladislav.training.platform.content.domain.Test(
            70L, 20L, "Final", null, TestType.CONTROL, ContentStatus.ARCHIVED,
            BigDecimal.valueOf(80), "DEFAULT", false, 0, FIXED_INSTANT, FIXED_INSTANT
        ));

        mockMvc.perform(post("/api/v1/expert/content/lifecycle/tests/70/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(70))
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
            .andExpect(jsonPath("$.isActiveFinalForTopic").doesNotExist());

        verify(testLifecycleService).archive(70L);
    }
}
