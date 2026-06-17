package com.vladislav.training.platform.content.controller;

import com.vladislav.training.platform.content.controller.dto.TestResponse;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.service.TestQueryService;
import com.vladislav.training.platform.content.service.TopicFinalControlService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code TopicFinalControlController}.
 */
@RestController
@RequestMapping("/api/v1/expert/content/topics")
public class TopicFinalControlController {

    private final TopicFinalControlService topicFinalControlService;
    private final TestQueryService testQueryService;

    public TopicFinalControlController(
        TopicFinalControlService topicFinalControlService,
        TestQueryService testQueryService
    ) {
        this.topicFinalControlService = topicFinalControlService;
        this.testQueryService = testQueryService;
    }

    @PostMapping("/{topicId}/active-final-tests/{testId}/assign")
    public void assign(@PathVariable Long topicId, @PathVariable Long testId) {
        topicFinalControlService.assignActiveFinalTest(topicId, testId);
    }

    @PostMapping("/{topicId}/active-final-tests/{testId}/replace")
    public void replace(@PathVariable Long topicId, @PathVariable Long testId) {
        topicFinalControlService.replaceActiveFinalTest(topicId, testId);
    }

    @DeleteMapping("/{topicId}/active-final-test")
    public void clear(@PathVariable Long topicId) {
        topicFinalControlService.clearActiveFinalTest(topicId);
    }

    @GetMapping("/{topicId}/active-final-test")
    public ResponseEntity<TestResponse> find(@PathVariable Long topicId) {
        return testQueryService.findActiveFinalTestByTopicId(topicId)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{topicId}/tests", params = "eligibleForFinalControl=true")
    public List<TestResponse> findEligibleFinalControlTests(@PathVariable Long topicId) {
        return testQueryService.findEligibleFinalControlTestsByTopicId(topicId).stream()
            .map(this::toResponse)
            .toList();
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
