package com.vladislav.training.platform.testing.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code SelfExecutionAdmissionFoundationStateRead}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfExecutionAdmissionFoundationStateReadServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T13:00:00Z");

    @Mock
    private TestRepository testRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private CourseRepository courseRepository;

    private SelfExecutionAdmissionFoundationStateReadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SelfExecutionAdmissionFoundationStateReadServiceImpl(
            testRepository,
            topicRepository,
            courseRepository
        );
    }

    @Test
    void returnsSelfExecutionFoundationStateWhenCanonicalSelfExecutionContentAnchorAllowsAdmission() {
        var test = test(501L, 301L, ContentStatus.PUBLISHED, false);
        var topic = topic(301L, 201L, ContentStatus.PUBLISHED);
        var course = course(201L, ContentStatus.PUBLISHED);
        when(testRepository.findTestById(501L)).thenReturn(test);
        when(topicRepository.findTopicById(301L)).thenReturn(topic);
        when(courseRepository.findCourseById(201L)).thenReturn(course);

        var foundationState = service.findSelfExecutionAdmissionFoundationState(101L, 501L);

        assertThat(foundationState.testId()).isEqualTo(501L);

        InOrder inOrder = inOrder(testRepository, topicRepository, courseRepository);
        inOrder.verify(testRepository).findTestById(501L);
        inOrder.verify(topicRepository).findTopicById(301L);
        inOrder.verify(courseRepository).findCourseById(201L);
    }

    @Test
    void returnsNullWhenTopicIsNotPublished() {
        var test = test(502L, 302L, ContentStatus.PUBLISHED, false);
        var topic = topic(302L, 202L, ContentStatus.DRAFT);
        when(testRepository.findTestById(502L)).thenReturn(test);
        when(topicRepository.findTopicById(302L)).thenReturn(topic);

        assertThat(service.findSelfExecutionAdmissionFoundationState(101L, 502L)).isNull();
    }

    @Test
    void returnsNullWhenTestIsMissing() {
        when(testRepository.findTestById(503L)).thenThrow(new NotFoundException("Test not found: 503"));

        assertThat(service.findSelfExecutionAdmissionFoundationState(101L, 503L)).isNull();
    }

    @Test
    void returnsNullWhenCourseIsNotPublished() {
        var test = test(504L, 304L, ContentStatus.PUBLISHED, false);
        var topic = topic(304L, 204L, ContentStatus.PUBLISHED);
        var course = course(204L, ContentStatus.DRAFT);
        when(testRepository.findTestById(504L)).thenReturn(test);
        when(topicRepository.findTopicById(304L)).thenReturn(topic);
        when(courseRepository.findCourseById(204L)).thenReturn(course);

        assertThat(service.findSelfExecutionAdmissionFoundationState(101L, 504L)).isNull();
    }

    @Test
    void returnsNullWhenTestIsNotEligibleForSelfExecutionAdmission() {
        var finalControlTest = test(505L, 305L, ContentStatus.DRAFT, false);
        var topic = topic(305L, 205L, ContentStatus.PUBLISHED);
        var course = course(205L, ContentStatus.PUBLISHED);
        when(testRepository.findTestById(505L)).thenReturn(finalControlTest);
        when(topicRepository.findTopicById(305L)).thenReturn(topic);
        when(courseRepository.findCourseById(205L)).thenReturn(course);

        assertThat(service.findSelfExecutionAdmissionFoundationState(101L, 505L)).isNull();
    }

    @Test
    void rejectsNullIdsBeforeAnyRepositoryRead() {
        assertThatThrownBy(() -> service.findSelfExecutionAdmissionFoundationState(null, 501L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId");
        assertThatThrownBy(() -> service.findSelfExecutionAdmissionFoundationState(101L, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("testId");
        verifyNoInteractions(testRepository, topicRepository, courseRepository);
    }

    private com.vladislav.training.platform.content.domain.Test test(
        Long id,
        Long topicId,
        ContentStatus status,
        boolean activeFinal
    ) {
        return new com.vladislav.training.platform.content.domain.Test(
            id,
            topicId,
            "Self test",
            "Description",
            TestType.TRAINING,
            status,
            new BigDecimal("70.00"),
            "STANDARD",
            activeFinal,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private Topic topic(Long id, Long courseId, ContentStatus status) {
        return new Topic(id, courseId, "Topic", "Description", status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private Course course(Long id, ContentStatus status) {
        return new Course(id, "Course", "Description", status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }
}
