package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code MaterialCommandServiceImpl}.
 */
@Service
@Transactional
public class MaterialCommandServiceImpl implements MaterialCommandService {

    private final MaterialRepository materialRepository;
    private final TopicRepository topicRepository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final UtcClock utcClock;

    public MaterialCommandServiceImpl(
        MaterialRepository materialRepository,
        TopicRepository topicRepository,
        ContentCommandSupport support,
        CriticalCommandAuditSupport auditSupport,
        UtcClock utcClock
    ) {
        this.materialRepository = materialRepository;
        this.topicRepository = topicRepository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.utcClock = utcClock;
    }

    @Override
    public Material createMaterial(CreateMaterialCommand command) {
        support.requireNonNull("topicId", command.topicId());
        support.requireNotBlank("name", command.name());
        support.requireNonNull("materialType", command.materialType());
        support.validateNonNegative("sortOrder", command.sortOrder());
        support.checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.MATERIAL,
            command.topicId(),
            CapabilityTargetEntityType.TOPIC
        );
        var parent = topicRepository.findTopicById(command.topicId());
        support.validateParentForChildRootAuthoring(parent.status(), "Topic");
        validateMaterialSortOrderAvailable(command.topicId(), command.sortOrder(), null);
        var now = utcClock.now();
        Material saved = materialRepository.saveMaterial(new Material(
            null,
            command.topicId(),
            command.name(),
            command.description(),
            command.body(),
            command.videoUrl(),
            command.materialType(),
            ContentStatus.DRAFT,
            command.sortOrder(),
            now,
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_MATERIAL_DRAFT_CREATED),
            "material",
            saved.id(),
            null,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_DRAFT_CREATE.code(),
                Map.of("entityType", "material", "topicId", saved.topicId())
            )
        );
        return saved;
    }

    @Override
    public Material updateMaterial(Long materialId, UpdateMaterialCommand command) {
        var existing = materialRepository.findMaterialById(materialId);
        support.checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.MATERIAL,
            materialId,
            existing.topicId(),
            CapabilityTargetEntityType.TOPIC
        );
        support.requireDraft(existing.status(), "Material");
        support.requireNotBlank("name", command.name());
        support.requireNonNull("materialType", command.materialType());
        support.validateNonNegative("sortOrder", command.sortOrder());
        validateMaterialSortOrderAvailable(existing.topicId(), command.sortOrder(), existing.id());
        var now = utcClock.now();
        Material saved = materialRepository.saveMaterial(new Material(
            existing.id(),
            existing.topicId(),
            command.name(),
            command.description(),
            command.body(),
            command.videoUrl(),
            command.materialType(),
            existing.status(),
            command.sortOrder(),
            existing.createdAt(),
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_MATERIAL_DRAFT_UPDATED),
            "material",
            saved.id(),
            existing,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(),
                Map.of("entityType", "material", "topicId", saved.topicId())
            )
        );
        return saved;
    }

    private void validateMaterialSortOrderAvailable(Long topicId, int sortOrder, Long currentMaterialId) {
        boolean conflict = materialRepository.findMaterialsByTopicId(topicId).stream()
            .anyMatch(material -> material.sortOrder() == sortOrder && !Objects.equals(material.id(), currentMaterialId));
        if (conflict) {
            throw new ConflictException("Material sortOrder must be unique within topic");
        }
    }
}
