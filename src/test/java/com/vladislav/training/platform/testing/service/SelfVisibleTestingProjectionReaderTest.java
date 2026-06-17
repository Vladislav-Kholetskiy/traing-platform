package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет чтение данных в {@code SelfVisibleTestingProjection}.
 * Тест держит под контролем выборку и состав возвращаемых данных.
 */
@ExtendWith(MockitoExtension.class)
class SelfVisibleTestingProjectionReaderTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T09:00:00Z");

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private TestRepository testRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private TestQuestionRepository testQuestionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerOptionRepository answerOptionRepository;

    private SelfVisibleTestingProjectionReader reader;

    @BeforeEach
    void setUp() {
        reader = new RepositoryBackedSelfVisibleTestingProjectionReader(
            courseRepository,
            topicRepository,
            testRepository,
            materialRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            new SelfVisibleTestVisibilityFilter()
        );
    }

    @Test
    void catalogProjectionDoesNotExposeDraftHiddenOrActiveFinalContent() {
        Course publishedCourse = course(201L, "Published course", ContentStatus.PUBLISHED, 1);
        Topic publishedTopic = topic(301L, 201L, "Published topic", ContentStatus.PUBLISHED, 0);
        Topic draftTopic = topic(302L, 201L, "Draft topic", ContentStatus.DRAFT, 1);
        var visibleTest = test(41L, 301L, "Visible self test", ContentStatus.PUBLISHED, false, 0);
        var hiddenFinal = test(42L, 301L, "Final control", ContentStatus.PUBLISHED, true, 1);
        var draftTest = test(43L, 301L, "Draft test", ContentStatus.DRAFT, false, 2);

        when(courseRepository.findCoursesByStatus(ContentStatus.PUBLISHED)).thenReturn(List.of(publishedCourse));
        when(topicRepository.findTopicsByCourseIdAndStatus(201L, ContentStatus.PUBLISHED)).thenReturn(List.of(publishedTopic));
        when(testRepository.findTestsByTopicIdAndStatus(301L, ContentStatus.PUBLISHED))
            .thenReturn(List.of(hiddenFinal, visibleTest));

        List<SelfVisibleTestCatalogEntryReadModel> catalog = reader.findSelfVisibleTests();

        assertThat(catalog).extracting(SelfVisibleTestCatalogEntryReadModel::id).containsExactly(41L);
        verify(topicRepository, never()).findTopicsByCourseIdAndStatus(201L, ContentStatus.DRAFT);
        assertThat(draftTest.status()).isEqualTo(ContentStatus.DRAFT);
    }

    @Test
    void detailProjectionAssemblesPublishedVisibleCompositionOnly() {
        var visibleTest = test(41L, 301L, "Visible self test", ContentStatus.PUBLISHED, false, 0);
        Topic topic = topic(301L, 201L, "Topic", ContentStatus.PUBLISHED, 0);
        Course course = course(201L, "Course", ContentStatus.PUBLISHED, 1);
        when(testRepository.findTestById(41L)).thenReturn(visibleTest);
        when(topicRepository.findTopicById(301L)).thenReturn(topic);
        when(courseRepository.findCourseById(201L)).thenReturn(course);
        when(testQuestionRepository.findTestQuestionsByTestId(41L)).thenReturn(List.of(
            testQuestion(702L, 41L, 502L, 1, "1.50"),
            testQuestion(701L, 41L, 501L, 0, "2.00")
        ));
        when(questionRepository.findQuestionById(501L)).thenReturn(question(501L, "First question", ContentStatus.PUBLISHED));
        when(questionRepository.findQuestionById(502L)).thenReturn(question(502L, "Second question", ContentStatus.PUBLISHED));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(501L)).thenReturn(List.of(
            answerOption(9002L, 501L, "B", 1),
            answerOption(9001L, 501L, "A", 0)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(502L)).thenReturn(List.of(answerOption(9003L, 502L, "C", 0)));

        SelfVisibleTestReadModel projection = reader.findSelfVisibleTestById(41L);

        assertThat(projection.id()).isEqualTo(41L);
        assertThat(projection.questions()).hasSize(2);
        assertThat(projection.questions().get(0).id()).isEqualTo(501L);
        assertThat(projection.questions().get(0).answerOptions())
            .extracting(SelfVisibleTestReadModel.SelfVisibleAnswerOptionReadModel::id)
            .containsExactly(9001L, 9002L);
    }

    @Test
    void detailProjectionRejectsDraftQuestionFailClosed() {
        var visibleTest = test(41L, 301L, "Visible self test", ContentStatus.PUBLISHED, false, 0);
        Topic topic = topic(301L, 201L, "Topic", ContentStatus.PUBLISHED, 0);
        Course course = course(201L, "Course", ContentStatus.PUBLISHED, 1);
        when(testRepository.findTestById(41L)).thenReturn(visibleTest);
        when(topicRepository.findTopicById(301L)).thenReturn(topic);
        when(courseRepository.findCourseById(201L)).thenReturn(course);
        when(testQuestionRepository.findTestQuestionsByTestId(41L)).thenReturn(List.of(testQuestion(701L, 41L, 501L, 0, "2.00")));
        when(questionRepository.findQuestionById(501L)).thenReturn(question(501L, "Draft question", ContentStatus.DRAFT));

        assertThatThrownBy(() -> reader.findSelfVisibleTestById(41L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self-visible question not found: 501");

        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void detailProjectionRejectsActiveFinalControlEvenIfPublished() {
        var hiddenFinal = new com.vladislav.training.platform.content.domain.Test(
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
        Topic topic = topic(301L, 201L, "Topic", ContentStatus.PUBLISHED, 0);
        Course course = course(201L, "Course", ContentStatus.PUBLISHED, 1);
        when(testRepository.findTestById(43L)).thenReturn(hiddenFinal);
        when(topicRepository.findTopicById(301L)).thenReturn(topic);
        when(courseRepository.findCourseById(201L)).thenReturn(course);

        assertThatThrownBy(() -> reader.findSelfVisibleTestById(43L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self-visible test not found: 43");

        verify(testQuestionRepository, never()).findTestQuestionsByTestId(43L);
    }

    private com.vladislav.training.platform.content.domain.Test test(
        Long id,
        Long topicId,
        String name,
        ContentStatus status,
        boolean activeFinal,
        int sortOrder
    ) {
        return new com.vladislav.training.platform.content.domain.Test(
            id,
            topicId,
            name,
            "Description",
            activeFinal ? TestType.CONTROL : TestType.TRAINING,
            status,
            new BigDecimal("70.00"),
            "STANDARD",
            activeFinal,
            sortOrder,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private Course course(Long id, String name, ContentStatus status, Integer sortOrder) {
        return new Course(id, name, "Description", status, sortOrder, FIXED_INSTANT, FIXED_INSTANT);
    }

    private Topic topic(Long id, Long courseId, String name, ContentStatus status, int sortOrder) {
        return new Topic(id, courseId, name, "Description", status, sortOrder, FIXED_INSTANT, FIXED_INSTANT);
    }

    private TestQuestion testQuestion(Long id, Long testId, Long questionId, int displayOrder, String weight) {
        return new TestQuestion(id, testId, questionId, displayOrder, new BigDecimal(weight), FIXED_INSTANT, FIXED_INSTANT);
    }

    private Question question(Long id, String body, ContentStatus status) {
        return new Question(id, 301L, body, QuestionType.SINGLE_CHOICE, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private AnswerOption answerOption(Long id, Long questionId, String body, int displayOrder) {
        return new AnswerOption(
            id,
            questionId,
            body,
            AnswerOptionRole.CHOICE_OPTION,
            false,
            displayOrder,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
