package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Читатель {@code RepositoryBackedSelfVisibleTestingProjectionReader}.
 */
@Service
@Transactional(readOnly = true)
class RepositoryBackedSelfVisibleTestingProjectionReader implements SelfVisibleTestingProjectionReader {

    private static final Comparator<Course> COURSE_ORDER = Comparator
        .comparing(RepositoryBackedSelfVisibleTestingProjectionReader::courseSortOrder)
        .thenComparing(Course::id);
    private static final Comparator<Topic> TOPIC_ORDER = Comparator
        .comparingInt(Topic::sortOrder)
        .thenComparing(Topic::id);
    private static final Comparator<Test> TEST_ORDER = Comparator
        .comparingInt(Test::sortOrder)
        .thenComparing(Test::id);
    private static final Comparator<TestQuestion> TEST_QUESTION_ORDER = Comparator
        .comparingInt(TestQuestion::displayOrder)
        .thenComparing(TestQuestion::id);
    private static final Comparator<AnswerOption> ANSWER_OPTION_ORDER = Comparator
        .comparingInt(AnswerOption::displayOrder)
        .thenComparing(AnswerOption::id);
    private static final Comparator<Material> MATERIAL_ORDER = Comparator
        .comparingInt(Material::sortOrder)
        .thenComparing(Material::id);

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final MaterialRepository materialRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final SelfVisibleTestVisibilityFilter selfVisibleTestVisibilityFilter;

    RepositoryBackedSelfVisibleTestingProjectionReader(
        CourseRepository courseRepository,
        TopicRepository topicRepository,
        TestRepository testRepository,
        MaterialRepository materialRepository,
        TestQuestionRepository testQuestionRepository,
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository,
        SelfVisibleTestVisibilityFilter selfVisibleTestVisibilityFilter
    ) {
        this.courseRepository = Objects.requireNonNull(courseRepository, "courseRepository must not be null");
        this.topicRepository = Objects.requireNonNull(topicRepository, "topicRepository must not be null");
        this.testRepository = Objects.requireNonNull(testRepository, "testRepository must not be null");
        this.materialRepository = Objects.requireNonNull(materialRepository, "materialRepository must not be null");
        this.testQuestionRepository = Objects.requireNonNull(
            testQuestionRepository,
            "testQuestionRepository must not be null"
        );
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
        this.answerOptionRepository = Objects.requireNonNull(
            answerOptionRepository,
            "answerOptionRepository must not be null"
        );
        this.selfVisibleTestVisibilityFilter = Objects.requireNonNull(
            selfVisibleTestVisibilityFilter,
            "selfVisibleTestVisibilityFilter must not be null"
        );
    }

    @Override
    public List<SelfVisibleTestCatalogEntryReadModel> findSelfVisibleTests() {
        return courseRepository.findCoursesByStatus(ContentStatus.PUBLISHED).stream()
            .sorted(COURSE_ORDER)
            .flatMap(course -> topicRepository.findTopicsByCourseIdAndStatus(course.id(), ContentStatus.PUBLISHED).stream()
                .sorted(TOPIC_ORDER)
                .flatMap(topic -> testRepository.findTestsByTopicIdAndStatus(topic.id(), ContentStatus.PUBLISHED).stream()
                    .filter(selfVisibleTestVisibilityFilter::isSelfVisible)
                    .sorted(TEST_ORDER)
                    .map(test -> toCatalogEntry(course, topic, test))
                )
            )
            .toList();
    }

    @Override
    public SelfVisibleTestReadModel findSelfVisibleTestById(Long testId) {
        Test test = testRepository.findTestById(testId);
        Topic topic = selfVisibleTestVisibilityFilter.requirePublishedTopic(topicRepository.findTopicById(test.topicId()));
        selfVisibleTestVisibilityFilter.requirePublishedCourse(courseRepository.findCourseById(topic.courseId()));
        selfVisibleTestVisibilityFilter.requireSelfVisible(test);
        List<SelfVisibleTestReadModel.SelfVisibleQuestionReadModel> questions = testQuestionRepository.findTestQuestionsByTestId(
            test.id()
        )
            .stream()
            .sorted(TEST_QUESTION_ORDER)
            .map(this::toQuestionReadModel)
            .toList();

        return new SelfVisibleTestReadModel(
            test.id(),
            test.topicId(),
            test.name(),
            test.description(),
            test.testType(),
            questions
        );
    }

    @Override
    public SelfVisibleTopicReadModel findSelfVisibleTopicById(Long topicId) {
        Topic topic = selfVisibleTestVisibilityFilter.requirePublishedTopic(topicRepository.findTopicById(topicId));
        Course course = selfVisibleTestVisibilityFilter.requirePublishedCourse(courseRepository.findCourseById(topic.courseId()));
        List<SelfVisibleTopicReadModel.SelfVisibleMaterialReadModel> materials = materialRepository
            .findMaterialsByTopicIdAndStatus(topic.id(), ContentStatus.PUBLISHED).stream()
            .sorted(MATERIAL_ORDER)
            .map(this::toMaterialReadModel)
            .toList();

        return new SelfVisibleTopicReadModel(
            topic.id(),
            topic.name(),
            topic.description(),
            course.id(),
            course.name(),
            materials
        );
    }

    private SelfVisibleTestCatalogEntryReadModel toCatalogEntry(Course course, Topic topic, Test test) {
        return new SelfVisibleTestCatalogEntryReadModel(
            test.id(),
            course.id(),
            course.name(),
            topic.id(),
            topic.name(),
            test.name(),
            test.description(),
            test.testType()
        );
    }

    private SelfVisibleTestReadModel.SelfVisibleQuestionReadModel toQuestionReadModel(TestQuestion testQuestion) {
        Question question = requirePublishedQuestion(questionRepository.findQuestionById(testQuestion.questionId()));
        List<SelfVisibleTestReadModel.SelfVisibleAnswerOptionReadModel> answerOptions = answerOptionRepository
            .findAnswerOptionsByQuestionId(question.id()).stream()
            .sorted(ANSWER_OPTION_ORDER)
            .map(answerOption -> new SelfVisibleTestReadModel.SelfVisibleAnswerOptionReadModel(
                answerOption.id(),
                answerOption.body(),
                answerOption.answerOptionRole(),
                answerOption.displayOrder()
            ))
            .toList();

        return new SelfVisibleTestReadModel.SelfVisibleQuestionReadModel(
            question.id(),
            question.body(),
            question.questionType(),
            testQuestion.displayOrder(),
            testQuestion.weight(),
            answerOptions
        );
    }

    private SelfVisibleTopicReadModel.SelfVisibleMaterialReadModel toMaterialReadModel(Material material) {
        return new SelfVisibleTopicReadModel.SelfVisibleMaterialReadModel(
            material.id(),
            material.name(),
            material.description(),
            material.body(),
            material.videoUrl(),
            material.materialType(),
            material.sortOrder(),
            material.updatedAt()
        );
    }

    private Question requirePublishedQuestion(Question question) {
        if (question.status() != ContentStatus.PUBLISHED) {
            throw new NotFoundException("Self-visible question not found: " + question.id());
        }
        return question;
    }

    private static Integer courseSortOrder(Course course) {
        return course.sortOrder() == null ? Integer.MAX_VALUE : course.sortOrder();
    }
}
