package com.vladislav.training.platform.content.controller;

import com.vladislav.training.platform.content.controller.dto.CreateTestRequest;
import com.vladislav.training.platform.content.controller.dto.SaveTestQuestionRequest;
import com.vladislav.training.platform.content.controller.dto.TestQuestionResponse;
import com.vladislav.training.platform.content.controller.dto.TestResponse;
import com.vladislav.training.platform.content.controller.dto.UpdateTestRequest;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.service.CreateTestCommand;
import com.vladislav.training.platform.content.service.CreateTestQuestionCommand;
import com.vladislav.training.platform.content.service.TestCommandService;
import com.vladislav.training.platform.content.service.TestQueryService;
import com.vladislav.training.platform.content.service.UpdateTestCommand;
import com.vladislav.training.platform.content.service.UpdateTestQuestionCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code TestController}.
 */
@RestController
@RequestMapping("/api/v1/expert/content/tests")
public class TestController {

    private final TestCommandService commandService;
    private final TestQueryService queryService;

    public TestController(TestCommandService commandService, TestQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping(params = "topicId")
    public List<TestResponse> findByTopicId(@RequestParam Long topicId) {
        return queryService.findTestsByTopicId(topicId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TestResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findTestById(id));
    }

    @GetMapping("/{testId}/questions")
    public List<TestQuestionResponse> findTestQuestions(@PathVariable Long testId) {
        return queryService.findTestQuestionsByTestId(testId).stream().map(this::toTestQuestionResponse).toList();
    }

    @PostMapping
    public TestResponse create(@Valid @RequestBody CreateTestRequest request) {
        Test saved = commandService.createTest(new CreateTestCommand(
            request.topicId(),
            request.name(),
            request.description(),
            request.testType(),
            request.thresholdPercent(),
            request.scoringPolicyCode(),
            request.sortOrder()
        ));
        return toResponse(saved);
    }

    @PatchMapping("/{id}")
    public TestResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTestRequest request) {
        Test saved = commandService.updateTest(id, new UpdateTestCommand(
            request.name(),
            request.description(),
            request.testType(),
            request.thresholdPercent(),
            request.scoringPolicyCode(),
            request.sortOrder()
        ));
        return toResponse(saved);
    }

    @PostMapping("/{testId}/questions")
    public TestQuestionResponse createTestQuestion(
        @PathVariable Long testId,
        @Valid @RequestBody SaveTestQuestionRequest request
    ) {
        TestQuestion saved = commandService.createTestQuestion(testId, new CreateTestQuestionCommand(
            request.questionId(),
            request.displayOrder(),
            request.weight()
        ));
        return toTestQuestionResponse(saved);
    }

    @PatchMapping("/{testId}/questions/{id}")
    public TestQuestionResponse updateTestQuestion(
        @PathVariable Long testId,
        @PathVariable Long id,
        @Valid @RequestBody SaveTestQuestionRequest request
    ) {
        TestQuestion saved = commandService.updateTestQuestion(testId, id, new UpdateTestQuestionCommand(
            request.questionId(),
            request.displayOrder(),
            request.weight()
        ));
        return toTestQuestionResponse(saved);
    }

    @DeleteMapping("/{testId}/questions/{id}")
    public void deleteTestQuestion(@PathVariable Long testId, @PathVariable Long id) {
        commandService.deleteTestQuestion(testId, id);
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

    private TestQuestionResponse toTestQuestionResponse(TestQuestion testQuestion) {
        return new TestQuestionResponse(
            testQuestion.id(),
            testQuestion.testId(),
            testQuestion.questionId(),
            testQuestion.displayOrder(),
            testQuestion.weight(),
            testQuestion.createdAt(),
            testQuestion.updatedAt()
        );
    }
}
