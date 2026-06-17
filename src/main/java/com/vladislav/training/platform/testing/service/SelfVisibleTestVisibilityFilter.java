package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.common.exception.NotFoundException;
import org.springframework.stereotype.Component;

/**
 * Фильтр {@code SelfVisibleTestVisibilityFilter}.
 */
@Component
public class SelfVisibleTestVisibilityFilter {

    public Course requirePublishedCourse(Course course) {
        if (course.status() != ContentStatus.PUBLISHED) {
            throw new NotFoundException("Self-visible course not found: " + course.id());
        }
        return course;
    }

    public Topic requirePublishedTopic(Topic topic) {
        if (topic.status() != ContentStatus.PUBLISHED) {
            throw new NotFoundException("Self-visible topic not found: " + topic.id());
        }
        return topic;
    }

    public boolean isSelfVisible(Test test) {
        return test.status() == ContentStatus.PUBLISHED && !test.isActiveFinalForTopic();
    }

    public Test requireSelfVisible(Test test) {
        if (!isSelfVisible(test)) {
            throw new NotFoundException("Self-visible test not found: " + test.id());
        }
        return test;
    }
}
