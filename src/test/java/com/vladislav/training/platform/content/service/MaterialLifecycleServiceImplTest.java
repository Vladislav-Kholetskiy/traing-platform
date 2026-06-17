package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code MaterialLifecycleServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class MaterialLifecycleServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private MaterialRepository repository;
    @Mock private ContentCommandSupport support;
    @Mock private com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport auditSupport;
    @Mock private TopicRepository topicRepository;
    @Mock private UtcClock utcClock;

    @Test
    void publishMaterialAllowsOnlyDraft() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        Material existing = material(10L, 20L, ContentStatus.DRAFT);
        Material published = material(10L, 20L, ContentStatus.PUBLISHED);
        when(repository.findMaterialById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, ContentStatus.PUBLISHED));
        when(repository.saveMaterial(org.mockito.ArgumentMatchers.any())).thenReturn(published);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Material result = service.publish(10L);

        assertThat(result.status()).isEqualTo(ContentStatus.PUBLISHED);
        verify(repository).saveMaterial(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishMaterialRejectsNonDraftState() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        when(repository.findMaterialById(11L)).thenReturn(new Material(11L, 21L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));
        doCallRealMethod().when(support).requireDraft(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.publish(11L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Material must be DRAFT");

        verify(repository, never()).saveMaterial(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveMaterialAllowsOnlyPublished() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        Material existing = material(12L, 22L, ContentStatus.PUBLISHED);
        Material archived = material(12L, 22L, ContentStatus.ARCHIVED);
        when(repository.findMaterialById(12L)).thenReturn(existing);
        when(repository.saveMaterial(org.mockito.ArgumentMatchers.any())).thenReturn(archived);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Material result = service.archive(12L);

        assertThat(result.status()).isEqualTo(ContentStatus.ARCHIVED);
        verify(repository).saveMaterial(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveMaterialRejectsNonPublishedState() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        when(repository.findMaterialById(12L)).thenReturn(new Material(12L, 22L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));
        doCallRealMethod().when(support).requirePublished(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.archive(12L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Material must be PUBLISHED");

        verify(repository, never()).saveMaterial(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishMaterialRejectsWhenParentTopicIsNotPublished() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        when(repository.findMaterialById(10L)).thenReturn(new Material(10L, 20L, "Material", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));
        when(topicRepository.findTopicById(20L)).thenReturn(new Topic(20L, 30L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Parent topic must be PUBLISHED");

        verify(repository, never()).saveMaterial(org.mockito.ArgumentMatchers.any());
    }

    private Material material(Long id, Long topicId, ContentStatus status) {
        return new Material(id, topicId, "Material", null, null, null, MaterialType.TEXT, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private Topic topic(Long id, ContentStatus status) {
        return new Topic(id, 30L, "Topic", null, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }
}
