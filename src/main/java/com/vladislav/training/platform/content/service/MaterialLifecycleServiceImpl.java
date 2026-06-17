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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code MaterialLifecycleServiceImpl}.
 */
@Service
@Transactional
public class MaterialLifecycleServiceImpl implements MaterialLifecycleService {

    private final MaterialRepository repository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final TopicRepository topicRepository;
    private final UtcClock utcClock;

    public MaterialLifecycleServiceImpl(
            MaterialRepository repository,
            ContentCommandSupport support,
            CriticalCommandAuditSupport auditSupport,
            TopicRepository topicRepository,
            UtcClock utcClock
    ) {
        this.repository = repository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.topicRepository = topicRepository;
        this.utcClock = utcClock;
    }

    @Override
    public Material publish(Long id) {
        Material existing = repository.findMaterialById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.MATERIAL, id);
        support.requireDraft(existing.status(), "Material");
        if (topicRepository.findTopicById(existing.topicId()).status() != ContentStatus.PUBLISHED) {
            throw new ConflictException("Parent topic must be PUBLISHED");
        }
        var now = utcClock.now();
        Material saved = repository.saveMaterial(new Material(
                existing.id(),
                existing.topicId(),
                existing.name(),
                existing.description(),
                existing.body(),
                existing.videoUrl(),
                existing.materialType(),
                ContentStatus.PUBLISHED,
                existing.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_MATERIAL_PUBLISHED),
                "material",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_PUBLISH,
                        Map.of("entityType", "material")
                )
        );
        return saved;
    }

    @Override
    public Material archive(Long id) {
        Material existing = repository.findMaterialById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.MATERIAL, id);
        support.requirePublished(existing.status(), "Material");
        var now = utcClock.now();
        Material saved = repository.saveMaterial(new Material(
                existing.id(),
                existing.topicId(),
                existing.name(),
                existing.description(),
                existing.body(),
                existing.videoUrl(),
                existing.materialType(),
                ContentStatus.ARCHIVED,
                existing.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_MATERIAL_ARCHIVED),
                "material",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_ARCHIVE,
                        Map.of("entityType", "material")
                )
        );
        return saved;
    }
}
