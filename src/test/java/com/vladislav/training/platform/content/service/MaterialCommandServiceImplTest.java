package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code MaterialCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class MaterialCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private MaterialRepository materialRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;

    @Test
    void createMaterialCreatesDraftUnderDraftTopic() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Topic parent = new Topic(20L, 30L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        when(materialRepository.findMaterialsByTopicId(20L)).thenReturn(List.of());
        when(materialRepository.saveMaterial(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Material result = service.createMaterial(new CreateMaterialCommand(20L, "Material", null, null, null, MaterialType.TEXT, 0));

        assertThat(result.topicId()).isEqualTo(20L);
        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        verify(materialRepository).saveMaterial(any(Material.class));
    }

    @Test
    void createMaterialRejectsDuplicateSortOrderWithinTopic() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Topic parent = new Topic(20L, 30L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Material sibling = new Material(41L, 20L, "Existing", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        when(materialRepository.findMaterialsByTopicId(20L)).thenReturn(List.of(sibling));

        assertThatThrownBy(() -> service.createMaterial(new CreateMaterialCommand(20L, "Material", null, null, null, MaterialType.TEXT, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("sortOrder")
            .hasMessageContaining("unique");

        verify(materialRepository).findMaterialsByTopicId(20L);
        verify(materialRepository, never()).saveMaterial(any(Material.class));
    }

    @Test
    void publishedTopicStillAllowsNewDraftMaterial() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Topic parent = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        Material created = new Material(40L, 20L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        when(materialRepository.findMaterialsByTopicId(20L)).thenReturn(List.of());
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(created);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_CREATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Material result = service.createMaterial(new CreateMaterialCommand(20L, "Material", null, null, null, MaterialType.TEXT, 0));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.MATERIAL,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        verify(support).validateParentForChildRootAuthoring(ContentStatus.PUBLISHED, "Topic");
    }

    @Test
    void createMaterialRejectsArchivedParentTopic() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Topic parent = new Topic(20L, 30L, "Topic", null, ContentStatus.ARCHIVED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        doCallRealMethod().when(support).validateParentForChildRootAuthoring(ContentStatus.ARCHIVED, "Topic");

        assertThatThrownBy(() -> service.createMaterial(new CreateMaterialCommand(20L, "Material", null, null, null, MaterialType.TEXT, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic ARCHIVED parent cannot accept child-root authoring");

        verify(materialRepository, never()).saveMaterial(any(Material.class));
    }

    @Test
    void createMaterialRejectsMissingParentTopic() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        when(topicRepository.findTopicById(20L)).thenThrow(new NotFoundException("Topic not found"));

        assertThatThrownBy(() -> service.createMaterial(new CreateMaterialCommand(20L, "Material", null, null, null, MaterialType.TEXT, 0)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Topic");

        verify(materialRepository, never()).saveMaterial(any(Material.class));
    }

    @Test
    void updateMaterialRejectsDuplicateSortOrderWithinTopic() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Material existing = new Material(10L, 20L, "Old", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Material sibling = new Material(11L, 20L, "Other", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(materialRepository.findMaterialsByTopicId(20L)).thenReturn(List.of(existing, sibling));

        assertThatThrownBy(() -> service.updateMaterial(10L, new UpdateMaterialCommand("New", null, null, null, MaterialType.PDF, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("sortOrder")
            .hasMessageContaining("unique");

        verify(materialRepository).findMaterialsByTopicId(20L);
        verify(materialRepository, never()).saveMaterial(any(Material.class));
    }

    @Test
    void updateMaterialAllowsCurrentSortOrderWithoutSelfConflict() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Material existing = new Material(10L, 20L, "Old", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Material sibling = new Material(11L, 20L, "Other", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        Material updated = new Material(10L, 20L, "New", null, null, null, MaterialType.PDF, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(materialRepository.findMaterialsByTopicId(20L)).thenReturn(List.of(existing, sibling));
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(updated);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Material result = service.updateMaterial(10L, new UpdateMaterialCommand("New", null, null, null, MaterialType.PDF, 0));

        assertThat(result.sortOrder()).isEqualTo(0);
        verify(materialRepository).findMaterialsByTopicId(20L);
        verify(materialRepository).saveMaterial(any(Material.class));
    }

    @Test
    void updateMaterialUpdatesOnlyDraftMaterial() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Material existing = new Material(10L, 20L, "Old", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(materialRepository.findMaterialsByTopicId(20L)).thenReturn(List.of(existing));
        when(materialRepository.saveMaterial(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Material result = service.updateMaterial(10L, new UpdateMaterialCommand("New", null, null, null, MaterialType.PDF, 1));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.materialType()).isEqualTo(MaterialType.PDF);
        assertThat(result.sortOrder()).isEqualTo(1);
        verify(materialRepository).saveMaterial(any(Material.class));
    }

    @Test
    void updateMaterialRejectsNonDraftMaterial() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Material existing = new Material(10L, 20L, "Old", null, null, null, MaterialType.TEXT, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Material");

        assertThatThrownBy(() -> service.updateMaterial(10L, new UpdateMaterialCommand("New", null, null, null, MaterialType.PDF, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Material must be DRAFT");

        verify(materialRepository, never()).saveMaterial(any(Material.class));
    }

    @Test
    void updateMaterialUsesSingleOwnerReadAndClockPolicy() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Material existing = new Material(10L, 20L, "Old", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Material updated = new Material(10L, 20L, "New", null, null, null, MaterialType.PDF, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(updated);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Material result = service.updateMaterial(10L, new UpdateMaterialCommand("New", null, null, null, MaterialType.PDF, 1));

        assertThat(result.materialType()).isEqualTo(MaterialType.PDF);
        verify(materialRepository).findMaterialById(10L);
        verify(materialRepository).saveMaterial(any(Material.class));
    }
}
