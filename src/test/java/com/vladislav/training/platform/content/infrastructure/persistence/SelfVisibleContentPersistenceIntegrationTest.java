package com.vladislav.training.platform.content.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = SelfVisibleContentPersistenceIntegrationTest.ContentPersistenceTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code SelfVisibleContentPersistence} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class SelfVisibleContentPersistenceIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-06T10:15:30Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private ContentMapper contentMapper;
    @Autowired
    private JpaCourseRepositoryAdapter courseRepository;
    @Autowired
    private JpaTopicRepositoryAdapter topicRepository;
    @Autowired
    private JpaTestRepositoryAdapter testRepository;
    @Autowired
    private JpaQuestionRepositoryAdapter questionRepository;
    @Autowired
    private JpaAnswerOptionRepositoryAdapter answerOptionRepository;
    @Autowired
    private JpaTestQuestionRepositoryAdapter testQuestionRepository;

    @Autowired
    private SpringDataCourseJpaRepository courseJpaRepository;
    @Autowired
    private SpringDataTopicJpaRepository topicJpaRepository;
    @Autowired
    private SpringDataTestJpaRepository testJpaRepository;
    @Autowired
    private SpringDataQuestionJpaRepository questionJpaRepository;
    @Autowired
    private SpringDataAnswerOptionJpaRepository answerOptionJpaRepository;
    @Autowired
    private SpringDataTestQuestionJpaRepository testQuestionJpaRepository;

    @AfterEach
    void tearDown() {
        answerOptionJpaRepository.deleteAllInBatch();
        testQuestionJpaRepository.deleteAllInBatch();
        questionJpaRepository.deleteAllInBatch();
        testJpaRepository.deleteAllInBatch();
        topicJpaRepository.deleteAllInBatch();
        courseJpaRepository.deleteAllInBatch();
    }

    @Test
    void publishedCatalogPersistenceChainFiltersByStatusAndKeepsVisibilityRelevantOrdering() {
        CourseEntity publishedCourse = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 10));
        CourseEntity draftCourse = courseJpaRepository.saveAndFlush(courseEntity("Draft course", ContentStatus.DRAFT, 0));

        TopicEntity topicB = topicJpaRepository.saveAndFlush(topicEntity(
            publishedCourse.getId(),
            "Topic B",
            ContentStatus.PUBLISHED,
            2
        ));
        TopicEntity topicA = topicJpaRepository.saveAndFlush(topicEntity(
            publishedCourse.getId(),
            "Topic A",
            ContentStatus.PUBLISHED,
            0
        ));
        topicJpaRepository.saveAndFlush(topicEntity(publishedCourse.getId(), "Draft topic", ContentStatus.DRAFT, 1));
        TopicEntity hiddenByCourse = topicJpaRepository.saveAndFlush(topicEntity(
            draftCourse.getId(),
            "Published but hidden by course draft",
            ContentStatus.PUBLISHED,
            0
        ));

        testJpaRepository.saveAndFlush(testEntity(
            topicA.getId(),
            "Archived self test",
            TestType.TRAINING,
            ContentStatus.ARCHIVED,
            false,
            4
        ));
        TestEntity finalControl = testJpaRepository.saveAndFlush(testEntity(
            topicA.getId(),
            "Final control",
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            true,
            0
        ));
        TestEntity visibleSelfTest = testJpaRepository.saveAndFlush(testEntity(
            topicA.getId(),
            "Visible self test",
            TestType.TRAINING,
            ContentStatus.PUBLISHED,
            false,
            3
        ));
        testJpaRepository.saveAndFlush(testEntity(
            topicA.getId(),
            "Draft self test",
            TestType.TRAINING,
            ContentStatus.DRAFT,
            false,
            2
        ));
        testJpaRepository.saveAndFlush(testEntity(
            topicB.getId(),
            "Sibling topic test",
            TestType.TRAINING,
            ContentStatus.PUBLISHED,
            false,
            1
        ));
        testJpaRepository.saveAndFlush(testEntity(
            hiddenByCourse.getId(),
            "Draft course topic test",
            TestType.TRAINING,
            ContentStatus.PUBLISHED,
            false,
            0
        ));

        List<Course> publishedCourses = courseRepository.findCoursesByStatus(ContentStatus.PUBLISHED);
        List<Topic> publishedTopics = topicRepository.findTopicsByCourseIdAndStatus(
            publishedCourse.getId(),
            ContentStatus.PUBLISHED
        );
        List<com.vladislav.training.platform.content.domain.Test> publishedTests = testRepository.findTestsByTopicIdAndStatus(
            topicA.getId(),
            ContentStatus.PUBLISHED
        );

        assertThat(publishedCourses)
            .extracting(Course::id)
            .containsExactly(publishedCourse.getId());
        assertThat(publishedTopics)
            .extracting(Topic::id)
            .containsExactly(topicA.getId(), topicB.getId());
        assertThat(publishedTests)
            .extracting(com.vladislav.training.platform.content.domain.Test::id)
            .containsExactly(finalControl.getId(), visibleSelfTest.getId());
        assertThat(publishedTests)
            .extracting(com.vladislav.training.platform.content.domain.Test::isActiveFinalForTopic)
            .containsExactly(true, false);
        assertThat(testRepository.findActiveFinalTestByTopicId(topicA.getId()))
            .isPresent()
            .get()
            .extracting(
                com.vladislav.training.platform.content.domain.Test::id,
                com.vladislav.training.platform.content.domain.Test::testType,
                com.vladislav.training.platform.content.domain.Test::status
            )
            .containsExactly(finalControl.getId(), TestType.CONTROL, ContentStatus.PUBLISHED);
    }

    @Test
    void detailPersistenceChainPreservesSliceBoundariesAndOrderingForQuestionsAndAnswerOptions() {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 0));
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        TopicEntity otherTopic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Other topic", ContentStatus.PUBLISHED, 1));
        TestEntity test = testJpaRepository.saveAndFlush(testEntity(
            topic.getId(),
            "Visible self test",
            TestType.TRAINING,
            ContentStatus.PUBLISHED,
            false,
            0
        ));
        TestEntity draftTest = testJpaRepository.saveAndFlush(testEntity(
            topic.getId(),
            "Draft self test",
            TestType.TRAINING,
            ContentStatus.DRAFT,
            false,
            1
        ));
        TestEntity otherTopicTest = testJpaRepository.saveAndFlush(testEntity(
            otherTopic.getId(),
            "Other topic test",
            TestType.TRAINING,
            ContentStatus.PUBLISHED,
            false,
            0
        ));

        QuestionEntity secondQuestion = questionJpaRepository.saveAndFlush(questionEntity(
            topic.getId(),
            "Second question",
            ContentStatus.PUBLISHED,
            3
        ));
        QuestionEntity firstQuestion = questionJpaRepository.saveAndFlush(questionEntity(
            topic.getId(),
            "First question",
            ContentStatus.PUBLISHED,
            1
        ));
        QuestionEntity draftQuestion = questionJpaRepository.saveAndFlush(questionEntity(
            topic.getId(),
            "Draft question",
            ContentStatus.DRAFT,
            2
        ));
        QuestionEntity otherTopicQuestion = questionJpaRepository.saveAndFlush(questionEntity(
            otherTopic.getId(),
            "Other topic question",
            ContentStatus.PUBLISHED,
            0
        ));

        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(otherTopicTest.getId(), otherTopicQuestion.getId(), 0, "1.00"));
        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(test.getId(), secondQuestion.getId(), 2, "1.50"));
        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(test.getId(), firstQuestion.getId(), 0, "2.00"));
        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(draftTest.getId(), draftQuestion.getId(), 0, "0.50"));

        answerOptionJpaRepository.saveAndFlush(answerOptionEntity(secondQuestion.getId(), "Other question option", 0));
        AnswerOptionEntity optionB = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(firstQuestion.getId(), "B", 2));
        AnswerOptionEntity optionA = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(firstQuestion.getId(), "A", 0));

        List<TestQuestion> testQuestions = testQuestionRepository.findTestQuestionsByTestId(test.getId());
        List<Question> publishedTopicQuestions = questionRepository.findQuestionsByTopicIdAndStatus(
            topic.getId(),
            ContentStatus.PUBLISHED
        );
        Question preservedDraftQuestion = questionRepository.findQuestionById(draftQuestion.getId());
        List<AnswerOption> answerOptions = answerOptionRepository.findAnswerOptionsByQuestionId(firstQuestion.getId());

        assertThat(testQuestions)
            .extracting(TestQuestion::questionId)
            .containsExactly(firstQuestion.getId(), secondQuestion.getId());
        assertThat(publishedTopicQuestions)
            .extracting(Question::id)
            .containsExactly(firstQuestion.getId(), secondQuestion.getId());
        assertThat(preservedDraftQuestion.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(answerOptions)
            .extracting(AnswerOption::id)
            .containsExactly(optionA.getId(), optionB.getId());
        assertThat(testQuestionRepository.existsPublishedTestUsingQuestion(firstQuestion.getId())).isTrue();
        assertThat(testQuestionRepository.existsPublishedTestUsingQuestion(draftQuestion.getId())).isFalse();
    }

    @Test
    void mapperRoundTripPreservesVisibilityRelevantFieldsUsedBySelfVisibleProjection() {
        com.vladislav.training.platform.content.domain.Test domainTest =
            new com.vladislav.training.platform.content.domain.Test(
            401L,
            301L,
            "Final control",
            "Description",
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            new BigDecimal("75.00"),
            "STANDARD",
            true,
            7,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        AnswerOption matchingAnswerOption = new AnswerOption(
            501L,
            601L,
            "Left pair",
            AnswerOptionRole.MATCH_LEFT,
            null,
            3,
            "PAIR-A",
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        AnswerOption orderingAnswerOption = new AnswerOption(
            502L,
            602L,
            "Ordered item",
            AnswerOptionRole.ORDER_ITEM,
            null,
            4,
            null,
            9,
            FIXED_INSTANT,
            FIXED_INSTANT
        );

        TestEntity mappedTestEntity = contentMapper.toEntity(domainTest);
        AnswerOptionEntity mappedMatchingAnswerOptionEntity = contentMapper.toEntity(matchingAnswerOption);
        AnswerOptionEntity mappedOrderingAnswerOptionEntity = contentMapper.toEntity(orderingAnswerOption);
        com.vladislav.training.platform.content.domain.Test remappedTest = contentMapper.toDomain(mappedTestEntity);
        AnswerOption remappedMatchingAnswerOption = contentMapper.toDomain(mappedMatchingAnswerOptionEntity);
        AnswerOption remappedOrderingAnswerOption = contentMapper.toDomain(mappedOrderingAnswerOptionEntity);

        assertThat(remappedTest)
            .extracting(
                com.vladislav.training.platform.content.domain.Test::id,
                com.vladislav.training.platform.content.domain.Test::topicId,
                com.vladislav.training.platform.content.domain.Test::status,
                com.vladislav.training.platform.content.domain.Test::testType,
                com.vladislav.training.platform.content.domain.Test::isActiveFinalForTopic,
                com.vladislav.training.platform.content.domain.Test::sortOrder
            )
            .containsExactly(401L, 301L, ContentStatus.PUBLISHED, TestType.CONTROL, true, 7);
        assertThat(remappedMatchingAnswerOption)
            .extracting(
                AnswerOption::id,
                AnswerOption::questionId,
                AnswerOption::answerOptionRole,
                AnswerOption::displayOrder,
                AnswerOption::pairingKey
            )
            .containsExactly(501L, 601L, AnswerOptionRole.MATCH_LEFT, 3, "PAIR-A");
        assertThat(remappedOrderingAnswerOption)
            .extracting(
                AnswerOption::id,
                AnswerOption::questionId,
                AnswerOption::answerOptionRole,
                AnswerOption::displayOrder,
                AnswerOption::canonicalOrderPosition
            )
            .containsExactly(502L, 602L, AnswerOptionRole.ORDER_ITEM, 4, 9);
    }

    private CourseEntity courseEntity(String name, ContentStatus status, Integer sortOrder) {
        CourseEntity entity = new CourseEntity();
        entity.setName(name);
        entity.setDescription("Course for self-visible persistence coverage");
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId, String name, ContentStatus status, int sortOrder) {
        TopicEntity entity = new TopicEntity();
        entity.setCourseId(courseId);
        entity.setName(name);
        entity.setDescription("Topic for self-visible persistence coverage");
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestEntity testEntity(
        Long topicId,
        String name,
        TestType testType,
        ContentStatus status,
        boolean activeFinalForTopic,
        int sortOrder
    ) {
        TestEntity entity = new TestEntity();
        entity.setTopicId(topicId);
        entity.setName(name);
        entity.setDescription("Test for self-visible persistence coverage");
        entity.setTestType(testType);
        entity.setStatus(status);
        entity.setThresholdPercent(new BigDecimal("70.00"));
        entity.setScoringPolicyCode("STANDARD");
        entity.setActiveFinalForTopic(activeFinalForTopic);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private QuestionEntity questionEntity(Long topicId, String body, ContentStatus status, Integer sortOrder) {
        QuestionEntity entity = new QuestionEntity();
        entity.setTopicId(topicId);
        entity.setBody(body);
        entity.setQuestionType(QuestionType.SINGLE_CHOICE);
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AnswerOptionEntity answerOptionEntity(Long questionId, String body, int displayOrder) {
        AnswerOptionEntity entity = new AnswerOptionEntity();
        entity.setQuestionId(questionId);
        entity.setBody(body);
        entity.setAnswerOptionRole(AnswerOptionRole.CHOICE_OPTION);
        entity.setIsCorrect(false);
        entity.setDisplayOrder(displayOrder);
        entity.setPairingKey(null);
        entity.setCanonicalOrderPosition(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestQuestionEntity testQuestionEntity(Long testId, Long questionId, int displayOrder, String weight) {
        TestQuestionEntity entity = new TestQuestionEntity();
        entity.setTestId(testId);
        entity.setQuestionId(questionId);
        entity.setDisplayOrder(displayOrder);
        entity.setWeight(new BigDecimal(weight));
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        CourseEntity.class,
        TopicEntity.class,
        MaterialEntity.class,
        QuestionEntity.class,
        AnswerOptionEntity.class,
        TestEntity.class,
        TestQuestionEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataMaterialJpaRepository.class,
        SpringDataQuestionJpaRepository.class,
        SpringDataAnswerOptionJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataTestQuestionJpaRepository.class
    })
    @Import({
        ContentMapper.class,
        JpaCourseRepositoryAdapter.class,
        JpaTopicRepositoryAdapter.class,
        JpaTestRepositoryAdapter.class,
        JpaQuestionRepositoryAdapter.class,
        JpaAnswerOptionRepositoryAdapter.class,
        JpaTestQuestionRepositoryAdapter.class
    })
    static class ContentPersistenceTestApplication {}
}
