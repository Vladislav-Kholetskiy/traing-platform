package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code QuestionQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class QuestionQueryServiceImpl implements QuestionQueryService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final AccessSpecificationPolicy policy;
    private final AccessPolicyQueryContextResolver resolver;

    public QuestionQueryServiceImpl(
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository,
        AccessSpecificationPolicy policy,
        AccessPolicyQueryContextResolver resolver
    ) {
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.policy = policy;
        this.resolver = resolver;
    }

    @Override
    public Question findQuestionById(Long questionId) {
        ensure(AccessReadType.DETAIL);
        return questionRepository.findQuestionById(questionId);
    }

    @Override
    public List<Question> findQuestionsByTopicId(Long topicId) {
        ensure(AccessReadType.LIST);
        return questionRepository.findQuestionsByTopicId(topicId);
    }

    @Override
    public List<Question> findQuestionsByTopicIdAndStatus(Long topicId, ContentStatus status) {
        ensure(AccessReadType.LIST);
        return questionRepository.findQuestionsByTopicIdAndStatus(topicId, status);
    }

    @Override
    public AnswerOption findAnswerOptionById(Long answerOptionId) {
        ensure(AccessReadType.DETAIL);
        return answerOptionRepository.findAnswerOptionById(answerOptionId);
    }

    @Override
    public List<AnswerOption> findAnswerOptionsByQuestionId(Long questionId) {
        ensure(AccessReadType.LIST);
        return answerOptionRepository.findAnswerOptionsByQuestionId(questionId);
    }

    private void ensure(AccessReadType readType) {
        if (!policy.canRead(resolver.resolve(AccessReadArea.CONTENT_AUTHORING, readType, "question"))) {
            throw new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "Actor is not authorized to read question data");
        }
    }
}
