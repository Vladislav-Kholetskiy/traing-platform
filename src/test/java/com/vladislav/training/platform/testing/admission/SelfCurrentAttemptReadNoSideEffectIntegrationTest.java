package com.vladislav.training.platform.testing.admission;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.SystemUtcClock;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.infrastructure.persistence.ContentMapper;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaCourseRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaQuestionRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaTestQuestionRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaTestRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.JpaTopicRepositoryAdapter;
import com.vladislav.training.platform.content.infrastructure.persistence.QuestionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataQuestionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestQuestionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TestQuestionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.service.CourseQueryServiceImpl;
import com.vladislav.training.platform.content.service.TestQueryServiceImpl;
import com.vladislav.training.platform.content.service.TopicQueryServiceImpl;
import com.vladislav.training.platform.testing.controller.CurrentAttemptReadController;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.infrastructure.persistence.JpaTestAttemptRepositoryAdapter;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestingPersistenceMapper;
import com.vladislav.training.platform.testing.service.ActiveAttemptOwnerLocalReadService;
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    classes = SelfCurrentAttemptReadNoSideEffectIntegrationTest.SelfCurrentAttemptReadNoSideEffectTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code SelfCurrentAttemptReadNoSideEffect} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
class SelfCurrentAttemptReadNoSideEffectIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-01T09:00:00Z");
    private static final Instant SENTINEL_UPDATED_AT = Instant.parse("2026-05-01T08:00:00Z");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:self-current-attempt;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private CurrentAttemptReadController currentAttemptReadController;
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
    private SpringDataTestQuestionJpaRepository testQuestionJpaRepository;
    @Autowired
    private SpringDataTestAttemptJpaRepository testAttemptJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Autowired
    private AccessPolicyQueryContextResolver accessPolicyQueryContextResolver;
    @Autowired
    private InteractiveActorResolver interactiveActorResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createProbeTablesIfMissing();
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(accessSpecificationPolicy.canRead(any())).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(currentAttemptReadController)
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @AfterEach
    void tearDown() {
        reset(accessSpecificationPolicy, accessPolicyQueryContextResolver, interactiveActorResolver);
        testAttemptJpaRepository.deleteAllInBatch();
        testQuestionJpaRepository.deleteAllInBatch();
        questionJpaRepository.deleteAllInBatch();
        testJpaRepository.deleteAllInBatch();
        topicJpaRepository.deleteAllInBatch();
        courseJpaRepository.deleteAllInBatch();
        truncateProbeTables();
    }

    @Test
    void selfCurrentAttemptReadReturnsExistingAttemptWithoutMutatingAttemptAnswerOrResultState() throws Exception {
        Long testId = createPublishedTestFixture();
        TestAttemptEntity attempt = testAttemptJpaRepository.saveAndFlush(activeSelfAttemptEntity(101L, testId));
        Long questionId = questionJpaRepository.saveAndFlush(questionEntity(findTopicIdByTestId(testId))).getId();
        seedProbeRows(attempt.getId(), questionId);
        when(accessPolicyQueryContextResolver.resolveSelfCurrentAttemptContext(101L, testId))
            .thenReturn(selfCurrentAttemptContext(101L, testId, attempt.getId()));

        assertReadLeavesProbeStateUntouched(get("/api/v1/current-attempts/self/tests/{testId}", testId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(attempt.getId()))
            .andExpect(jsonPath("$.userId").value(101))
            .andExpect(jsonPath("$.testId").value(testId))
            .andExpect(jsonPath("$.attemptMode").value("SELF"))
            .andExpect(jsonPath("$.status").value("STARTED"))
            .andExpect(jsonPath("$.assignmentTestId").isEmpty());
    }

    @Test
    void selfCurrentAttemptReadPolicyDenyDoesNotMutateAttemptAnswerOrResultState() throws Exception {
        Long testId = createPublishedTestFixture();
        TestAttemptEntity attempt = testAttemptJpaRepository.saveAndFlush(activeSelfAttemptEntity(101L, testId));
        Long questionId = questionJpaRepository.saveAndFlush(questionEntity(findTopicIdByTestId(testId))).getId();
        seedProbeRows(attempt.getId(), questionId);
        when(accessPolicyQueryContextResolver.resolveSelfCurrentAttemptContext(101L, testId))
            .thenReturn(selfCurrentAttemptContext(101L, testId, null));
        when(accessSpecificationPolicy.canRead(any())).thenReturn(false);

        assertReadLeavesProbeStateUntouched(get("/api/v1/current-attempts/self/tests/{testId}", testId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Actor is not authorized to read self current attempt"));
    }

    private org.springframework.test.web.servlet.ResultActions assertReadLeavesProbeStateUntouched(
        MockHttpServletRequestBuilder requestBuilder
    ) throws Exception {
        Map<String, TableProbeSnapshot> before = captureProbeState();
        org.springframework.test.web.servlet.ResultActions result = mockMvc.perform(requestBuilder);
        org.assertj.core.api.Assertions.assertThat(captureProbeState())
            
            .isEqualTo(before);
        return result;
    }

    private Map<String, TableProbeSnapshot> captureProbeState() {
        Map<String, TableProbeSnapshot> state = new LinkedHashMap<>();
        state.put("test_attempt", snapshot("test_attempt"));
        state.put("user_answer", snapshot("user_answer"));
        state.put("user_answer_item", snapshot("user_answer_item"));
        state.put("result", snapshot("result"));
        return state;
    }

    private TableProbeSnapshot snapshot(String tableName) {
        Long rowCount = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        Timestamp maxUpdatedAt = jdbcTemplate.queryForObject("select max(updated_at) from " + tableName, Timestamp.class);
        return new TableProbeSnapshot(rowCount, maxUpdatedAt == null ? null : maxUpdatedAt.toInstant());
    }

    private void createProbeTablesIfMissing() {
        jdbcTemplate.execute(
            "create table if not exists user_answer (" +
                "id bigint generated by default as identity primary key, " +
                "updated_at timestamp)"
        );
        jdbcTemplate.execute(
            "create table if not exists user_answer_item (" +
                "id bigint generated by default as identity primary key, " +
                "updated_at timestamp)"
        );
        jdbcTemplate.execute(
            "create table if not exists result (" +
                "id bigint generated by default as identity primary key, " +
                "created_at timestamp, " +
                "updated_at timestamp)"
        );
    }

    private void truncateProbeTables() {
        jdbcTemplate.execute("truncate table user_answer");
        jdbcTemplate.execute("truncate table user_answer_item");
        jdbcTemplate.execute("truncate table result");
    }

    private void seedProbeRows(Long testAttemptId, Long questionId) {
        jdbcTemplate.update(
            "insert into user_answer(test_attempt_id, question_id, created_at, updated_at) values (?, ?, ?, ?)",
            testAttemptId,
            questionId,
            Timestamp.from(SENTINEL_UPDATED_AT),
            Timestamp.from(SENTINEL_UPDATED_AT)
        );
        Long userAnswerId = jdbcTemplate.queryForObject("select max(id) from user_answer", Long.class);
        jdbcTemplate.update(
            "insert into user_answer_item(" +
                "user_answer_id, answer_option_id, left_answer_option_id, right_answer_option_id, user_order_position, created_at, updated_at" +
                ") values (?, ?, ?, ?, ?, ?, ?)",
            userAnswerId,
            null,
            null,
            null,
            null,
            Timestamp.from(SENTINEL_UPDATED_AT),
            Timestamp.from(SENTINEL_UPDATED_AT)
        );
        jdbcTemplate.update(
            "insert into result(created_at, updated_at) values (?, ?)",
            Timestamp.from(SENTINEL_UPDATED_AT),
            Timestamp.from(SENTINEL_UPDATED_AT)
        );
    }

    private Long createPublishedTestFixture() {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId()));
        return testJpaRepository.saveAndFlush(testEntity(topic.getId())).getId();
    }

    private CourseEntity courseEntity() {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Self current attempt course");
        entity.setDescription("Read-only course");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName("Self current attempt topic");
        entity.setDescription("Read-only topic");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestEntity testEntity(Long topicId) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName("Self current attempt test");
        entity.setDescription("Read-only test");
        entity.setTestType(TestType.TRAINING);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new BigDecimal("70.00"));
        entity.setScoringPolicyCode("STANDARD");
        entity.setActiveFinalForTopic(false);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private QuestionEntity questionEntity(Long topicId) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody("Probe question");
        entity.setQuestionType(QuestionType.SINGLE_CHOICE);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestAttemptEntity activeSelfAttemptEntity(Long userId, Long testId) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(null);
        entity.setAttemptMode(AttemptMode.SELF);
        entity.setStatus(TestAttemptStatus.STARTED);
        entity.setStartedAt(FIXED_INSTANT.minusSeconds(600));
        entity.setCompletedAt(null);
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(FIXED_INSTANT.minusSeconds(60));
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(60));
        return entity;
    }

    private AccessPolicyQueryContext selfCurrentAttemptContext(Long actorUserId, Long testId, Long currentAttemptId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_current_attempt",
            testId,
            currentAttemptId,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
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

    private Long findTopicIdByTestId(Long testId) {
        return testJpaRepository.findById(testId)
            .orElseThrow(() -> new IllegalStateException("Test fixture not found: " + testId))
            .getTopicId();
    }

    private record TableProbeSnapshot(Long rowCount, Instant maxUpdatedAt) {
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        CourseEntity.class,
        TopicEntity.class,
        TestEntity.class,
        QuestionEntity.class,
        TestQuestionEntity.class,
        TestAttemptEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataQuestionJpaRepository.class,
        SpringDataTestQuestionJpaRepository.class,
        SpringDataTestAttemptJpaRepository.class
    })
    @Import({
        ContentMapper.class,
        TestingPersistenceMapper.class,
        JpaCourseRepositoryAdapter.class,
        JpaTopicRepositoryAdapter.class,
        JpaTestRepositoryAdapter.class,
        JpaQuestionRepositoryAdapter.class,
        JpaTestQuestionRepositoryAdapter.class,
        JpaTestAttemptRepositoryAdapter.class,
        CourseQueryServiceImpl.class,
        TopicQueryServiceImpl.class,
        TestQueryServiceImpl.class,
        SelfCurrentAttemptReadFoundationStateReadServiceImpl.class,
        ActiveAttemptOwnerLocalReadService.class,
        SelfCurrentAttemptReadService.class,
        CurrentAttemptReadController.class,
        SystemUtcClock.class
    })
    static class SelfCurrentAttemptReadNoSideEffectTestApplication {

        @Bean
        AccessSpecificationPolicy accessSpecificationPolicy() {
            return org.mockito.Mockito.mock(AccessSpecificationPolicy.class);
        }

        @Bean
        AccessPolicyQueryContextResolver accessPolicyQueryContextResolver() {
            return org.mockito.Mockito.mock(AccessPolicyQueryContextResolver.class);
        }

        @Bean
        InteractiveActorResolver interactiveActorResolver() {
            return org.mockito.Mockito.mock(InteractiveActorResolver.class);
        }

        @Bean
        AssignedCurrentAttemptReadService assignedCurrentAttemptReadService() {
            return org.mockito.Mockito.mock(AssignedCurrentAttemptReadService.class);
        }
    }
}
