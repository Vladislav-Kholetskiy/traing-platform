package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code QuestionCommandServiceImpl}.
 */
@Service
@Transactional
public class QuestionCommandServiceImpl implements QuestionCommandService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final TopicRepository topicRepository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final UtcClock utcClock;

    public QuestionCommandServiceImpl(
            QuestionRepository questionRepository,
            AnswerOptionRepository answerOptionRepository,
            TopicRepository topicRepository,
            ContentCommandSupport support,
            CriticalCommandAuditSupport auditSupport,
            UtcClock utcClock
    ) {
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.topicRepository = topicRepository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.utcClock = utcClock;
    }

    @Override
    public Question createQuestion(CreateQuestionCommand command) {
        support.requireNonNull("topicId", command.topicId());
        support.requireNotBlank("body", command.body());
        support.requireNonNull("questionType", command.questionType());
        support.validateNonNegative("sortOrder", command.sortOrder());
        support.checkChildRootCreate(
                CapabilityOperationCode.CONTENT_DRAFT_CREATE,
                CapabilityTargetEntityType.QUESTION,
                command.topicId(),
                CapabilityTargetEntityType.TOPIC
        );
        var parent = topicRepository.findTopicById(command.topicId());
        support.validateParentForChildRootAuthoring(parent.status(), "Topic");
        var now = utcClock.now();
        Question saved = questionRepository.saveQuestion(new Question(
                null,
                command.topicId(),
                command.body(),
                command.questionType(),
                ContentStatus.DRAFT,
                command.sortOrder(),
                now,
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_QUESTION_DRAFT_CREATED),
                "question",
                saved.id(),
                null,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_DRAFT_CREATE.code(),
                        Map.of("entityType", "question", "topicId", saved.topicId())
                )
        );
        return saved;
    }

    @Override
    public Question updateQuestion(Long questionId, UpdateQuestionCommand command) {
        var existing = questionRepository.findQuestionById(questionId);
        support.checkDraftUpdate(
                CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
                CapabilityTargetEntityType.QUESTION,
                questionId,
                existing.topicId(),
                CapabilityTargetEntityType.TOPIC
        );
        support.requireDraft(existing.status(), "Question");
        support.requireNotBlank("body", command.body());
        support.requireNonNull("questionType", command.questionType());
        support.validateNonNegative("sortOrder", command.sortOrder());
        validateQuestionTypeTransition(existing, command.questionType());
        var now = utcClock.now();
        Question saved = questionRepository.saveQuestion(new Question(
                existing.id(),
                existing.topicId(),
                command.body(),
                command.questionType(),
                existing.status(),
                command.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_QUESTION_DRAFT_UPDATED),
                "question",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(),
                        Map.of("entityType", "question", "topicId", saved.topicId())
                )
        );
        return saved;
    }

    @Override
    public AnswerOption createAnswerOption(Long questionId, CreateAnswerOptionCommand command) {
        support.checkDraftUpdate(
                CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
                CapabilityTargetEntityType.QUESTION,
                questionId,
                questionId,
                CapabilityTargetEntityType.QUESTION
        );
        var parent = questionRepository.findQuestionById(questionId);
        support.requireDraft(parent.status(), "Question");
        support.validateAnswerOptionFields(
                command.body(),
                command.answerOptionRole(),
                command.isCorrect(),
                command.displayOrder(),
                command.canonicalOrderPosition()
        );
        var now = utcClock.now();
        AnswerOption candidate = new AnswerOption(
                null,
                questionId,
                command.body(),
                command.answerOptionRole(),
                command.isCorrect(),
                command.displayOrder(),
                command.pairingKey(),
                command.canonicalOrderPosition(),
                now,
                now
        );
        List<AnswerOption> resultingOptions = new ArrayList<>(
                answerOptionRepository.findAnswerOptionsByQuestionId(questionId)
        );
        resultingOptions.add(candidate);
        validateUniqueAnswerOptionDisplayOrder(resultingOptions);
        validateDraftCompositionCompatibleWithQuestionType(parent.questionType(), resultingOptions);
        AnswerOption saved = answerOptionRepository.saveAnswerOption(candidate);
        recordQuestionCompositionAudit(parent.id(), CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(), "create", null, saved);
        return saved;
    }

    @Override
    public AnswerOption updateAnswerOption(Long questionId, Long answerOptionId, UpdateAnswerOptionCommand command) {
        var existing = answerOptionRepository.findAnswerOptionById(answerOptionId);
        if (!questionId.equals(existing.questionId())) {
            throw new ConflictException("Answer option does not belong to question");
        }
        var parent = questionRepository.findQuestionById(existing.questionId());
        support.checkDraftUpdate(
                CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
                CapabilityTargetEntityType.QUESTION,
                parent.id(),
                parent.id(),
                CapabilityTargetEntityType.QUESTION
        );
        support.requireDraft(parent.status(), "Question");
        support.validateAnswerOptionFields(
                command.body(),
                command.answerOptionRole(),
                command.isCorrect(),
                command.displayOrder(),
                command.canonicalOrderPosition()
        );
        var now = utcClock.now();
        AnswerOption candidate = new AnswerOption(
                existing.id(),
                existing.questionId(),
                command.body(),
                command.answerOptionRole(),
                command.isCorrect(),
                command.displayOrder(),
                command.pairingKey(),
                command.canonicalOrderPosition(),
                existing.createdAt(),
                now
        );
        List<AnswerOption> resultingOptions = loadQuestionCompositionEnsuringExistingPresent(questionId, existing);
        replaceAnswerOption(resultingOptions, candidate);
        validateUniqueAnswerOptionDisplayOrder(resultingOptions);
        validateDraftCompositionCompatibleWithQuestionType(parent.questionType(), resultingOptions);
        AnswerOption saved = answerOptionRepository.saveAnswerOption(candidate);
        recordQuestionCompositionAudit(parent.id(), CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(), "update", existing, saved);
        return saved;
    }

    @Override
    public void deleteAnswerOption(Long questionId, Long answerOptionId) {
        var existing = answerOptionRepository.findAnswerOptionById(answerOptionId);
        if (!questionId.equals(existing.questionId())) {
            throw new ConflictException("Answer option does not belong to question");
        }
        var parent = questionRepository.findQuestionById(existing.questionId());
        support.checkDraftUpdate(
                CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
                CapabilityTargetEntityType.QUESTION,
                parent.id(),
                parent.id(),
                CapabilityTargetEntityType.QUESTION
        );
        support.requireDraft(parent.status(), "Question");
        List<AnswerOption> resultingOptions = loadQuestionCompositionEnsuringExistingPresent(questionId, existing);
        resultingOptions.removeIf(option -> option.id().equals(answerOptionId));
        validateDraftCompositionCompatibleWithQuestionType(parent.questionType(), resultingOptions);
        answerOptionRepository.deleteAnswerOption(answerOptionId);
        recordQuestionCompositionAudit(parent.id(), CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(), "delete", existing, null);
    }

    private void validateQuestionTypeTransition(Question existing, QuestionType newQuestionType) {
        if (existing.questionType() == newQuestionType) {
            return;
        }
        List<AnswerOption> currentOptions = answerOptionRepository.findAnswerOptionsByQuestionId(existing.id());
        validateDraftCompositionCompatibleWithQuestionType(newQuestionType, currentOptions);
    }

    private void validateDraftCompositionCompatibleWithQuestionType(
            QuestionType questionType,
            List<AnswerOption> options
    ) {
        for (AnswerOption option : options) {
            if (!isRoleAllowedForQuestionType(questionType, option.answerOptionRole())) {
                throw new ValidationException(
                        "Draft answer-option composition is incompatible with questionType="
                                + questionType
                                + ": role "
                                + option.answerOptionRole()
                                + " is not allowed"
                );
            }
        }
    }

    private void validateUniqueAnswerOptionDisplayOrder(List<AnswerOption> options) {
        var seenDisplayOrders = new HashSet<Integer>();
        for (AnswerOption option : options) {
            if (!seenDisplayOrders.add(option.displayOrder())) {
                throw new ConflictException("Answer option displayOrder must be unique within question");
            }
        }
    }

    private boolean isRoleAllowedForQuestionType(QuestionType questionType, AnswerOptionRole role) {
        return switch (questionType) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> role == AnswerOptionRole.CHOICE_OPTION;
            case MATCHING -> role == AnswerOptionRole.MATCH_LEFT || role == AnswerOptionRole.MATCH_RIGHT;
            case ORDERING -> role == AnswerOptionRole.ORDER_ITEM;
        };
    }

    private List<AnswerOption> loadQuestionCompositionEnsuringExistingPresent(Long questionId, AnswerOption existing) {
        List<AnswerOption> options = new ArrayList<>(
                answerOptionRepository.findAnswerOptionsByQuestionId(questionId)
        );
        boolean alreadyPresent = options.stream()
                .anyMatch(option -> option.id().equals(existing.id()));
        if (!alreadyPresent) {
            options.add(existing);
        }
        return options;
    }

    private void replaceAnswerOption(List<AnswerOption> options, AnswerOption replacement) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id().equals(replacement.id())) {
                options.set(i, replacement);
                return;
            }
        }
        throw new ConflictException("Answer option does not belong to question");
    }

    private void recordQuestionCompositionAudit(
            Long questionId,
            String operationCode,
            String action,
            AnswerOption payloadBefore,
            AnswerOption payloadAfter
    ) {
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_QUESTION_DRAFT_COMPOSITION_UPDATED),
                "question",
                questionId,
                payloadBefore,
                payloadAfter,
                auditSupport.buildAuditContext(
                        "content",
                        operationCode,
                        Map.of("entityType", "answer_option", "questionId", questionId, "action", action)
                )
        );
    }
}
