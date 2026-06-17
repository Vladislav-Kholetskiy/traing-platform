package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code TopicQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class TopicQueryServiceImpl implements TopicQueryService {

    private final TopicRepository repository;
    private final AccessSpecificationPolicy policy;
    private final AccessPolicyQueryContextResolver resolver;

    public TopicQueryServiceImpl(
        TopicRepository repository,
        AccessSpecificationPolicy policy,
        AccessPolicyQueryContextResolver resolver
    ) {
        this.repository = repository;
        this.policy = policy;
        this.resolver = resolver;
    }

    @Override
    public Topic findTopicById(Long topicId) {
        ensure(AccessReadType.DETAIL);
        return repository.findTopicById(topicId);
    }

    @Override
    public List<Topic> findTopicsByCourseId(Long courseId) {
        ensure(AccessReadType.LIST);
        return repository.findTopicsByCourseId(courseId);
    }

    @Override
    public List<Topic> findTopicsByCourseIdAndStatus(Long courseId, ContentStatus status) {
        ensure(AccessReadType.LIST);
        return repository.findTopicsByCourseIdAndStatus(courseId, status);
    }

    private void ensure(AccessReadType readType) {
        if (!policy.canRead(resolver.resolve(AccessReadArea.CONTENT_AUTHORING, readType, "topic"))) {
            throw new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "Actor is not authorized to read topic data");
        }
    }
}
