package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code SelfVisibleTestVisibilityFilter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class SelfVisibleTestVisibilityFilterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T10:30:00Z");

    private SelfVisibleTestVisibilityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SelfVisibleTestVisibilityFilter();
    }

    @Test
    void allowsPublishedNonFinalTrainingTest() {
        var test = new com.vladislav.training.platform.content.domain.Test(
            41L,
            301L,
            "Self test",
            "Description",
            TestType.TRAINING,
            ContentStatus.PUBLISHED,
            new BigDecimal("70.00"),
            "STANDARD",
            false,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );

        assertThat(filter.requireSelfVisible(test)).isSameAs(test);
        assertThat(filter.isSelfVisible(test)).isTrue();
    }

    @Test
    void rejectsDraftTestFailClosed() {
        var test = new com.vladislav.training.platform.content.domain.Test(
            42L,
            301L,
            "Draft test",
            "Description",
            TestType.TRAINING,
            ContentStatus.DRAFT,
            new BigDecimal("70.00"),
            "STANDARD",
            false,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> filter.requireSelfVisible(test))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self-visible test not found: 42");
        assertThat(filter.isSelfVisible(test)).isFalse();
    }

    @Test
    void rejectsPublishedActiveFinalControlTestFailClosed() {
        var test = new com.vladislav.training.platform.content.domain.Test(
            43L,
            301L,
            "Final control",
            "Description",
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            new BigDecimal("70.00"),
            "STANDARD",
            true,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> filter.requireSelfVisible(test))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self-visible test not found: 43");
        assertThat(filter.isSelfVisible(test)).isFalse();
    }

    @Test
    void allowsPublishedCourseAndTopicAsPartOfSelfVisibleBasis() {
        Course course = new Course(11L, "Course", "Description", ContentStatus.PUBLISHED, 1, FIXED_INSTANT, FIXED_INSTANT);
        Topic topic = new Topic(21L, 11L, "Topic", "Description", ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);

        assertThat(filter.requirePublishedCourse(course)).isSameAs(course);
        assertThat(filter.requirePublishedTopic(topic)).isSameAs(topic);
    }

    @Test
    void rejectsDraftCourseAndTopicFailClosed() {
        Course course = new Course(12L, "Course", "Description", ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        Topic topic = new Topic(22L, 12L, "Topic", "Description", ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);

        assertThatThrownBy(() -> filter.requirePublishedCourse(course))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self-visible course not found: 12");
        assertThatThrownBy(() -> filter.requirePublishedTopic(topic))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self-visible topic not found: 22");
    }
}
