package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code TestLifecycleServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TestLifecycleServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TestRepository repository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private TopicRepository topicRepository;
    @Mock private ContentPublicationValidationService publicationValidationService;
    @Mock private UtcClock utcClock;

    @Test
    void publishTestRejectsNonDraftState() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        when(repository.findTestById(14L)).thenReturn(test(14L, 24L, ContentStatus.PUBLISHED, false));
        doCallRealMethod().when(support).requireDraft(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.publish(14L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Test must be DRAFT");

        verify(repository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    @Test
    void publishPromotesDraftTestAndRecordsAudit() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(10L, 20L, ContentStatus.DRAFT, false);
        Topic parent = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test published = test(10L, 20L, ContentStatus.PUBLISHED, false);
        when(repository.findTestById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        when(repository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(published);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_PUBLISH), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        com.vladislav.training.platform.content.domain.Test result = service.publish(10L);

        assertThat(result.status()).isEqualTo(ContentStatus.PUBLISHED);
        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TEST, 10L);
        verify(publicationValidationService).validateTestPublishable(10L);
        verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(published), any(AuditContext.class));
    }

    @Test
    void genericPublishStillDoesNotMaterializeActiveFinal() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(13L, 23L, ContentStatus.DRAFT, false);
        Topic parent = new Topic(23L, 33L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(repository.findTestById(13L)).thenReturn(existing);
        when(topicRepository.findTopicById(23L)).thenReturn(parent);
        when(repository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        com.vladislav.training.platform.content.domain.Test result = service.publish(13L);

        assertThat(result.isActiveFinalForTopic()).isFalse();
    }

    @Test
    void publishTestRejectsWhenParentTopicIsNotPublished() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(12L, 22L, ContentStatus.DRAFT, false);
        Topic parent = new Topic(22L, 32L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(repository.findTestById(12L)).thenReturn(existing);
        when(topicRepository.findTopicById(22L)).thenReturn(parent);

        assertThatThrownBy(() -> service.publish(12L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Parent topic must be PUBLISHED");
    }

    @Test
    void archiveTestAllowsWhenPublishedButNotActiveFinal() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(16L, 26L, ContentStatus.PUBLISHED, false);
        com.vladislav.training.platform.content.domain.Test archived = test(16L, 26L, ContentStatus.ARCHIVED, false);
        when(repository.findTestById(16L)).thenReturn(existing);
        when(repository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(archived);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        com.vladislav.training.platform.content.domain.Test result = service.archive(16L);

        assertThat(result.status()).isEqualTo(ContentStatus.ARCHIVED);
        assertThat(result.isActiveFinalForTopic()).isFalse();
        verify(repository).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    @Test
    void archiveTestRejectsNonPublishedState() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        when(repository.findTestById(15L)).thenReturn(test(15L, 25L, ContentStatus.DRAFT, false));
        doCallRealMethod().when(support).requirePublished(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.archive(15L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Test must be PUBLISHED");

        verify(repository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    @Test
    void genericArchiveStillRejectsCurrentActiveFinal() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        when(repository.findTestById(11L)).thenReturn(test(11L, 21L, ContentStatus.PUBLISHED, true));

        assertThatThrownBy(() -> service.archive(11L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Current active final test cannot be archived");

        verify(repository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    private com.vladislav.training.platform.content.domain.Test test(Long testId, Long topicId, ContentStatus status, boolean activeFinal) {
        return new com.vladislav.training.platform.content.domain.Test(testId, topicId, "Test", null, TestType.CONTROL, status, BigDecimal.valueOf(80), "DEFAULT", activeFinal, 0, FIXED_INSTANT, FIXED_INSTANT);
    }
}
