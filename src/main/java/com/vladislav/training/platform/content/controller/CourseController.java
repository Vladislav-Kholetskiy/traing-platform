package com.vladislav.training.platform.content.controller;

import com.vladislav.training.platform.content.controller.dto.CourseResponse;
import com.vladislav.training.platform.content.controller.dto.SaveCourseRequest;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.service.CourseCommandService;
import com.vladislav.training.platform.content.service.CourseQueryService;
import com.vladislav.training.platform.content.service.CreateCourseCommand;
import com.vladislav.training.platform.content.service.UpdateCourseCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code CourseController}.
 */
@RestController
@RequestMapping("/api/v1/expert/content/courses")
public class CourseController {

    private final CourseCommandService commandService;
    private final CourseQueryService queryService;

    public CourseController(CourseCommandService commandService, CourseQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    public List<CourseResponse> findAll() {
        return queryService.findAllCourses().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findCourseById(id));
    }

    @PostMapping
    public CourseResponse create(@Valid @RequestBody SaveCourseRequest request) {
        Course saved = commandService.createCourse(new CreateCourseCommand(
            request.name(),
            request.description(),
            request.sortOrder()
        ));
        return toResponse(saved);
    }

    @PatchMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody SaveCourseRequest request) {
        Course saved = commandService.updateCourse(id, new UpdateCourseCommand(
            request.name(),
            request.description(),
            request.sortOrder()
        ));
        return toResponse(saved);
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
}
