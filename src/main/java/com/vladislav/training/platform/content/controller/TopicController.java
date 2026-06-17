package com.vladislav.training.platform.content.controller;

import com.vladislav.training.platform.content.controller.dto.CreateTopicRequest;
import com.vladislav.training.platform.content.controller.dto.TopicResponse;
import com.vladislav.training.platform.content.controller.dto.UpdateTopicRequest;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.service.CreateTopicCommand;
import com.vladislav.training.platform.content.service.TopicCommandService;
import com.vladislav.training.platform.content.service.TopicQueryService;
import com.vladislav.training.platform.content.service.UpdateTopicCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code TopicController}.
 */
@RestController
@RequestMapping("/api/v1/expert/content/topics")
public class TopicController {

    private final TopicCommandService commandService;
    private final TopicQueryService queryService;

    public TopicController(TopicCommandService commandService, TopicQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping("/{id}")
    public TopicResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findTopicById(id));
    }

    @GetMapping(params = "courseId")
    public List<TopicResponse> findByCourseId(@RequestParam Long courseId) {
        return queryService.findTopicsByCourseId(courseId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public TopicResponse create(@Valid @RequestBody CreateTopicRequest request) {
        Topic saved = commandService.createTopic(new CreateTopicCommand(
            request.courseId(),
            request.name(),
            request.description(),
            request.sortOrder()
        ));
        return toResponse(saved);
    }

    @PatchMapping("/{id}")
    public TopicResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTopicRequest request) {
        Topic saved = commandService.updateTopic(id, new UpdateTopicCommand(
            request.name(),
            request.description(),
            request.sortOrder()
        ));
        return toResponse(saved);
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
}
