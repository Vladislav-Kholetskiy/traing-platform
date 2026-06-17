package com.vladislav.training.platform.assignment.controller;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.service.AssignedLearningContext;
import com.vladislav.training.platform.assignment.service.AssignedTestContext;
import com.vladislav.training.platform.assignment.service.AssignmentSelfScopedQueryService;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContext;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContextReader;
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
 * Проверяет поведение контроллера {@code AssignmentSelfScopedRead}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentSelfScopedReadControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T10:00:00Z");

    @Mock
    private AssignmentSelfScopedQueryService assignmentSelfScopedQueryService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private PublishedCourseLearningContextReader publishedCourseLearningContextReader;
    @Mock
    private TestRepository testRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        lenient().when(publishedCourseLearningContextReader.readPublishedCourseLearningContext(301L))
            .thenReturn(publishedLearningContext(301L));
        lenient().when(testRepository.findTestById(501L))
            .thenReturn(new com.vladislav.training.platform.content.domain.Test(
                501L,
                401L,
                "Итоговый тест по теме",
                "Published assigned test",
                TestType.CONTROL,
                ContentStatus.PUBLISHED,
                BigDecimal.valueOf(70),
                "DEFAULT",
                true,
                0,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));
        lenient().when(testRepository.findTestById(502L))
            .thenReturn(new com.vladislav.training.platform.content.domain.Test(
                502L,
                401L,
                "Промежуточный тест по теме",
                "Published assigned test",
                TestType.CONTROL,
                ContentStatus.PUBLISHED,
                BigDecimal.valueOf(70),
                "DEFAULT",
                false,
                1,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));
        lenient().when(topicRepository.findTopicById(401L))
            .thenReturn(new Topic(401L, 301L, "Topic", "Published topic", ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AssignmentSelfScopedReadController(
                    assignmentSelfScopedQueryService,
                    interactiveActorResolver,
                    publishedCourseLearningContextReader,
                    testRepository,
                    topicRepository
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void selfScopedListEndpointUsesTrustedActorIdentityAndReturnsAssignments() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findSelfAssignments(101L))
            .thenReturn(List.of(assignment(77L, 101L), assignment(78L, 101L)));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(77))
            .andExpect(jsonPath("$[0].userId").value(101))
            .andExpect(jsonPath("$[0].courseName").value("Course"))
            .andExpect(jsonPath("$[1].id").value(78));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findSelfAssignments(101L);
    }

    @Test
    void selfScopedDetailEndpointUsesTrustedActorIdentityAndReturnsAssignment() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findSelfAssignmentById(101L, 77L))
            .thenReturn(assignment(77L, 101L));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/77"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(77))
            .andExpect(jsonPath("$.userId").value(101))
            .andExpect(jsonPath("$.courseName").value("Course"))
            .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findSelfAssignmentById(101L, 77L);
    }

    @Test
    void requestUserIdParameterDoesNotOverrideResolvedActorIdentity() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findSelfAssignments(101L))
            .thenReturn(List.of(assignment(77L, 101L)));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments").queryParam("userId", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(101));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findSelfAssignments(101L);
    }

    @Test
    void deniedSelfScopedReadPropagatesThroughExistingExceptionMapping() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findSelfAssignmentById(101L, 77L))
            .thenThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "Actor is not authorized to read self-scoped assignment data"));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/77"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Actor is not authorized to read self-scoped assignment data"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findSelfAssignmentById(101L, 77L);
    }

    @Test
    void ownerSafeNotFoundPropagatesFromSelfScopedDetailPath() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findSelfAssignmentById(101L, 77L))
            .thenThrow(new NotFoundException("Assignment not found in self scope: actorUserId=101, assignmentId=77"));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/77"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Assignment not found in self scope: actorUserId=101, assignmentId=77"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findSelfAssignmentById(101L, 77L);
    }

    @Test
    void assignedLearningContextEndpointUsesTrustedActorIdentityAndReturnsOwnedAssignmentLinkedContext() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findAssignedLearningContext(101L, 77L))
            .thenReturn(new AssignedLearningContext(
                assignment(77L, 101L),
                List.of(
                    assignmentTest(701L, 77L, 501L, null, false),
                    assignmentTest(702L, 77L, 502L, 9002L, true)
                ),
                publishedLearningContext(301L)
            ));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/77/learning-context"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignment.id").value(77))
            .andExpect(jsonPath("$.assignment.userId").value(101))
            .andExpect(jsonPath("$.assignmentTests.length()").value(2))
            .andExpect(jsonPath("$.assignmentTests[0].id").value(701))
            .andExpect(jsonPath("$.assignmentTests[0].assignmentId").value(77))
            .andExpect(jsonPath("$.assignmentTests[0].testName").value("Итоговый тест по теме"))
            .andExpect(jsonPath("$.assignmentTests[0].topicName").value("Topic"))
            .andExpect(jsonPath("$.assignmentTests[1].countedResultId").value(9002))
            .andExpect(jsonPath("$.assignmentTests[1].isClosed").value(true))
            .andExpect(jsonPath("$.publishedCourse.id").value(301))
            .andExpect(jsonPath("$.publishedCourse.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.publishedTopics.length()").value(1))
            .andExpect(jsonPath("$.publishedTopics[0].courseId").value(301))
            .andExpect(jsonPath("$.publishedMaterials.length()").value(1))
            .andExpect(jsonPath("$.publishedMaterials[0].topicId").value(401))
            .andExpect(jsonPath("$.publishedMaterials[0].materialType").value("TEXT"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findAssignedLearningContext(101L, 77L);
    }

    @Test
    void deniedAssignedLearningContextPropagatesThroughExistingExceptionMapping() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findAssignedLearningContext(101L, 77L))
            .thenThrow(new PolicyViolationException(
                "ACTOR_NOT_AUTHORIZED",
                "Actor is not authorized to read assigned learning context"
            ));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/77/learning-context"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Actor is not authorized to read assigned learning context"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findAssignedLearningContext(101L, 77L);
    }

    @Test
    void ownerSafeNotFoundPropagatesFromAssignedLearningContextPath() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findAssignedLearningContext(101L, 77L))
            .thenThrow(new NotFoundException("Assignment not found in self scope: actorUserId=101, assignmentId=77"));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/77/learning-context"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Assignment not found in self scope: actorUserId=101, assignmentId=77"));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findAssignedLearningContext(101L, 77L);
    }

    @Test
    void assignedTestContextEndpointUsesTrustedActorIdentityAndReturnsOwnedAssignmentBoundComposition() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findAssignedTestContext(101L, 77L, 701L))
            .thenReturn(new AssignedTestContext(
                77L,
                701L,
                501L,
                "Final test",
                List.of(
                    new AssignedTestContext.AssignedTestQuestionContext(
                        9001L,
                        "Question B",
                        com.vladislav.training.platform.content.domain.QuestionType.SINGLE_CHOICE,
                        0,
                        List.of(
                            new AssignedTestContext.AssignedTestAnswerOptionContext(
                                9101L,
                                "Option A",
                                AnswerOptionRole.CHOICE_OPTION,
                                0
                            )
                        )
                    )
                )
            ));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/77/assignment-tests/701/test-context"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignmentId").value(77))
            .andExpect(jsonPath("$.assignmentTestId").value(701))
            .andExpect(jsonPath("$.testId").value(501))
            .andExpect(jsonPath("$.testName").value("Final test"))
            .andExpect(jsonPath("$.questions[0].questionId").value(9001))
            .andExpect(jsonPath("$.questions[0].displayOrder").value(0))
            .andExpect(jsonPath("$.questions[0].answerOptions[0].answerOptionId").value(9101))
            .andExpect(jsonPath("$.questions[0].answerOptions[0].displayOrder").value(0))
            .andExpect(jsonPath("$.questions[0].answerOptions[0].isCorrect").doesNotExist())
            .andExpect(jsonPath("$.questions[0].answerOptions[0].correct").doesNotExist())
            .andExpect(jsonPath("$.questions[0].weight").doesNotExist())
            .andExpect(jsonPath("$.scoringPolicyCode").doesNotExist());

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findAssignedTestContext(101L, 77L, 701L);
    }

    @Test
    void ownerSafeNotFoundPropagatesFromAssignedTestContextPath() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(assignmentSelfScopedQueryService.findAssignedTestContext(101L, 77L, 701L))
            .thenThrow(new NotFoundException(
                "Assignment test not found in self-scoped assignment context: assignmentId=77, assignmentTestId=701"
            ));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/77/assignment-tests/701/test-context"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Assignment test not found in self-scoped assignment context: assignmentId=77, assignmentTestId=701"
            ));

        verify(interactiveActorResolver).resolveActorUserId();
        verify(assignmentSelfScopedQueryService).findAssignedTestContext(101L, 77L, 701L);
    }

    private Assignment assignment(Long assignmentId, Long userId) {
        return new Assignment(
            assignmentId,
            900L,
            userId,
            301L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(86400),
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentTest assignmentTest(
        Long assignmentTestId,
        Long assignmentId,
        Long testId,
        Long countedResultId,
        boolean closed
    ) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            testId,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            countedResultId,
            closed ? FIXED_INSTANT : null,
            closed,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private PublishedCourseLearningContext publishedLearningContext(Long courseId) {
        return new PublishedCourseLearningContext(
            new Course(courseId, "Course", "Published course", ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT),
            List.of(new Topic(401L, courseId, "Topic", "Published topic", ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT)),
            List.of(new Material(
                501L,
                401L,
                "Material",
                "Published material",
                null,
                null,
                MaterialType.TEXT,
                ContentStatus.PUBLISHED,
                0,
                FIXED_INSTANT,
                FIXED_INSTANT
            ))
        );
    }
}
