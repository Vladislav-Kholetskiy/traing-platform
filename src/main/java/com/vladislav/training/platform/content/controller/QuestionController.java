package com.vladislav.training.platform.content.controller;

import com.vladislav.training.platform.content.controller.dto.AnswerOptionResponse;
import com.vladislav.training.platform.content.controller.dto.CreateQuestionRequest;
import com.vladislav.training.platform.content.controller.dto.QuestionResponse;
import com.vladislav.training.platform.content.controller.dto.SaveAnswerOptionRequest;
import com.vladislav.training.platform.content.controller.dto.UpdateQuestionRequest;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.service.CreateAnswerOptionCommand;
import com.vladislav.training.platform.content.service.CreateQuestionCommand;
import com.vladislav.training.platform.content.service.QuestionCommandService;
import com.vladislav.training.platform.content.service.QuestionQueryService;
import com.vladislav.training.platform.content.service.UpdateAnswerOptionCommand;
import com.vladislav.training.platform.content.service.UpdateQuestionCommand;
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
 * Контроллер {@code QuestionController}.
 */
@RestController
@RequestMapping("/api/v1/expert/content/questions")
public class QuestionController {

    private final QuestionCommandService commandService;
    private final QuestionQueryService queryService;

    public QuestionController(QuestionCommandService commandService, QuestionQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping(params = "topicId")
    public List<QuestionResponse> findByTopicId(@RequestParam Long topicId) {
        return queryService.findQuestionsByTopicId(topicId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public QuestionResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findQuestionById(id));
    }

    @GetMapping("/{questionId}/answer-options")
    public List<AnswerOptionResponse> findAnswerOptions(@PathVariable Long questionId) {
        return queryService.findAnswerOptionsByQuestionId(questionId).stream().map(this::toAnswerOptionResponse).toList();
    }

    @PostMapping
    public QuestionResponse create(@Valid @RequestBody CreateQuestionRequest request) {
        Question saved = commandService.createQuestion(new CreateQuestionCommand(
            request.topicId(),
            request.body(),
            request.questionType(),
            request.sortOrder()
        ));
        return toResponse(saved);
    }

    @PatchMapping("/{id}")
    public QuestionResponse update(@PathVariable Long id, @Valid @RequestBody UpdateQuestionRequest request) {
        Question saved = commandService.updateQuestion(id, new UpdateQuestionCommand(
            request.body(),
            request.questionType(),
            request.sortOrder()
        ));
        return toResponse(saved);
    }

    @PostMapping("/{questionId}/answer-options")
    public AnswerOptionResponse createAnswerOption(
        @PathVariable Long questionId,
        @Valid @RequestBody SaveAnswerOptionRequest request
    ) {
        AnswerOption saved = commandService.createAnswerOption(questionId, new CreateAnswerOptionCommand(
            request.body(),
            request.answerOptionRole(),
            request.isCorrect(),
            request.displayOrder(),
            request.pairingKey(),
            request.canonicalOrderPosition()
        ));
        return toAnswerOptionResponse(saved);
    }

    @PatchMapping("/{questionId}/answer-options/{id}")
    public AnswerOptionResponse updateAnswerOption(
        @PathVariable Long questionId,
        @PathVariable Long id,
        @Valid @RequestBody SaveAnswerOptionRequest request
    ) {
        AnswerOption saved = commandService.updateAnswerOption(questionId, id, new UpdateAnswerOptionCommand(
            request.body(),
            request.answerOptionRole(),
            request.isCorrect(),
            request.displayOrder(),
            request.pairingKey(),
            request.canonicalOrderPosition()
        ));
        return toAnswerOptionResponse(saved);
    }

    @DeleteMapping("/{questionId}/answer-options/{id}")
    public void deleteAnswerOption(@PathVariable Long questionId, @PathVariable Long id) {
        commandService.deleteAnswerOption(questionId, id);
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

    private AnswerOptionResponse toAnswerOptionResponse(AnswerOption answerOption) {
        return new AnswerOptionResponse(
            answerOption.id(),
            answerOption.questionId(),
            answerOption.body(),
            answerOption.answerOptionRole(),
            answerOption.isCorrect(),
            answerOption.displayOrder(),
            answerOption.pairingKey(),
            answerOption.canonicalOrderPosition(),
            answerOption.createdAt(),
            answerOption.updatedAt()
        );
    }
}
