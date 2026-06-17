package com.vladislav.training.platform.assignment.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.controller.dto.AssignedLearningContextResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignedMaterialContentResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignedTestContextResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignmentResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignmentTestResponse;
import com.vladislav.training.platform.assignment.service.AssignedTestContext;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.service.AssignedLearningContext;
import com.vladislav.training.platform.assignment.service.AssignedMaterialContent;
import com.vladislav.training.platform.assignment.service.AssignmentSelfScopedQueryService;
import com.vladislav.training.platform.content.controller.dto.CourseResponse;
import com.vladislav.training.platform.content.controller.dto.MaterialResponse;
import com.vladislav.training.platform.content.controller.dto.TopicResponse;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContextReader;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code AssignmentSelfScopedReadController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/assigned-learning/assignments")
public class AssignmentSelfScopedReadController {

    /*
     * Contract note: Dedicated self-scoped REST adapter.
     */

    private final AssignmentSelfScopedQueryService assignmentSelfScopedQueryService;
    private final InteractiveActorResolver interactiveActorResolver;
    private final PublishedCourseLearningContextReader publishedCourseLearningContextReader;
    private final TestRepository testRepository;
    private final TopicRepository topicRepository;

    public AssignmentSelfScopedReadController(
        AssignmentSelfScopedQueryService assignmentSelfScopedQueryService,
        InteractiveActorResolver interactiveActorResolver,
        PublishedCourseLearningContextReader publishedCourseLearningContextReader,
        TestRepository testRepository,
        TopicRepository topicRepository
    ) {
        this.assignmentSelfScopedQueryService = Objects.requireNonNull(
            assignmentSelfScopedQueryService,
            "assignmentSelfScopedQueryService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
        this.publishedCourseLearningContextReader = Objects.requireNonNull(
            publishedCourseLearningContextReader,
            "publishedCourseLearningContextReader must not be null"
        );
        this.testRepository = Objects.requireNonNull(testRepository, "testRepository must not be null");
        this.topicRepository = Objects.requireNonNull(topicRepository, "topicRepository must not be null");
    }

    @GetMapping
    public List<AssignmentResponse> findSelfAssignments() {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return assignmentSelfScopedQueryService.findSelfAssignments(actorUserId).stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{assignmentId}")
    public AssignmentResponse findSelfAssignmentById(@PathVariable @Positive Long assignmentId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(assignmentSelfScopedQueryService.findSelfAssignmentById(actorUserId, assignmentId));
    }

    @GetMapping("/{assignmentId}/learning-context")
    public AssignedLearningContextResponse findAssignedLearningContext(@PathVariable @Positive Long assignmentId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toLearningContextResponse(
            assignmentSelfScopedQueryService.findAssignedLearningContext(actorUserId, assignmentId)
        );
    }

    @GetMapping("/{assignmentId}/materials/{materialId}")
    public AssignedMaterialContentResponse findAssignedMaterialContent(
        @PathVariable @Positive Long assignmentId,
        @PathVariable @Positive Long materialId
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toAssignedMaterialContentResponse(
            assignmentSelfScopedQueryService.findAssignedMaterialContent(actorUserId, assignmentId, materialId)
        );
    }

    @GetMapping("/{assignmentId}/assignment-tests/{assignmentTestId}/test-context")
    public AssignedTestContextResponse findAssignedTestContext(
        @PathVariable @Positive Long assignmentId,
        @PathVariable @Positive Long assignmentTestId
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toAssignedTestContextResponse(
            assignmentSelfScopedQueryService.findAssignedTestContext(actorUserId, assignmentId, assignmentTestId)
        );
    }

    private AssignedLearningContextResponse toLearningContextResponse(AssignedLearningContext context) {
        return new AssignedLearningContextResponse(
            toResponse(context.assignment()),
            context.assignmentTests().stream().map(this::toResponse).toList(),
            toResponse(context.publishedLearningContext().course()),
            context.publishedLearningContext().topics().stream().map(this::toResponse).toList(),
            context.publishedLearningContext().materials().stream().map(this::toResponse).toList()
        );
    }

    private AssignedTestContextResponse toAssignedTestContextResponse(AssignedTestContext context) {
        return new AssignedTestContextResponse(
            context.assignmentId(),
            context.assignmentTestId(),
            context.testId(),
            context.testName(),
            context.questions().stream()
                .map(question -> new AssignedTestContextResponse.AssignedTestQuestionResponse(
                    question.questionId(),
                    question.body(),
                    question.questionType(),
                    question.displayOrder(),
                    question.answerOptions().stream()
                        .map(answerOption -> new AssignedTestContextResponse.AssignedTestAnswerOptionResponse(
                            answerOption.answerOptionId(),
                            answerOption.body(),
                            answerOption.answerOptionRole(),
                            answerOption.displayOrder()
                        ))
                        .toList()
                ))
                .toList()
        );
    }

    private AssignedMaterialContentResponse toAssignedMaterialContentResponse(AssignedMaterialContent content) {
        return new AssignedMaterialContentResponse(
            content.assignmentId(),
            toResponse(content.course()),
            toResponse(content.topic()),
            toResponse(content.material())
        );
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        Course course = publishedCourseLearningContextReader.readPublishedCourseLearningContext(assignment.courseId()).course();
        return new AssignmentResponse(
            assignment.id(),
            assignment.campaignId(),
            assignment.userId(),
            assignment.courseId(),
            course.name(),
            assignment.status(),
            assignment.assignedAt(),
            assignment.deadlineAt(),
            assignment.cancelledAt(),
            assignment.closedAt(),
            assignment.createdAt(),
            assignment.updatedAt()
        );
    }

    private AssignmentTestResponse toResponse(AssignmentTest assignmentTest) {
        Test test = testRepository.findTestById(assignmentTest.testId());
        Topic topic = topicRepository.findTopicById(test.topicId());
        return new AssignmentTestResponse(
            assignmentTest.id(),
            assignmentTest.assignmentId(),
            assignmentTest.testId(),
            test.name(),
            topic.name(),
            assignmentTest.assignmentTestRole(),
            assignmentTest.countedResultId(),
            assignmentTest.closedAt(),
            assignmentTest.isClosed(),
            assignmentTest.createdAt(),
            assignmentTest.updatedAt()
        );
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse(
            course.id(),
            course.name(),
            course.description(),
            course.status(),
            course.sortOrder(),
            course.createdAt(),
            course.updatedAt()
        );
    }

    private TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
            topic.id(),
            topic.courseId(),
            topic.name(),
            topic.description(),
            topic.status(),
            topic.sortOrder(),
            topic.createdAt(),
            topic.updatedAt()
        );
    }

    private MaterialResponse toResponse(Material material) {
        return new MaterialResponse(
            material.id(),
            material.topicId(),
            material.name(),
            material.description(),
            material.body(),
            material.videoUrl(),
            material.materialType(),
            material.status(),
            material.sortOrder(),
            material.createdAt(),
            material.updatedAt()
        );
    }
}
