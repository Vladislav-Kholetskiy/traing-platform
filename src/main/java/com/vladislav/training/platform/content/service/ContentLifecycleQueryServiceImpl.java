package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code ContentLifecycleQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class ContentLifecycleQueryServiceImpl implements ContentLifecycleQueryService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String MESSAGE = "Actor is not authorized to read content lifecycle data";

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final MaterialRepository materialRepository;
    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final AccessSpecificationPolicy policy;
    private final AccessPolicyQueryContextResolver resolver;

    public ContentLifecycleQueryServiceImpl(
        CourseRepository courseRepository,
        TopicRepository topicRepository,
        MaterialRepository materialRepository,
        QuestionRepository questionRepository,
        TestRepository testRepository,
        AccessSpecificationPolicy policy,
        AccessPolicyQueryContextResolver resolver
    ) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.materialRepository = materialRepository;
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.policy = policy;
        this.resolver = resolver;
    }

    @Override
    public Course findCourseById(Long courseId) {
        ensure("course");
        return courseRepository.findCourseById(courseId);
    }

    @Override
    public Topic findTopicById(Long topicId) {
        ensure("topic");
        return topicRepository.findTopicById(topicId);
    }

    @Override
    public Material findMaterialById(Long materialId) {
        ensure("material");
        return materialRepository.findMaterialById(materialId);
    }

    @Override
    public Question findQuestionById(Long questionId) {
        ensure("question");
        return questionRepository.findQuestionById(questionId);
    }

    @Override
    public Test findTestById(Long testId) {
        ensure("test");
        return testRepository.findTestById(testId);
    }

    private void ensure(String family) {
        if (!policy.canRead(resolver.resolve(AccessReadArea.CONTENT_LIFECYCLE, AccessReadType.DETAIL, family))) {
            throw new PolicyViolationException(NOT_AUTHORIZED, MESSAGE);
        }
    }
}
