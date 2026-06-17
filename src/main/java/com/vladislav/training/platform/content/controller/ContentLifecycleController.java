package com.vladislav.training.platform.content.controller;

import com.vladislav.training.platform.content.controller.dto.CourseResponse;
import com.vladislav.training.platform.content.controller.dto.MaterialResponse;
import com.vladislav.training.platform.content.controller.dto.QuestionResponse;
import com.vladislav.training.platform.content.controller.dto.TestResponse;
import com.vladislav.training.platform.content.controller.dto.TopicResponse;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.service.ContentLifecycleQueryService;
import com.vladislav.training.platform.content.service.CourseLifecycleService;
import com.vladislav.training.platform.content.service.MaterialLifecycleService;
import com.vladislav.training.platform.content.service.QuestionLifecycleService;
import com.vladislav.training.platform.content.service.TestLifecycleService;
import com.vladislav.training.platform.content.service.TopicLifecycleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code ContentLifecycleController}.
 */
@RestController
@RequestMapping("/api/v1/expert/content/lifecycle")
public class ContentLifecycleController {

    private final ContentLifecycleQueryService lifecycleQueryService;
    private final CourseLifecycleService courseLifecycleService;
    private final TopicLifecycleService topicLifecycleService;
    private final MaterialLifecycleService materialLifecycleService;
    private final QuestionLifecycleService questionLifecycleService;
    private final TestLifecycleService testLifecycleService;

    public ContentLifecycleController(
        ContentLifecycleQueryService lifecycleQueryService,
        CourseLifecycleService courseLifecycleService,
        TopicLifecycleService topicLifecycleService,
        MaterialLifecycleService materialLifecycleService,
        QuestionLifecycleService questionLifecycleService,
        TestLifecycleService testLifecycleService
    ) {
        this.lifecycleQueryService = lifecycleQueryService;
        this.courseLifecycleService = courseLifecycleService;
        this.topicLifecycleService = topicLifecycleService;
        this.materialLifecycleService = materialLifecycleService;
        this.questionLifecycleService = questionLifecycleService;
        this.testLifecycleService = testLifecycleService;
    }

    @GetMapping("/courses/{id}")
    public CourseResponse findCourse(@PathVariable Long id) {
        return toResponse(lifecycleQueryService.findCourseById(id));
    }

    @GetMapping("/topics/{id}")
    public TopicResponse findTopic(@PathVariable Long id) {
        return toResponse(lifecycleQueryService.findTopicById(id));
    }

    @GetMapping("/materials/{id}")
    public MaterialResponse findMaterial(@PathVariable Long id) {
        return toResponse(lifecycleQueryService.findMaterialById(id));
    }

    @GetMapping("/questions/{id}")
    public QuestionResponse findQuestion(@PathVariable Long id) {
        return toResponse(lifecycleQueryService.findQuestionById(id));
    }

    @GetMapping("/tests/{id}")
    public TestResponse findTest(@PathVariable Long id) {
        return toResponse(lifecycleQueryService.findTestById(id));
    }

    @PostMapping("/courses/{id}/publish")
    public CourseResponse publishCourse(@PathVariable Long id) {
        return toResponse(courseLifecycleService.publish(id));
    }

    @PostMapping("/courses/{id}/archive")
    public CourseResponse archiveCourse(@PathVariable Long id) {
        return toResponse(courseLifecycleService.archive(id));
    }

    @PostMapping("/topics/{id}/publish")
    public TopicResponse publishTopic(@PathVariable Long id) {
        return toResponse(topicLifecycleService.publish(id));
    }

    @PostMapping("/topics/{id}/archive")
    public TopicResponse archiveTopic(@PathVariable Long id) {
        return toResponse(topicLifecycleService.archive(id));
    }

    @PostMapping("/materials/{id}/publish")
    public MaterialResponse publishMaterial(@PathVariable Long id) {
        return toResponse(materialLifecycleService.publish(id));
    }

    @PostMapping("/materials/{id}/archive")
    public MaterialResponse archiveMaterial(@PathVariable Long id) {
        return toResponse(materialLifecycleService.archive(id));
    }

    @PostMapping("/questions/{id}/publish")
    public QuestionResponse publishQuestion(@PathVariable Long id) {
        return toResponse(questionLifecycleService.publish(id));
    }

    @PostMapping("/questions/{id}/archive")
    public QuestionResponse archiveQuestion(@PathVariable Long id) {
        return toResponse(questionLifecycleService.archive(id));
    }

    @PostMapping("/tests/{id}/publish")
    public TestResponse publishTest(@PathVariable Long id) {
        return toResponse(testLifecycleService.publish(id));
    }

    @PostMapping("/tests/{id}/archive")
    public TestResponse archiveTest(@PathVariable Long id) {
        return toResponse(testLifecycleService.archive(id));
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

    private QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
            question.id(),
            question.topicId(),
            question.body(),
            question.questionType(),
            question.status(),
            question.sortOrder(),
            question.createdAt(),
            question.updatedAt()
        );
    }

    private TestResponse toResponse(Test test) {
        return new TestResponse(
            test.id(),
            test.topicId(),
            test.name(),
            test.description(),
            test.testType(),
            test.status(),
            test.thresholdPercent(),
            test.scoringPolicyCode(),
            test.sortOrder(),
            test.createdAt(),
            test.updatedAt()
        );
    }
}
