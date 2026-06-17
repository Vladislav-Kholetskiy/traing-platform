package com.vladislav.training.platform.testing.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.testing.service.SelfVisibleTestCatalogEntryReadModel;
import com.vladislav.training.platform.testing.service.SelfVisibleTestReadModel;
import com.vladislav.training.platform.testing.service.SelfVisibleTestingReadService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code SelfVisibleTestingRead}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class SelfVisibleTestingReadControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T09:30:00Z");

    @Mock
    private SelfVisibleTestingReadService selfVisibleTestingReadService;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(new SelfVisibleTestingReadController(selfVisibleTestingReadService))
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void findSelfVisibleTestsReturnsReadOnlyCatalogProjection() throws Exception {
        when(selfVisibleTestingReadService.findSelfVisibleTests()).thenReturn(List.of(
            new SelfVisibleTestCatalogEntryReadModel(
                41L,
                201L,
                "Java course",
                301L,
                "Basics",
                "Self test",
                "Description",
                TestType.TRAINING
            )
        ));

        mockMvc.perform(get("/api/v1/self-testing/tests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(41))
            .andExpect(jsonPath("$[0].courseId").value(201))
            .andExpect(jsonPath("$[0].courseName").value("Java course"))
            .andExpect(jsonPath("$[0].topicId").value(301))
            .andExpect(jsonPath("$[0].topicName").value("Basics"))
            .andExpect(jsonPath("$[0].testType").value("TRAINING"))
            .andExpect(jsonPath("$[0].attemptId").doesNotExist())
            .andExpect(jsonPath("$[0].resultId").doesNotExist());

        verify(selfVisibleTestingReadService).findSelfVisibleTests();
    }

    @Test
    void findSelfVisibleTestByIdReturnsReadOnlyProjection() throws Exception {
        when(selfVisibleTestingReadService.findSelfVisibleTestById(41L)).thenReturn(readModel());

        mockMvc.perform(get("/api/v1/self-testing/tests/41"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(41))
            .andExpect(jsonPath("$.topicId").value(301))
            .andExpect(jsonPath("$.testType").value("TRAINING"))
            .andExpect(jsonPath("$.questions.length()").value(1))
            .andExpect(jsonPath("$.questions[0].id").value(501))
            .andExpect(jsonPath("$.questions[0].answerOptions[0].id").value(9001))
            .andExpect(jsonPath("$.questions[0].answerOptions[0].answerOptionRole").value("CHOICE_OPTION"))
            .andExpect(jsonPath("$.attemptId").doesNotExist())
            .andExpect(jsonPath("$.resultId").doesNotExist());

        verify(selfVisibleTestingReadService).findSelfVisibleTestById(41L);
    }

    @Test
    void missingSelfVisibleTestUsesExistingExceptionMapping() throws Exception {
        when(selfVisibleTestingReadService.findSelfVisibleTestById(99L))
            .thenThrow(new NotFoundException("Self-visible test not found: 99"));

        mockMvc.perform(get("/api/v1/self-testing/tests/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Self-visible test not found: 99"));
    }

    private SelfVisibleTestReadModel readModel() {
        return new SelfVisibleTestReadModel(
            41L,
            301L,
            "Self test",
            "Description",
            TestType.TRAINING,
            List.of(
                new SelfVisibleTestReadModel.SelfVisibleQuestionReadModel(
                    501L,
                    "Question",
                    QuestionType.SINGLE_CHOICE,
                    0,
                    new BigDecimal("2.00"),
                    List.of(
                        new SelfVisibleTestReadModel.SelfVisibleAnswerOptionReadModel(
                            9001L,
                            "Option A",
                            AnswerOptionRole.CHOICE_OPTION,
                            0
                        )
                    )
                )
            )
        );
    }
}
