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
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code QuestionCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class QuestionCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerOptionRepository answerOptionRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;

    @Test
    void createQuestionCreatesDraftUnderDraftTopic() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Topic parent = new Topic(30L, 40L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(30L)).thenReturn(parent);
        when(questionRepository.saveQuestion(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Question result = service.createQuestion(new CreateQuestionCommand(30L, "Q", QuestionType.SINGLE_CHOICE, 0));

        assertThat(result.topicId()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        verify(questionRepository).saveQuestion(any(Question.class));
    }

    @Test
    void publishedTopicStillAllowsNewDraftQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Topic parent = new Topic(30L, 40L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        Question created = new Question(20L, 30L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(30L)).thenReturn(parent);
        when(questionRepository.saveQuestion(any(Question.class))).thenReturn(created);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_CREATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Question result = service.createQuestion(new CreateQuestionCommand(30L, "Q", QuestionType.SINGLE_CHOICE, 0));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.QUESTION,
            30L,
            CapabilityTargetEntityType.TOPIC
        );
        verify(support).validateParentForChildRootAuthoring(ContentStatus.PUBLISHED, "Topic");
    }

    @Test
    void createQuestionRejectsArchivedParentTopic() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Topic parent = new Topic(30L, 40L, "Topic", null, ContentStatus.ARCHIVED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(30L)).thenReturn(parent);
        doCallRealMethod().when(support).validateParentForChildRootAuthoring(ContentStatus.ARCHIVED, "Topic");

        assertThatThrownBy(() -> service.createQuestion(new CreateQuestionCommand(30L, "Q", QuestionType.SINGLE_CHOICE, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic ARCHIVED parent cannot accept child-root authoring");

        verify(questionRepository, never()).saveQuestion(any(Question.class));
    }

    @Test
    void updateQuestionUpdatesOnlyDraftQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Question existing = new Question(20L, 30L, "Old", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(questionRepository.findQuestionById(20L)).thenReturn(existing);
        when(questionRepository.saveQuestion(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Question result = service.updateQuestion(20L, new UpdateQuestionCommand("New", QuestionType.SINGLE_CHOICE, 1));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.body()).isEqualTo("New");
        assertThat(result.sortOrder()).isEqualTo(1);
        verify(questionRepository).saveQuestion(any(Question.class));
    }

    @Test
    void updateQuestionRejectsNonDraftQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Question existing = new Question(20L, 30L, "Old", QuestionType.SINGLE_CHOICE, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(questionRepository.findQuestionById(20L)).thenReturn(existing);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Question");

        assertThatThrownBy(() -> service.updateQuestion(20L, new UpdateQuestionCommand("New", QuestionType.SINGLE_CHOICE, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Question must be DRAFT");

        verify(questionRepository, never()).saveQuestion(any(Question.class));
    }

    @Test
    void createAnswerOptionCreatesCompositionOnlyForDraftQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Question parent = new Question(20L, 30L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(questionRepository.findQuestionById(20L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(20L)).thenReturn(List.of());
        when(answerOptionRepository.saveAnswerOption(any(AnswerOption.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        AnswerOption result = service.createAnswerOption(20L, new CreateAnswerOptionCommand(
            "New",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null
        ));

        assertThat(result.questionId()).isEqualTo(20L);
        assertThat(result.displayOrder()).isEqualTo(0);
        verify(answerOptionRepository).saveAnswerOption(any(AnswerOption.class));
        verify(questionRepository, never()).saveQuestion(any(Question.class));
    }

    @Test
    void createAnswerOptionRejectsDuplicateDisplayOrderWithinQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Question parent = new Question(20L, 30L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        AnswerOption sibling = new AnswerOption(12L, 20L, "Existing", AnswerOptionRole.CHOICE_OPTION, Boolean.TRUE, 1, null, null, FIXED_INSTANT, FIXED_INSTANT);
        when(questionRepository.findQuestionById(20L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(20L)).thenReturn(List.of(sibling));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatThrownBy(() -> service.createAnswerOption(20L, new CreateAnswerOptionCommand(
            "New",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.FALSE,
            1,
            null,
            null
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("displayOrder")
            .hasMessageContaining("unique");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            20L,
            20L,
            CapabilityTargetEntityType.QUESTION
        );
        verify(answerOptionRepository, never()).saveAnswerOption(any(AnswerOption.class));
    }

    @Test
    void createAnswerOptionRejectsNonDraftQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Question parent = new Question(20L, 30L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(questionRepository.findQuestionById(20L)).thenReturn(parent);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Question");

        assertThatThrownBy(() -> service.createAnswerOption(20L, new CreateAnswerOptionCommand(
            "New",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Question must be DRAFT");

        verify(answerOptionRepository, never()).saveAnswerOption(any(AnswerOption.class));
    }

    @Test
    void updateAnswerOptionRunsAdmissionOnParentQuestionContour() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = new AnswerOption(10L, 20L, "Old", AnswerOptionRole.CHOICE_OPTION, Boolean.FALSE, 0, null, null, FIXED_INSTANT, FIXED_INSTANT);
        Question parent = new Question(20L, 30L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        AnswerOption updated = new AnswerOption(10L, 20L, "New", AnswerOptionRole.CHOICE_OPTION, Boolean.TRUE, 0, null, null, FIXED_INSTANT, FIXED_INSTANT);
        when(answerOptionRepository.findAnswerOptionById(10L)).thenReturn(existing);
        when(questionRepository.findQuestionById(20L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(20L)).thenReturn(List.of(
            existing,
            new AnswerOption(12L, 20L, "Other", AnswerOptionRole.CHOICE_OPTION, Boolean.FALSE, 1, null, null, FIXED_INSTANT, FIXED_INSTANT)
        ));
        when(answerOptionRepository.saveAnswerOption(any(AnswerOption.class))).thenReturn(updated);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateAnswerOption(20L, 10L, new UpdateAnswerOptionCommand(
            "New",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null
        ));

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            20L,
            20L,
            CapabilityTargetEntityType.QUESTION
        );
        verify(answerOptionRepository).saveAnswerOption(any(AnswerOption.class));
        verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(20L), eq(existing), eq(updated), any());
    }

    @Test
    void updateAnswerOptionRejectsDuplicateDisplayOrderWithinQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = new AnswerOption(10L, 20L, "Old", AnswerOptionRole.CHOICE_OPTION, Boolean.FALSE, 0, null, null, FIXED_INSTANT, FIXED_INSTANT);
        AnswerOption sibling = new AnswerOption(11L, 20L, "Other", AnswerOptionRole.CHOICE_OPTION, Boolean.TRUE, 1, null, null, FIXED_INSTANT, FIXED_INSTANT);
        Question parent = new Question(20L, 30L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(answerOptionRepository.findAnswerOptionById(10L)).thenReturn(existing);
        when(questionRepository.findQuestionById(20L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(20L)).thenReturn(List.of(existing, sibling));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatThrownBy(() -> service.updateAnswerOption(20L, 10L, new UpdateAnswerOptionCommand(
            "New",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            1,
            null,
            null
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("displayOrder")
            .hasMessageContaining("unique");

        verify(answerOptionRepository, never()).saveAnswerOption(any(AnswerOption.class));
    }

    @Test
    void updateAnswerOptionAllowsSameDisplayOrderForCurrentOption() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = new AnswerOption(10L, 20L, "Old", AnswerOptionRole.CHOICE_OPTION, Boolean.FALSE, 0, null, null, FIXED_INSTANT, FIXED_INSTANT);
        AnswerOption sibling = new AnswerOption(11L, 20L, "Other", AnswerOptionRole.CHOICE_OPTION, Boolean.TRUE, 1, null, null, FIXED_INSTANT, FIXED_INSTANT);
        Question parent = new Question(20L, 30L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        AnswerOption updated = new AnswerOption(10L, 20L, "New", AnswerOptionRole.CHOICE_OPTION, Boolean.TRUE, 0, null, null, FIXED_INSTANT, FIXED_INSTANT);
        when(answerOptionRepository.findAnswerOptionById(10L)).thenReturn(existing);
        when(questionRepository.findQuestionById(20L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(20L)).thenReturn(List.of(existing, sibling));
        when(answerOptionRepository.saveAnswerOption(any(AnswerOption.class))).thenReturn(updated);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateAnswerOption(20L, 10L, new UpdateAnswerOptionCommand(
            "New",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null
        ));

        verify(answerOptionRepository).saveAnswerOption(any(AnswerOption.class));
    }

    @Test
    void updateAnswerOptionRejectsNonDraftQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = new AnswerOption(10L, 20L, "Old", AnswerOptionRole.CHOICE_OPTION, Boolean.FALSE, 0, null, null, FIXED_INSTANT, FIXED_INSTANT);
        Question parent = new Question(20L, 30L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(answerOptionRepository.findAnswerOptionById(10L)).thenReturn(existing);
        when(questionRepository.findQuestionById(20L)).thenReturn(parent);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Question");

        assertThatThrownBy(() -> service.updateAnswerOption(20L, 10L, new UpdateAnswerOptionCommand(
            "New",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Question must be DRAFT");

        verify(answerOptionRepository, never()).saveAnswerOption(any(AnswerOption.class));
    }

    @Test
    void deleteAnswerOptionRunsAdmissionBeforeDelete() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = new AnswerOption(11L, 21L, "Old", AnswerOptionRole.CHOICE_OPTION, Boolean.FALSE, 0, null, null, FIXED_INSTANT, FIXED_INSTANT);
        Question parent = new Question(21L, 31L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(answerOptionRepository.findAnswerOptionById(11L)).thenReturn(existing);
        when(questionRepository.findQuestionById(21L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(21L)).thenReturn(List.of(existing));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));

        service.deleteAnswerOption(21L, 11L);

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            21L,
            21L,
            CapabilityTargetEntityType.QUESTION
        );
        verify(answerOptionRepository).deleteAnswerOption(11L);
        verify(questionRepository, never()).saveQuestion(any(Question.class));
        verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(21L), eq(existing), eq(null), any());
    }

    @Test
    void deleteAnswerOptionRejectsNonDraftQuestion() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = new AnswerOption(11L, 21L, "Old", AnswerOptionRole.CHOICE_OPTION, Boolean.FALSE, 0, null, null, FIXED_INSTANT, FIXED_INSTANT);
        Question parent = new Question(21L, 31L, "Q", QuestionType.SINGLE_CHOICE, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(answerOptionRepository.findAnswerOptionById(11L)).thenReturn(existing);
        when(questionRepository.findQuestionById(21L)).thenReturn(parent);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Question");

        assertThatThrownBy(() -> service.deleteAnswerOption(21L, 11L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Question must be DRAFT");

        verify(answerOptionRepository, never()).deleteAnswerOption(11L);
    }
}
