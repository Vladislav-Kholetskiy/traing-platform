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
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentAdministrativeActionEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentPersistenceMapper;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentTestEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.JpaAssignmentReadRepositoryAdapter;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentAdministrativeActionJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentTestJpaRepository;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.SystemUtcClock;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.QuestionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataQuestionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.testing.controller.CurrentAttemptReadController;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.infrastructure.persistence.JpaTestAttemptRepositoryAdapter;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataUserAnswerItemJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataUserAnswerJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestingPersistenceMapper;
import com.vladislav.training.platform.testing.infrastructure.persistence.UserAnswerEntity;
import com.vladislav.training.platform.testing.infrastructure.persistence.UserAnswerItemEntity;
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
    classes = AssignedCurrentAttemptReadNoSideEffectIntegrationTest.AssignedCurrentAttemptReadNoSideEffectTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code AssignedCurrentAttemptReadNoSideEffect} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
class AssignedCurrentAttemptReadNoSideEffectIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-06T09:00:00Z");
    private static final Instant SENTINEL_UPDATED_AT = Instant.parse("2026-05-06T08:00:00Z");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:assigned-current-attempt;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignJpaRepository;
    @Autowired
    private SpringDataAssignmentJpaRepository assignmentJpaRepository;
    @Autowired
    private SpringDataAssignmentTestJpaRepository assignmentTestJpaRepository;
    @Autowired
    private SpringDataAssignmentAdministrativeActionJpaRepository assignmentAdministrativeActionJpaRepository;
    @Autowired
    private SpringDataTestAttemptJpaRepository testAttemptJpaRepository;
    @Autowired
    private SpringDataUserAnswerJpaRepository userAnswerJpaRepository;
    @Autowired
    private SpringDataUserAnswerItemJpaRepository userAnswerItemJpaRepository;
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
        userAnswerItemJpaRepository.deleteAllInBatch();
        userAnswerJpaRepository.deleteAllInBatch();
        testAttemptJpaRepository.deleteAllInBatch();
        assignmentAdministrativeActionJpaRepository.deleteAllInBatch();
        assignmentTestJpaRepository.deleteAllInBatch();
        assignmentJpaRepository.deleteAllInBatch();
        assignmentCampaignJpaRepository.deleteAllInBatch();
        questionJpaRepository.deleteAllInBatch();
        testJpaRepository.deleteAllInBatch();
        topicJpaRepository.deleteAllInBatch();
        courseJpaRepository.deleteAllInBatch();
        truncateProbeTables();
    }

    @Test
    void assignedCurrentAttemptReadReturnsExistingAttemptWithoutMutatingOwnerFacts() throws Exception {
        AssignedFixture fixture = createAssignedFixture();
        seedProbeRows(fixture.attemptId(), fixture.questionId());
        when(accessPolicyQueryContextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(assignedCurrentAttemptContext(101L));
        when(accessPolicyQueryContextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            "assignment"
        )).thenReturn(assignmentContext(101L));

        assertReadLeavesProbeStateUntouched(get(
            "/api/v1/current-attempts/assigned/assignments/{assignmentId}/assignment-tests/{assignmentTestId}",
            fixture.assignmentId(),
            fixture.assignmentTestId()
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(fixture.attemptId()))
            .andExpect(jsonPath("$.userId").value(101))
            .andExpect(jsonPath("$.testId").value(fixture.testId()))
            .andExpect(jsonPath("$.assignmentTestId").value(fixture.assignmentTestId()))
            .andExpect(jsonPath("$.attemptMode").value("ASSIGNED"))
            .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    void assignedCurrentAttemptReadPolicyDenyDoesNotMutateOwnerFacts() throws Exception {
        AssignedFixture fixture = createAssignedFixture();
        seedProbeRows(fixture.attemptId(), fixture.questionId());
        when(accessPolicyQueryContextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(assignedCurrentAttemptContext(101L));
        when(accessSpecificationPolicy.canRead(any())).thenReturn(false);

        assertReadLeavesProbeStateUntouched(get(
            "/api/v1/current-attempts/assigned/assignments/{assignmentId}/assignment-tests/{assignmentTestId}",
            fixture.assignmentId(),
            fixture.assignmentTestId()
        ))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Actor is not authorized to read assigned current attempt"));
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
        state.put("assignment", snapshot("assignment", "updated_at"));
        state.put("assignment_test", snapshot("assignment_test", "updated_at"));
        state.put("test_attempt", snapshot("test_attempt", "updated_at"));
        state.put("user_answer", snapshot("user_answer", "updated_at"));
        state.put("user_answer_item", snapshot("user_answer_item", "updated_at"));
        state.put("result", snapshot("result", "updated_at"));
        state.put("audit_event", snapshot("audit_event", "created_at"));
        return state;
    }

    private TableProbeSnapshot snapshot(String tableName, String timestampColumn) {
        Long rowCount = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        Timestamp maxTimestamp = jdbcTemplate.queryForObject(
            "select max(" + timestampColumn + ") from " + tableName,
            Timestamp.class
        );
        return new TableProbeSnapshot(rowCount, maxTimestamp == null ? null : maxTimestamp.toInstant());
    }

    private void createProbeTablesIfMissing() {
        jdbcTemplate.execute(
            "create table if not exists result (" +
                "id bigint generated by default as identity primary key, " +
                "created_at timestamp, " +
                "updated_at timestamp)"
        );
        jdbcTemplate.execute(
            "create table if not exists audit_event (" +
                "id bigint generated by default as identity primary key, " +
                "created_at timestamp)"
        );
    }

    private void truncateProbeTables() {
        jdbcTemplate.execute("truncate table result");
        jdbcTemplate.execute("truncate table audit_event");
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
        jdbcTemplate.update(
            "insert into audit_event(created_at) values (?)",
            Timestamp.from(SENTINEL_UPDATED_AT)
        );
    }

    private AssignedFixture createAssignedFixture() {
        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicJpaRepository.saveAndFlush(topicEntity(course.getId()));
        TestEntity test = testJpaRepository.saveAndFlush(testEntity(topic.getId()));
        QuestionEntity question = questionJpaRepository.saveAndFlush(questionEntity(topic.getId()));
        AssignmentCampaignEntity campaign = assignmentCampaignJpaRepository.saveAndFlush(assignmentCampaignEntity());
        AssignmentEntity assignment = assignmentJpaRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), 101L, course.getId())
        );
        AssignmentTestEntity assignmentTest = assignmentTestJpaRepository.saveAndFlush(
            assignmentTestEntity(assignment.getId(), test.getId())
        );
        TestAttemptEntity attempt = testAttemptJpaRepository.saveAndFlush(
            activeAssignedAttemptEntity(101L, test.getId(), assignmentTest.getId())
        );
        return new AssignedFixture(
            assignment.getId(),
            assignmentTest.getId(),
            test.getId(),
            question.getId(),
            attempt.getId()
        );
    }

    private CourseEntity courseEntity() {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Assigned current attempt course");
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
        entity.setName("Assigned current attempt topic");
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
        entity.setName("Assigned current attempt test");
        entity.setDescription("Read-only test");
        entity.setTestType(TestType.CONTROL);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new BigDecimal("70.00"));
        entity.setScoringPolicyCode("STANDARD");
        entity.setActiveFinalForTopic(true);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private QuestionEntity questionEntity(Long topicId) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody("Assigned probe question");
        entity.setQuestionType(QuestionType.SINGLE_CHOICE);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignEntity assignmentCampaignEntity() {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Assigned current attempt campaign");
        entity.setDescription("Read-only campaign");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("UNIT-ASSIGNED-READ");
        entity.setSourceNameSnapshot("Operations");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentEntity assignmentEntity(Long campaignId, Long userId, Long courseId) {
        AssignmentEntity entity = instantiate(AssignmentEntity.class);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setStatus(AssignmentStatus.ASSIGNED);
        entity.setAssignedAt(FIXED_INSTANT.minusSeconds(600));
        entity.setDeadlineAt(FIXED_INSTANT.plusSeconds(3600));
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(120));
        return entity;
    }

    private AssignmentTestEntity assignmentTestEntity(Long assignmentId, Long testId) {
        AssignmentTestEntity entity = instantiate(AssignmentTestEntity.class);
        entity.setAssignmentId(assignmentId);
        entity.setTestId(testId);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(null);
        entity.setClosedAt(null);
        entity.setClosed(false);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(600));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(120));
        return entity;
    }

    private TestAttemptEntity activeAssignedAttemptEntity(Long userId, Long testId, Long assignmentTestId) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(AttemptMode.ASSIGNED);
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

    private AccessPolicyQueryContext assignedCurrentAttemptContext(Long actorUserId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assigned_current_attempt",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private AccessPolicyQueryContext assignmentContext(Long actorUserId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF
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

    private record AssignedFixture(
        Long assignmentId,
        Long assignmentTestId,
        Long testId,
        Long questionId,
        Long attemptId
    ) {
    }

    private record TableProbeSnapshot(Long rowCount, Instant maxTimestamp) {
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        CourseEntity.class,
        TopicEntity.class,
        TestEntity.class,
        QuestionEntity.class,
        AssignmentCampaignEntity.class,
        AssignmentEntity.class,
        AssignmentTestEntity.class,
        AssignmentAdministrativeActionEntity.class,
        TestAttemptEntity.class,
        UserAnswerEntity.class,
        UserAnswerItemEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataQuestionJpaRepository.class,
        SpringDataAssignmentCampaignJpaRepository.class,
        SpringDataAssignmentJpaRepository.class,
        SpringDataAssignmentTestJpaRepository.class,
        SpringDataAssignmentAdministrativeActionJpaRepository.class,
        SpringDataTestAttemptJpaRepository.class,
        SpringDataUserAnswerJpaRepository.class,
        SpringDataUserAnswerItemJpaRepository.class
    })
    @Import({
        AssignmentPersistenceMapper.class,
        TestingPersistenceMapper.class,
        JpaAssignmentReadRepositoryAdapter.class,
        JpaTestAttemptRepositoryAdapter.class,
        AssignedCurrentAttemptReadFoundationStateReadServiceImpl.class,
        ActiveAttemptOwnerLocalReadService.class,
        AssignedCurrentAttemptReadService.class,
        CurrentAttemptReadController.class,
        SystemUtcClock.class
    })
    static class AssignedCurrentAttemptReadNoSideEffectTestApplication {

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
        SelfCurrentAttemptReadService selfCurrentAttemptReadService() {
            return org.mockito.Mockito.mock(SelfCurrentAttemptReadService.class);
        }
    }
}
