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
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.service.CreateTopicCommand;
import com.vladislav.training.platform.content.service.TopicCommandService;
import com.vladislav.training.platform.content.service.TopicQueryService;
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
 * Проверяет поведение контроллера {@code Topic}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class TopicControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TopicCommandService commandService;
    @Mock private TopicQueryService queryService;
    @Mock private UtcClock utcClock;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TopicController(commandService, queryService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void createDelegatesToDraftCommandService() throws Exception {
        when(commandService.createTopic(any())).thenReturn(new Topic(10L, 20L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(post("/api/v1/expert/content/topics")
                .contentType("application/json")
                .content("""
                    {
                      "courseId": 20,
                      "name": "Topic",
                      "description": "",
                      "sortOrder": 0,
                      "status": "PUBLISHED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.courseId").value(20))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        ArgumentCaptor<CreateTopicCommand> captor = ArgumentCaptor.forClass(CreateTopicCommand.class);
        verify(commandService).createTopic(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(20L);
        assertThat(captor.getValue().name()).isEqualTo("Topic");
        assertThat(captor.getValue().description()).isEmpty();
        assertThat(captor.getValue().sortOrder()).isEqualTo(0);
    }

    @Test
    void updateUsesDedicatedUpdateDtoWithoutParentId() throws Exception {
        when(commandService.updateTopic(any(), any())).thenReturn(new Topic(10L, 20L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(patch("/api/v1/expert/content/topics/10")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of("name", "Topic", "description", "", "sortOrder", 0))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courseId").value(20));

        verify(commandService).updateTopic(any(), any());
    }

    @Test
    void updateIgnoresLifecycleFieldInGenericSurface() throws Exception {
        when(commandService.updateTopic(any(), any())).thenReturn(new Topic(10L, 20L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        mockMvc.perform(patch("/api/v1/expert/content/topics/10")
                .contentType("application/json")
                .content("""
                    {
                      "name": "Topic",
                      "description": "",
                      "sortOrder": 0,
                      "status": "PUBLISHED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(commandService).updateTopic(any(), any());
    }
}
