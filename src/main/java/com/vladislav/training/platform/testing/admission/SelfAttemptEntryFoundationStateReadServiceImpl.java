package com.vladislav.training.platform.testing.admission;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean({
    TestRepository.class,
    TopicRepository.class,
    CourseRepository.class
})
/**
 * Реализация сервиса {@code SelfAttemptEntryFoundationStateReadServiceImpl}.
 */
class SelfAttemptEntryFoundationStateReadServiceImpl implements SelfAttemptEntryFoundationStateReadService {

    private final TestRepository testRepository;
    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;

    SelfAttemptEntryFoundationStateReadServiceImpl(
        TestRepository testRepository,
        TopicRepository topicRepository,
        CourseRepository courseRepository
    ) {
        this.testRepository = testRepository;
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public SelfAttemptEntryFoundationState findSelfAttemptEntryFoundationState(Long actorUserId, Long testId) {
        requireId(actorUserId, "actorUserId");
        requireId(testId, "testId");

        Test test = findTestOrNull(testId);
        if (test == null) {
            return null;
        }

        try {
            Topic topic = requirePublishedTopic(topicRepository.findTopicById(test.topicId()));
            requirePublishedCourse(courseRepository.findCourseById(topic.courseId()));
            requireSelfStartEligible(test);
            return new SelfAttemptEntryFoundationState(test.id());
        } catch (NotFoundException exception) {
            return null;
        }
    }

    private Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private Test findTestOrNull(Long testId) {
        try {
            return testRepository.findTestById(testId);
        } catch (NotFoundException exception) {
            return null;
        }
    }

    private Topic requirePublishedTopic(Topic topic) {
        if (topic.status() != ContentStatus.PUBLISHED) {
            throw new NotFoundException("Self start topic not found: " + topic.id());
        }
        return topic;
    }

    private void requirePublishedCourse(com.vladislav.training.platform.content.domain.Course course) {
        if (course.status() != ContentStatus.PUBLISHED) {
            throw new NotFoundException("Self start course not found: " + course.id());
        }
    }

    private void requireSelfStartEligible(Test test) {
        if (test.status() != ContentStatus.PUBLISHED || test.isActiveFinalForTopic()) {
            throw new NotFoundException("Self start test not found: " + test.id());
        }
    }
}
