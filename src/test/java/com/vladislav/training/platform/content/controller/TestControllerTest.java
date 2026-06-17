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
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.service.CreateTestCommand;
import com.vladislav.training.platform.content.service.CreateTestQuestionCommand;
import com.vladislav.training.platform.content.service.TestCommandService;
import com.vladislav.training.platform.content.service.TestQueryService;
import com.vladislav.training.platform.content.service.UpdateTestCommand;
import java.math.BigDecimal;
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
 * Проверяет поведение контроллера {@code Test}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class TestControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TestCommandService commandService;
    @Mock private TestQueryService queryService;
    @Mock private UtcClock utcClock;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController(commandService, queryService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void createDelegatesToDraftCommandService() throws Exception {
        when(commandService.createTest(any())).thenReturn(
            new com.vladislav.training.platform.content.domain.Test(
                70L,
                20L,
                "Final",
                null,
                TestType.CONTROL,
                ContentStatus.DRAFT,
                BigDecimal.valueOf(80),
                "DEFAULT",
                false,
                0,
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        );

        mockMvc.perform(post("/api/v1/expert/content/tests")
                .contentType("application/json")
                .content("""
                    {
                      "topicId": 20,
                      "name": "Final",
                      "description": "",
                      "testType": "CONTROL",
                      "thresholdPercent": 80,
                      "scoringPolicyCode": "DEFAULT",
                      "sortOrder": 0
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(70))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        ArgumentCaptor<CreateTestCommand> captor = ArgumentCaptor.forClass(CreateTestCommand.class);
        verify(commandService).createTest(captor.capture());
        assertThat(captor.getValue().topicId()).isEqualTo(20L);
        assertThat(captor.getValue().name()).isEqualTo("Final");
        assertThat(captor.getValue().description()).isEmpty();
        assertThat(captor.getValue().testType()).isEqualTo(TestType.CONTROL);
        assertThat(captor.getValue().thresholdPercent()).isEqualByComparingTo("80");
        assertThat(captor.getValue().scoringPolicyCode()).isEqualTo("DEFAULT");
        assertThat(captor.getValue().sortOrder()).isEqualTo(0);
    }

    @Test
    void createIgnoresLifecycleAndFinalControlFieldsInGenericSurface() throws Exception {
        when(commandService.createTest(any())).thenReturn(
            new com.vladislav.training.platform.content.domain.Test(
                70L,
                20L,
                "Final",
                null,
                TestType.CONTROL,
                ContentStatus.DRAFT,
                BigDecimal.valueOf(80),
                "DEFAULT",
                false,
                0,
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        );

        mockMvc.perform(post("/api/v1/expert/content/tests")
                .contentType("application/json")
                .content("""
                    {
                      "topicId": 20,
                      "name": "Final",
                      "description": "",
                      "testType": "CONTROL",
                      "thresholdPercent": 80,
                      "scoringPolicyCode": "DEFAULT",
                      "sortOrder": 0,
                      "status": "PUBLISHED",
                      "isActiveFinalForTopic": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(70))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.isActiveFinalForTopic").doesNotExist());

        ArgumentCaptor<CreateTestCommand> captor = ArgumentCaptor.forClass(CreateTestCommand.class);
        verify(commandService).createTest(captor.capture());
        assertThat(captor.getValue().topicId()).isEqualTo(20L);
        assertThat(captor.getValue().name()).isEqualTo("Final");
        assertThat(captor.getValue().testType()).isEqualTo(TestType.CONTROL);
    }

    @Test
    void getTestQuestionsReturnsCompositionReadSurface() throws Exception {
        when(queryService.findTestQuestionsByTestId(70L)).thenReturn(List.of(
            new TestQuestion(30L, 70L, 80L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT)
        ));

        mockMvc.perform(get("/api/v1/expert/content/tests/70/questions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(30))
            .andExpect(jsonPath("$[0].testId").value(70));
    }

    @Test
    void getByIdDoesNotExposeActiveFinalFieldInGenericSurface() throws Exception {
        when(queryService.findTestById(70L)).thenReturn(
            new com.vladislav.training.platform.content.domain.Test(
                70L,
                20L,
                "Final",
                null,
                TestType.CONTROL,
                ContentStatus.PUBLISHED,
                BigDecimal.valueOf(80),
                "DEFAULT",
                true,
                0,
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        );

        mockMvc.perform(get("/api/v1/expert/content/tests/70"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(70))
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.isActiveFinalForTopic").doesNotExist());
    }

    @Test
    void createTestQuestionDelegatesToDraftCommandService() throws Exception {
        when(commandService.createTestQuestion(any(), any())).thenReturn(
            new TestQuestion(30L, 70L, 80L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT)
        );

        mockMvc.perform(post("/api/v1/expert/content/tests/70/questions")
                .contentType("application/json")
                .content("""
                    {
                      "questionId": 80,
                      "displayOrder": 0,
                      "weight": 1
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(30))
            .andExpect(jsonPath("$.testId").value(70));

        ArgumentCaptor<CreateTestQuestionCommand> captor = ArgumentCaptor.forClass(CreateTestQuestionCommand.class);
        verify(commandService).createTestQuestion(org.mockito.ArgumentMatchers.eq(70L), captor.capture());
        assertThat(captor.getValue().questionId()).isEqualTo(80L);
        assertThat(captor.getValue().displayOrder()).isEqualTo(0);
        assertThat(captor.getValue().weight()).isEqualByComparingTo("1");
    }

    @Test
    void updateDelegatesWithoutControllerSideStateReconstruction() throws Exception {
        when(commandService.updateTest(any(), any())).thenReturn(
            new com.vladislav.training.platform.content.domain.Test(
                70L,
                20L,
                "Final",
                null,
                TestType.CONTROL,
                ContentStatus.DRAFT,
                BigDecimal.valueOf(80),
                "DEFAULT",
                false,
                0,
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        );

        mockMvc.perform(patch("/api/v1/expert/content/tests/70")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "Final",
                    "description", "",
                    "testType", "CONTROL",
                    "thresholdPercent", 80,
                    "scoringPolicyCode", "DEFAULT",
                    "sortOrder", 0
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(70));

        verify(commandService).updateTest(any(), any());
    }

    @Test
    void updateIgnoresLifecycleAndFinalControlFieldsInGenericSurface() throws Exception {
        when(commandService.updateTest(any(), any())).thenReturn(
            new com.vladislav.training.platform.content.domain.Test(
                70L,
                20L,
                "Final",
                null,
                TestType.CONTROL,
                ContentStatus.DRAFT,
                BigDecimal.valueOf(80),
                "DEFAULT",
                false,
                0,
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        );

        mockMvc.perform(patch("/api/v1/expert/content/tests/70")
                .contentType("application/json")
                .content("""
                    {
                      "name": "Final",
                      "description": "",
                      "testType": "CONTROL",
                      "thresholdPercent": 80,
                      "scoringPolicyCode": "DEFAULT",
                      "sortOrder": 0,
                      "status": "PUBLISHED",
                      "isActiveFinalForTopic": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(70))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.isActiveFinalForTopic").doesNotExist());

        ArgumentCaptor<UpdateTestCommand> captor = ArgumentCaptor.forClass(UpdateTestCommand.class);
        verify(commandService).updateTest(org.mockito.ArgumentMatchers.eq(70L), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Final");
        assertThat(captor.getValue().description()).isEmpty();
        assertThat(captor.getValue().testType()).isEqualTo(TestType.CONTROL);
        assertThat(captor.getValue().thresholdPercent()).isEqualByComparingTo("80");
        assertThat(captor.getValue().scoringPolicyCode()).isEqualTo("DEFAULT");
        assertThat(captor.getValue().sortOrder()).isEqualTo(0);
    }
}
