package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code QuestionLifecycleServiceImpl}.
 */
@Service
@Transactional
public class QuestionLifecycleServiceImpl implements QuestionLifecycleService {

    private final QuestionRepository repository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final TopicRepository topicRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final ContentPublicationValidationService publicationValidationService;
    private final UtcClock utcClock;

    public QuestionLifecycleServiceImpl(
            QuestionRepository repository,
            ContentCommandSupport support,
            CriticalCommandAuditSupport auditSupport,
            TopicRepository topicRepository,
            TestQuestionRepository testQuestionRepository,
            ContentPublicationValidationService publicationValidationService,
            UtcClock utcClock
    ) {
        this.repository = repository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.topicRepository = topicRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.publicationValidationService = publicationValidationService;
        this.utcClock = utcClock;
    }

    @Override
    public Question publish(Long id) {
        Question existing = repository.findQuestionById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.QUESTION, id);
        support.requireDraft(existing.status(), "Question");
        if (topicRepository.findTopicById(existing.topicId()).status() != ContentStatus.PUBLISHED) {
            throw new ConflictException("Parent topic must be PUBLISHED");
        }
        publicationValidationService.validateQuestionPublishable(id);
        var now = utcClock.now();
        Question saved = repository.saveQuestion(new Question(
                existing.id(),
                existing.topicId(),
                existing.body(),
                existing.questionType(),
                ContentStatus.PUBLISHED,
                existing.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_QUESTION_PUBLISHED),
                "question",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_PUBLISH,
                        Map.of("entityType", "question")
                )
        );
        return saved;
    }

    @Override
    public Question archive(Long id) {
        Question existing = repository.findQuestionById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.QUESTION, id);
        support.requirePublished(existing.status(), "Question");
        if (testQuestionRepository.existsPublishedTestUsingQuestion(id)) {
            throw new ConflictException("Question cannot be archived while it is used by a PUBLISHED test");
        }
        var now = utcClock.now();
        Question saved = repository.saveQuestion(new Question(
                existing.id(),
                existing.topicId(),
                existing.body(),
                existing.questionType(),
                ContentStatus.ARCHIVED,
                existing.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_QUESTION_ARCHIVED),
                "question",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_ARCHIVE,
                        Map.of("entityType", "question")
                )
        );
        return saved;
    }
}
