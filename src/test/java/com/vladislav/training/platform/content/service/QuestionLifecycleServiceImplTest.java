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
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code QuestionLifecycleServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class QuestionLifecycleServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private QuestionRepository repository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private TopicRepository topicRepository;
    @Mock private TestQuestionRepository testQuestionRepository;
    @Mock private ContentPublicationValidationService publicationValidationService;
    @Mock private UtcClock utcClock;

    @Test
    void publishQuestionAllowsOnlyDraft() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        Question existing = question(10L, ContentStatus.DRAFT);
        Question published = question(10L, ContentStatus.PUBLISHED);
        when(repository.findQuestionById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));
        when(repository.saveQuestion(any(Question.class))).thenReturn(published);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Question result = service.publish(10L);

        assertThat(result.status()).isEqualTo(ContentStatus.PUBLISHED);
        verify(publicationValidationService).validateQuestionPublishable(10L);
        verify(repository).saveQuestion(any(Question.class));
    }

    @Test
    void publishQuestionRejectsNonDraftState() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        when(repository.findQuestionById(12L)).thenReturn(question(12L, ContentStatus.PUBLISHED));
        doCallRealMethod().when(support).requireDraft(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.publish(12L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Question must be DRAFT");

        verify(repository, never()).saveQuestion(any(Question.class));
    }

    @Test
    void archiveQuestionAllowsWhenOnlyDraftOrNoTestUsageExists() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        Question existing = question(15L, ContentStatus.PUBLISHED);
        Question archived = question(15L, ContentStatus.ARCHIVED);
        when(repository.findQuestionById(15L)).thenReturn(existing);
        when(testQuestionRepository.existsPublishedTestUsingQuestion(15L)).thenReturn(false);
        when(repository.saveQuestion(any(Question.class))).thenReturn(archived);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Question result = service.archive(15L);

        assertThat(result.status()).isEqualTo(ContentStatus.ARCHIVED);
        verify(testQuestionRepository).existsPublishedTestUsingQuestion(15L);
        verify(repository).saveQuestion(any(Question.class));
    }

    @Test
    void archiveQuestionRejectsNonPublishedState() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        when(repository.findQuestionById(13L)).thenReturn(question(13L, ContentStatus.DRAFT));
        doCallRealMethod().when(support).requirePublished(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.archive(13L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Question must be PUBLISHED");

        verify(repository, never()).saveQuestion(any(Question.class));
    }

    @Test
    void archiveQuestionRejectsWhenUsedInPublishedTestComposition() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        when(repository.findQuestionById(11L)).thenReturn(question(11L, ContentStatus.PUBLISHED));
        when(testQuestionRepository.existsPublishedTestUsingQuestion(11L)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(11L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("used by a PUBLISHED test");

        verify(testQuestionRepository).existsPublishedTestUsingQuestion(11L);
        verify(repository, never()).saveQuestion(any(Question.class));
    }

    @Test
    void publishQuestionRejectsWhenParentTopicIsNotPublished() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        when(repository.findQuestionById(14L)).thenReturn(question(14L, ContentStatus.DRAFT));
        when(topicRepository.findTopicById(20L)).thenReturn(new Topic(20L, 30L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        assertThatThrownBy(() -> service.publish(14L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Parent topic must be PUBLISHED");

        verify(publicationValidationService, never()).validateQuestionPublishable(14L);
        verify(repository, never()).saveQuestion(any(Question.class));
    }

    @Test
    void publishDelegatesToPublicationGateAndAdmission() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            repository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        Question existing = question(10L, ContentStatus.DRAFT);
        Topic parent = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        Question published = question(10L, ContentStatus.PUBLISHED);
        when(repository.findQuestionById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        when(repository.saveQuestion(any(Question.class))).thenReturn(published);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_PUBLISH), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.QUESTION, 10L);
        verify(publicationValidationService).validateQuestionPublishable(10L);
    }

    private Question question(Long questionId, ContentStatus status) {
        return new Question(questionId, 20L, "Body", QuestionType.SINGLE_CHOICE, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }
}
