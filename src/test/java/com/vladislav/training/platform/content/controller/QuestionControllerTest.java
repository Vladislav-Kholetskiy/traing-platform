package com.vladislav.training.platform.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.service.CreateAnswerOptionCommand;
import com.vladislav.training.platform.content.service.CreateQuestionCommand;
import com.vladislav.training.platform.content.service.QuestionCommandService;
import com.vladislav.training.platform.content.service.QuestionQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code Question}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private QuestionCommandService commandService;
    @Mock private QuestionQueryService queryService;
    @Mock private UtcClock utcClock;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new QuestionController(commandService, queryService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void createDelegatesToDraftCommandService() throws Exception {
        when(commandService.createQuestion(any())).thenReturn(
            new Question(20L, 30L, "Body", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT)
        );

        mockMvc.perform(post("/api/v1/expert/content/questions")
                .contentType("application/json")
                .content("""
                    {
                      "topicId": 30,
                      "body": "Body",
                      "questionType": "SINGLE_CHOICE",
                      "sortOrder": 0,
                      "status": "PUBLISHED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(20))
            .andExpect(jsonPath("$.topicId").value(30))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        ArgumentCaptor<CreateQuestionCommand> captor = ArgumentCaptor.forClass(CreateQuestionCommand.class);
        verify(commandService).createQuestion(captor.capture());
        assertThat(captor.getValue().topicId()).isEqualTo(30L);
        assertThat(captor.getValue().body()).isEqualTo("Body");
        assertThat(captor.getValue().questionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(captor.getValue().sortOrder()).isEqualTo(0);
    }

    @Test
    void getAnswerOptionsReturnsCompositionReadSurface() throws Exception {
        when(queryService.findAnswerOptionsByQuestionId(20L)).thenReturn(List.of(
            new AnswerOption(10L, 20L, "A", AnswerOptionRole.CHOICE_OPTION, true, 0, null, null, FIXED_INSTANT, FIXED_INSTANT)
        ));

        mockMvc.perform(get("/api/v1/expert/content/questions/20/answer-options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].questionId").value(20));
    }

    @Test
    void createAnswerOptionDelegatesToDraftCommandService() throws Exception {
        when(commandService.createAnswerOption(any(), any())).thenReturn(
            new AnswerOption(10L, 20L, "A", AnswerOptionRole.CHOICE_OPTION, true, 0, null, null, FIXED_INSTANT, FIXED_INSTANT)
        );

        mockMvc.perform(post("/api/v1/expert/content/questions/20/answer-options")
                .contentType("application/json")
                .content("""
                    {
                      "body": "A",
                      "answerOptionRole": "CHOICE_OPTION",
                      "isCorrect": true,
                      "displayOrder": 0
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.questionId").value(20));

        ArgumentCaptor<CreateAnswerOptionCommand> captor = ArgumentCaptor.forClass(CreateAnswerOptionCommand.class);
        verify(commandService).createAnswerOption(org.mockito.ArgumentMatchers.eq(20L), captor.capture());
        assertThat(captor.getValue().body()).isEqualTo("A");
        assertThat(captor.getValue().answerOptionRole()).isEqualTo(AnswerOptionRole.CHOICE_OPTION);
        assertThat(captor.getValue().isCorrect()).isTrue();
        assertThat(captor.getValue().displayOrder()).isEqualTo(0);
        assertThat(captor.getValue().pairingKey()).isNull();
        assertThat(captor.getValue().canonicalOrderPosition()).isNull();
    }

    @Test
    void updateDelegatesWithoutControllerSideStateReconstruction() throws Exception {
        when(commandService.updateQuestion(any(), any())).thenReturn(
            new Question(20L, 30L, "Body", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT)
        );

        mockMvc.perform(patch("/api/v1/expert/content/questions/20")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "body", "Body",
                    "questionType", "SINGLE_CHOICE",
                    "sortOrder", 0
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(20));

        verify(commandService).updateQuestion(any(), any());
    }

    @Test
    void updateIgnoresLifecycleFieldInGenericSurface() throws Exception {
        when(commandService.updateQuestion(any(), any())).thenReturn(
            new Question(20L, 30L, "Body", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT)
        );

        mockMvc.perform(patch("/api/v1/expert/content/questions/20")
                .contentType("application/json")
                .content("""
                    {
                      "body": "Body",
                      "questionType": "SINGLE_CHOICE",
                      "sortOrder": 0,
                      "status": "PUBLISHED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(20))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(commandService).updateQuestion(any(), any());
    }
}
