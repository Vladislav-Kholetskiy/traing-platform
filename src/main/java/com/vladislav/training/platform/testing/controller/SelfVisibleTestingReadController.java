package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestCatalogEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTopicResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestResponse;
import com.vladislav.training.platform.testing.service.SelfVisibleTestCatalogEntryReadModel;
import com.vladislav.training.platform.testing.service.SelfVisibleTopicReadModel;
import com.vladislav.training.platform.testing.service.SelfVisibleTestReadModel;
import com.vladislav.training.platform.testing.service.SelfVisibleTestingReadService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code SelfVisibleTestingReadController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/self-testing/tests")
public class SelfVisibleTestingReadController {

    private final SelfVisibleTestingReadService selfVisibleTestingReadService;

    public SelfVisibleTestingReadController(SelfVisibleTestingReadService selfVisibleTestingReadService) {
        this.selfVisibleTestingReadService = Objects.requireNonNull(
            selfVisibleTestingReadService,
            "selfVisibleTestingReadService must not be null"
        );
    }

    @GetMapping
    public List<SelfVisibleTestCatalogEntryResponse> findSelfVisibleTests() {
        return selfVisibleTestingReadService.findSelfVisibleTests().stream()
            .map(this::toCatalogResponse)
            .toList();
    }

    @GetMapping("/{testId}")
    public SelfVisibleTestResponse findSelfVisibleTestById(@PathVariable @Positive Long testId) {
        return toResponse(selfVisibleTestingReadService.findSelfVisibleTestById(testId));
    }

    @GetMapping("/topics/{topicId}")
    public SelfVisibleTopicResponse findSelfVisibleTopicById(@PathVariable @Positive Long topicId) {
        return toTopicResponse(selfVisibleTestingReadService.findSelfVisibleTopicById(topicId));
    }

    private SelfVisibleTestCatalogEntryResponse toCatalogResponse(SelfVisibleTestCatalogEntryReadModel readModel) {
        return new SelfVisibleTestCatalogEntryResponse(
            readModel.id(),
            readModel.courseId(),
            readModel.courseName(),
            readModel.topicId(),
            readModel.topicName(),
            readModel.name(),
            readModel.description(),
            readModel.testType()
        );
    }

    private SelfVisibleTestResponse toResponse(SelfVisibleTestReadModel readModel) {
        return new SelfVisibleTestResponse(
            readModel.id(),
            readModel.topicId(),
            readModel.name(),
            readModel.description(),
            readModel.testType(),
            readModel.questions().stream()
                .map(question -> new SelfVisibleTestResponse.SelfVisibleQuestionResponse(
                    question.id(),
                    question.body(),
                    question.questionType(),
                    question.displayOrder(),
                    question.weight(),
                    question.answerOptions().stream()
                        .map(answerOption -> new SelfVisibleTestResponse.SelfVisibleAnswerOptionResponse(
                            answerOption.id(),
                            answerOption.body(),
                            answerOption.answerOptionRole(),
                            answerOption.displayOrder()
                        ))
                        .toList()
                ))
                .toList()
        );
    }

    private SelfVisibleTopicResponse toTopicResponse(SelfVisibleTopicReadModel readModel) {
        return new SelfVisibleTopicResponse(
            readModel.topicId(),
            readModel.topicName(),
            readModel.topicDescription(),
            readModel.courseId(),
            readModel.courseName(),
            readModel.materials().stream()
                .map(material -> new SelfVisibleTopicResponse.SelfVisibleMaterialResponse(
                    material.materialId(),
                    material.name(),
                    material.description(),
                    material.body(),
                    material.videoUrl(),
                    material.materialType(),
                    material.sortOrder(),
                    material.updatedAt()
                ))
                .toList()
        );
    }
}
