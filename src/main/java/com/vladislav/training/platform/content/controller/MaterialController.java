package com.vladislav.training.platform.content.controller;

import com.vladislav.training.platform.content.controller.dto.CreateMaterialRequest;
import com.vladislav.training.platform.content.controller.dto.MaterialResponse;
import com.vladislav.training.platform.content.controller.dto.UpdateMaterialRequest;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.service.CreateMaterialCommand;
import com.vladislav.training.platform.content.service.MaterialCommandService;
import com.vladislav.training.platform.content.service.MaterialQueryService;
import com.vladislav.training.platform.content.service.UpdateMaterialCommand;
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
 * Контроллер {@code MaterialController}.
 */
@RestController
@RequestMapping("/api/v1/expert/content/materials")
public class MaterialController {

    private final MaterialCommandService commandService;
    private final MaterialQueryService queryService;

    public MaterialController(MaterialCommandService commandService, MaterialQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping(params = "topicId")
    public List<MaterialResponse> findByTopicId(@RequestParam Long topicId) {
        return queryService.findMaterialsByTopicId(topicId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MaterialResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findMaterialById(id));
    }

    @PostMapping
    public MaterialResponse create(@Valid @RequestBody CreateMaterialRequest request) {
        Material saved = commandService.createMaterial(new CreateMaterialCommand(
            request.topicId(),
            request.name(),
            request.description(),
            request.body(),
            request.videoUrl(),
            request.materialType(),
            request.sortOrder()
        ));
        return toResponse(saved);
    }

    @PatchMapping("/{id}")
    public MaterialResponse update(@PathVariable Long id, @Valid @RequestBody UpdateMaterialRequest request) {
        Material saved = commandService.updateMaterial(id, new UpdateMaterialCommand(
            request.name(),
            request.description(),
            request.body(),
            request.videoUrl(),
            request.materialType(),
            request.sortOrder()
        ));
        return toResponse(saved);
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
