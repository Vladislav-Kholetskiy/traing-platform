package com.vladislav.training.platform.testing.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.time.SystemUtcClock;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.infrastructure.persistence.AnswerOptionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.ContentMapper;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaAnswerOptionRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaCourseRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaMaterialRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaQuestionRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaTestQuestionRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaTestRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaTopicRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.MaterialEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.QuestionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataAnswerOptionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataMaterialJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataQuestionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestQuestionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TestQuestionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.testing.controller.SelfVisibleTestingReadController;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(
    classes = SelfVisibleTestingReadRuntimeIntegrationTest.SelfVisibleTestingReadRuntimeTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code SelfVisibleTestingReadRuntime} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
class SelfVisibleTestingReadRuntimeIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T12:00:00Z");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:self-visible-testing;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private SelfVisibleTestingReadController selfVisibleTestingReadController;
    @Autowired
    private UtcClock utcClock;
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
    private SpringDataMaterialJpaRepository materialJpaRepository;
    @Autowired
    private SpringDataTestQuestionJpaRepository testQuestionJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Autowired
    private AccessPolicyQueryContextResolver accessPolicyQueryContextResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createNoImplicitStartProbeTablesIfMissing();
        when(accessSpecificationPolicy.canRead(any())).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(selfVisibleTestingReadController)
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @AfterEach
    void tearDown() {
        reset(accessSpecificationPolicy, accessPolicyQueryContextResolver);
        answerOptionJpaRepository.deleteAllInBatch();
        testQuestionJpaRepository.deleteAllInBatch();
        questionJpaRepository.deleteAllInBatch();
        testJpaRepository.deleteAllInBatch();
        materialJpaRepository.deleteAllInBatch();
        topicJpaRepository.deleteAllInBatch();
        courseJpaRepository.deleteAllInBatch();
        truncateNoImplicitStartProbeTables();
    }

    @Test
    void selfVisibleCatalogReturnsOnlyPublishedNonFinalTestsAndLeavesCountsUntouched() throws Exception {
        CourseEntity publishedCourse = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        CourseEntity draftCourse = courseJpaRepository.saveAndFlush(courseEntity("Draft course", ContentStatus.DRAFT, 2));
        TopicEntity publishedTopic = topicJpaRepository.saveAndFlush(topicEntity(publishedCourse.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        TopicEntity draftTopic = topicJpaRepository.saveAndFlush(topicEntity(publishedCourse.getId(), "Draft topic", ContentStatus.DRAFT, 1));
        TopicEntity hiddenTopic = topicJpaRepository.saveAndFlush(topicEntity(draftCourse.getId(), "Hidden topic", ContentStatus.PUBLISHED, 0));

        testJpaRepository.saveAndFlush(testEntity(publishedTopic.getId(), "Visible self test", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0));
        testJpaRepository.saveAndFlush(testEntity(publishedTopic.getId(), "Final control", TestType.CONTROL, ContentStatus.PUBLISHED, true, 1));
        testJpaRepository.saveAndFlush(testEntity(draftTopic.getId(), "Draft topic test", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0));
        testJpaRepository.saveAndFlush(testEntity(hiddenTopic.getId(), "Draft course test", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0));

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].courseName").value("Published course"))
            .andExpect(jsonPath("$[0].topicName").value("Published topic"))
            .andExpect(jsonPath("$[0].name").value("Visible self test"))
            .andExpect(jsonPath("$[0].attemptId").doesNotExist())
            .andExpect(jsonPath("$[0].resultId").doesNotExist());
    }

    @Test
    void selfVisibleReadDoesNotMaterializeAttemptOrResultArtifactsWhenReturningVisibleProjection() throws Exception {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        TestEntity test = testJpaRepository.saveAndFlush(
            testEntity(topic.getId(), "Visible self test", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0)
        );
        QuestionEntity question = questionJpaRepository.saveAndFlush(questionEntity(topic.getId(), ContentStatus.PUBLISHED));
        answerOptionJpaRepository.saveAndFlush(answerOptionEntity(question.getId()));
        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(test.getId(), question.getId()));

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests/{testId}", test.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(test.getId()))
            .andExpect(jsonPath("$.questions.length()").value(1))
            .andExpect(jsonPath("$.attemptId").doesNotExist())
            .andExpect(jsonPath("$.resultId").doesNotExist());
    }

    @Test
    void selfVisibleReadFailClosedDenyDoesNotMaterializeAttemptOrResultArtifacts() throws Exception {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        TestEntity hiddenTest = testJpaRepository.saveAndFlush(
            testEntity(topic.getId(), "Hidden self test", TestType.TRAINING, ContentStatus.DRAFT, false, 0)
        );

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests/{testId}", hiddenTest.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Self-visible test not found: " + hiddenTest.getId()));
    }

    @Test
    void selfVisibleDetailFailClosedWhenTopicIsNotPublished() throws Exception {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        TopicEntity draftTopic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Draft topic", ContentStatus.DRAFT, 0));
        TestEntity test = testJpaRepository.saveAndFlush(
            testEntity(draftTopic.getId(), "Visible only by id", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0)
        );

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests/{testId}", test.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Self-visible topic not found: " + draftTopic.getId()));
    }

    @Test
    void selfVisibleCatalogPolicyDeniedDoesNotMaterializeAttemptAnswerOrResultArtifacts() throws Exception {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        testJpaRepository.saveAndFlush(testEntity(topic.getId(), "Visible self test", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0));

        when(accessPolicyQueryContextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.LIST,
            "self_visible_testing"
        )).thenReturn(selfVisiblePolicyContext(AccessReadType.LIST));
        when(accessSpecificationPolicy.canRead(any())).thenReturn(false);

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Actor is not authorized to read self-visible testing data"));
    }

    @Test
    void selfVisibleDetailPolicyDeniedDoesNotMaterializeAttemptAnswerOrResultArtifacts() throws Exception {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        TestEntity test = testJpaRepository.saveAndFlush(
            testEntity(topic.getId(), "Visible self test", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0)
        );

        when(accessPolicyQueryContextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.DETAIL,
            "self_visible_testing"
        )).thenReturn(selfVisiblePolicyContext(AccessReadType.DETAIL));
        when(accessSpecificationPolicy.canRead(any())).thenReturn(false);

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests/{testId}", test.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Actor is not authorized to read self-visible testing data"));
    }

    @Test
    void ordinarySelfActorCanReadSelfVisibleProjectionWhenOnlySelfVisibleContourIsAllowed() throws Exception {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        TestEntity test = testJpaRepository.saveAndFlush(
            testEntity(topic.getId(), "Visible self test", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0)
        );
        QuestionEntity question = questionJpaRepository.saveAndFlush(questionEntity(topic.getId(), ContentStatus.PUBLISHED));
        answerOptionJpaRepository.saveAndFlush(answerOptionEntity(question.getId()));
        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(test.getId(), question.getId()));

        List<AccessReadArea> observedContours = new ArrayList<>();
        when(accessPolicyQueryContextResolver.resolveActorSelfScope(any(), any(), anyString())).thenAnswer(invocation ->
            selfVisiblePolicyContext(invocation.getArgument(1))
        );
        when(accessPolicyQueryContextResolver.resolve(any(), any(), anyString())).thenAnswer(invocation -> new AccessPolicyQueryContext(
            101L,
            invocation.getArgument(0),
            invocation.getArgument(1),
            FIXED_INSTANT,
            null,
            null,
            invocation.getArgument(2)
        ));
        when(accessSpecificationPolicy.canRead(any())).thenAnswer(invocation -> {
            AccessPolicyQueryContext context = invocation.getArgument(0);
            observedContours.add(context.contour());
            return context.contour() == AccessReadArea.SELF_VISIBLE_TESTING;
        });

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests/{testId}", test.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(test.getId()))
            .andExpect(jsonPath("$.questions.length()").value(1));

        org.assertj.core.api.Assertions.assertThat(observedContours)
            
            .containsOnly(AccessReadArea.SELF_VISIBLE_TESTING);
    }

    @Test
    void selfVisibleAllowedPathMustNotBuildContentAuthoringContextsDownstream() throws Exception {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        testJpaRepository.saveAndFlush(
            testEntity(topic.getId(), "Visible self test", TestType.TRAINING, ContentStatus.PUBLISHED, false, 0)
        );

        when(accessPolicyQueryContextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.LIST,
            "self_visible_testing"
        )).thenReturn(selfVisiblePolicyContext(AccessReadType.LIST));
        when(accessPolicyQueryContextResolver.resolve(any(), any(), anyString())).thenAnswer(invocation -> new AccessPolicyQueryContext(
            101L,
            invocation.getArgument(0),
            invocation.getArgument(1),
            FIXED_INSTANT,
            null,
            null,
            invocation.getArgument(2)
        ));
        when(accessSpecificationPolicy.canRead(any())).thenAnswer(invocation ->
            ((AccessPolicyQueryContext) invocation.getArgument(0)).contour() == AccessReadArea.SELF_VISIBLE_TESTING
        );

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(accessPolicyQueryContextResolver, org.mockito.Mockito.never()).resolve(
            eq(AccessReadArea.CONTENT_AUTHORING),
            any(),
            anyString()
        );
    }

    @Test
    void selfVisibleTopicReadReturnsPublishedMaterialsWithoutMaterializingAttemptArtifacts() throws Exception {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("Published course", ContentStatus.PUBLISHED, 1));
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId(), "Published topic", ContentStatus.PUBLISHED, 0));
        materialJpaRepository.saveAndFlush(materialEntity(topic.getId(), "Guide", ContentStatus.PUBLISHED, 0));
        materialJpaRepository.saveAndFlush(materialEntity(topic.getId(), "Draft guide", ContentStatus.DRAFT, 1));

        assertReadLeavesCountsUntouched(get("/api/v1/self-testing/tests/topics/{topicId}", topic.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topicId").value(topic.getId()))
            .andExpect(jsonPath("$.topicName").value("Published topic"))
            .andExpect(jsonPath("$.courseName").value("Published course"))
            .andExpect(jsonPath("$.materials.length()").value(1))
            .andExpect(jsonPath("$.materials[0].name").value("Guide"))
            .andExpect(jsonPath("$.materials[0].materialType").value("TEXT"));
    }

    private org.springframework.test.web.servlet.ResultActions assertReadLeavesCountsUntouched(
        MockHttpServletRequestBuilder requestBuilder
    ) throws Exception {
        Map<String, Long> countsBefore = captureRelevantRowCounts();
        org.springframework.test.web.servlet.ResultActions result = mockMvc.perform(requestBuilder);
        org.assertj.core.api.Assertions.assertThat(captureRelevantRowCounts())
            
            .isEqualTo(countsBefore);
        return result;
    }

    private Map<String, Long> captureRelevantRowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("test", testJpaRepository.count());
        counts.put("question", questionJpaRepository.count());
        counts.put("answer_option", answerOptionJpaRepository.count());
        counts.put("test_question", testQuestionJpaRepository.count());
        counts.put("test_attempt", countRows("test_attempt"));
        counts.put("user_answer", countRows("user_answer"));
        counts.put("user_answer_item", countRows("user_answer_item"));
        counts.put("result", countRows("result"));
        counts.put("result_question_snapshot", countRows("result_question_snapshot"));
        counts.put("result_answer_option_snapshot", countRows("result_answer_option_snapshot"));
        return counts;
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private void createNoImplicitStartProbeTablesIfMissing() {
        jdbcTemplate.execute("create table if not exists test_attempt (id bigint generated by default as identity primary key)");
        jdbcTemplate.execute("create table if not exists user_answer (id bigint generated by default as identity primary key)");
        jdbcTemplate.execute("create table if not exists user_answer_item (id bigint generated by default as identity primary key)");
        jdbcTemplate.execute("create table if not exists result (id bigint generated by default as identity primary key)");
        jdbcTemplate.execute(
            "create table if not exists result_question_snapshot (id bigint generated by default as identity primary key)"
        );
        jdbcTemplate.execute(
            "create table if not exists result_answer_option_snapshot (id bigint generated by default as identity primary key)"
        );
    }

    private void truncateNoImplicitStartProbeTables() {
        jdbcTemplate.execute("truncate table test_attempt");
        jdbcTemplate.execute("truncate table user_answer");
        jdbcTemplate.execute("truncate table user_answer_item");
        jdbcTemplate.execute("truncate table result");
        jdbcTemplate.execute("truncate table result_question_snapshot");
        jdbcTemplate.execute("truncate table result_answer_option_snapshot");
    }

    private TestEntity testEntity(
        Long topicId,
        String name,
        TestType testType,
        ContentStatus status,
        boolean activeFinalForTopic,
        int sortOrder
    ) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName(name);
        entity.setDescription("Read-only self-entry prerequisite");
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

    private QuestionEntity questionEntity(Long topicId, ContentStatus status) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody("Question body");
        entity.setQuestionType(QuestionType.SINGLE_CHOICE);
        entity.setStatus(status);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private CourseEntity courseEntity(String name, ContentStatus status, Integer sortOrder) {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName(name);
        entity.setDescription("Read-only self-entry course");
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId, String name, ContentStatus status, int sortOrder) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName(name);
        entity.setDescription("Read-only self-entry topic");
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AnswerOptionEntity answerOptionEntity(Long questionId) {
        AnswerOptionEntity entity = instantiate(AnswerOptionEntity.class);
        entity.setQuestionId(questionId);
        entity.setBody("Option A");
        entity.setAnswerOptionRole(AnswerOptionRole.CHOICE_OPTION);
        entity.setIsCorrect(false);
        entity.setDisplayOrder(0);
        entity.setPairingKey(null);
        entity.setCanonicalOrderPosition(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestQuestionEntity testQuestionEntity(Long testId, Long questionId) {
        TestQuestionEntity entity = instantiate(TestQuestionEntity.class);
        entity.setTestId(testId);
        entity.setQuestionId(questionId);
        entity.setDisplayOrder(0);
        entity.setWeight(new BigDecimal("2.00"));
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private MaterialEntity materialEntity(Long topicId, String name, ContentStatus status, int sortOrder) {
        MaterialEntity entity = instantiate(MaterialEntity.class);
        entity.setTopicId(topicId);
        entity.setName(name);
        entity.setDescription("Read-only self-visible topic material");
        entity.setBody("Material body");
        entity.setVideoUrl(null);
        entity.setMaterialType(MaterialType.TEXT);
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity: " + type.getName(), exception);
        }
    }

    private AccessPolicyQueryContext selfVisiblePolicyContext(AccessReadType readType) {
        return new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_VISIBLE_TESTING,
            readType,
            FIXED_INSTANT,
            null,
            null,
            "self_visible_testing",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        CourseEntity.class,
        TopicEntity.class,
        MaterialEntity.class,
        TestEntity.class,
        QuestionEntity.class,
        AnswerOptionEntity.class,
        TestQuestionEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataMaterialJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataQuestionJpaRepository.class,
        SpringDataAnswerOptionJpaRepository.class,
        SpringDataTestQuestionJpaRepository.class
    })
    @Import({
        ContentMapper.class,
        JpaCourseRepositoryAdapter.class,
        JpaTopicRepositoryAdapter.class,
        JpaMaterialRepositoryAdapter.class,
        JpaTestRepositoryAdapter.class,
        JpaTestQuestionRepositoryAdapter.class,
        JpaQuestionRepositoryAdapter.class,
        JpaAnswerOptionRepositoryAdapter.class,
        SelfVisibleTestVisibilityFilter.class,
        RepositoryBackedSelfVisibleTestingProjectionReader.class,
        SelfVisibleTestingReadService.class,
        SelfVisibleTestingReadController.class,
        SystemUtcClock.class
    })
    static class SelfVisibleTestingReadRuntimeTestApplication {

        @Bean
        AccessSpecificationPolicy accessSpecificationPolicy() {
            return org.mockito.Mockito.mock(AccessSpecificationPolicy.class);
        }

        @Bean
        AccessPolicyQueryContextResolver accessPolicyQueryContextResolver() {
            return org.mockito.Mockito.mock(AccessPolicyQueryContextResolver.class);
        }
    }
}
