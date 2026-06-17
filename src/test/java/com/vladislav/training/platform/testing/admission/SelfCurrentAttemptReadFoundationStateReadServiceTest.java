package com.vladislav.training.platform.testing.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code SelfCurrentAttemptReadFoundationStateRead}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfCurrentAttemptReadFoundationStateReadServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-23T11:00:00Z");

    @Mock
    private TestRepository testRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private CourseRepository courseRepository;

    private SelfCurrentAttemptReadFoundationStateReadService service;

    @BeforeEach
    void setUp() {
        service = new SelfCurrentAttemptReadFoundationStateReadServiceImpl(
            testRepository,
            topicRepository,
            courseRepository
        );
    }

    @Test
    void returnsFoundationWhenSelfCurrentAttemptReadAnchorIsVisible() {
        when(testRepository.findTestById(501L)).thenReturn(test(501L, 201L, false, ContentStatus.PUBLISHED));
        when(topicRepository.findTopicById(201L)).thenReturn(topic(201L, 301L, ContentStatus.PUBLISHED));
        when(courseRepository.findCourseById(301L)).thenReturn(course(301L, ContentStatus.PUBLISHED));

        assertThat(service.findSelfCurrentAttemptReadFoundationState(101L, 501L))
            .isEqualTo(new SelfCurrentAttemptReadFoundationStateReadService.SelfCurrentAttemptReadFoundationState(501L));
    }

    @Test
    void returnsNullWhenSelfCurrentAttemptReadAnchorIsNotVisible() {
        when(testRepository.findTestById(502L)).thenReturn(test(502L, 202L, true, ContentStatus.PUBLISHED));
        when(topicRepository.findTopicById(202L)).thenReturn(topic(202L, 302L, ContentStatus.PUBLISHED));
        when(courseRepository.findCourseById(302L)).thenReturn(course(302L, ContentStatus.PUBLISHED));

        assertThat(service.findSelfCurrentAttemptReadFoundationState(101L, 502L)).isNull();
    }

    @Test
    void returnsNullWhenContentChainIsMissing() {
        when(testRepository.findTestById(503L)).thenThrow(new NotFoundException("Test not found: 503"));

        assertThat(service.findSelfCurrentAttemptReadFoundationState(101L, 503L)).isNull();
    }

    @Test
    void rejectsNullIdsBeforeAnyLookup() {
        assertThatThrownBy(() -> service.findSelfCurrentAttemptReadFoundationState(null, 501L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId");

        assertThatThrownBy(() -> service.findSelfCurrentAttemptReadFoundationState(101L, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("testId");
    }

    private com.vladislav.training.platform.content.domain.Test test(
        Long id,
        Long topicId,
        boolean activeFinalForTopic,
        ContentStatus status
    ) {
        return new com.vladislav.training.platform.content.domain.Test(
            id,
            topicId,
            "Test " + id,
            "Description",
            TestType.CONTROL,
            status,
            new BigDecimal("70.00"),
            "STANDARD",
            activeFinalForTopic,
            0,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private Topic topic(Long id, Long courseId, ContentStatus status) {
        return new Topic(
            id,
            courseId,
            "Topic " + id,
            "Description",
            status,
            0,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private Course course(Long id, ContentStatus status) {
        return new Course(
            id,
            "Course " + id,
            "Description",
            status,
            0,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }
}
