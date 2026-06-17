package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code TestQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class TestQueryServiceImpl implements TestQueryService {

    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final AccessSpecificationPolicy policy;
    private final AccessPolicyQueryContextResolver resolver;

    public TestQueryServiceImpl(
        TestRepository testRepository,
        TestQuestionRepository testQuestionRepository,
        AccessSpecificationPolicy policy,
        AccessPolicyQueryContextResolver resolver
    ) {
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.policy = policy;
        this.resolver = resolver;
    }

    @Override
    public Test findTestById(Long testId) {
        ensure(AccessReadArea.CONTENT_AUTHORING, AccessReadType.DETAIL);
        return testRepository.findTestById(testId);
    }

    @Override
    public List<Test> findTestsByTopicId(Long topicId) {
        ensure(AccessReadArea.CONTENT_AUTHORING, AccessReadType.LIST);
        return testRepository.findTestsByTopicId(topicId);
    }

    @Override
    public List<Test> findTestsByTopicIdAndStatus(Long topicId, ContentStatus status) {
        ensure(AccessReadArea.CONTENT_AUTHORING, AccessReadType.LIST);
        return testRepository.findTestsByTopicIdAndStatus(topicId, status);
    }

    @Override
    public Optional<Test> findActiveFinalTestByTopicId(Long topicId) {
        ensure(AccessReadArea.CONTENT_FINAL_CONTROL, AccessReadType.DETAIL);
        return testRepository.findActiveFinalTestByTopicId(topicId);
    }

    @Override
    public List<Test> findEligibleFinalControlTestsByTopicId(Long topicId) {
        ensure(AccessReadArea.CONTENT_FINAL_CONTROL, AccessReadType.LIST);
        return testRepository.findEligibleFinalControlTestsByTopicId(topicId);
    }

    @Override
    public TestQuestion findTestQuestionById(Long testQuestionId) {
        ensure(AccessReadArea.CONTENT_AUTHORING, AccessReadType.DETAIL);
        return testQuestionRepository.findTestQuestionById(testQuestionId);
    }

    @Override
    public List<TestQuestion> findTestQuestionsByTestId(Long testId) {
        ensure(AccessReadArea.CONTENT_AUTHORING, AccessReadType.LIST);
        return testQuestionRepository.findTestQuestionsByTestId(testId);
    }

    private void ensure(AccessReadArea contour, AccessReadType readType) {
        if (!policy.canRead(resolver.resolve(contour, readType, "test"))) {
            throw new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "Actor is not authorized to read test data");
        }
    }
}
