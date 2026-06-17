package com.vladislav.training.platform.assignment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.JpaAccessSpecificationPolicy;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.controller.AssignmentSelfScopedReadController;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentPersistenceMapper;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentTestEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.JpaAssignmentReadRepositoryAdapter;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentTestJpaRepository;
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
import com.vladislav.training.platform.content.service.PublishedCourseLearningContextReader;
import com.vladislav.training.platform.content.service.CourseQueryService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(
    classes = AssignmentSelfScopedReadRuntimeIntegrationTest.AssignmentSelfScopedReadRuntimeTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code AssignmentSelfScopedReadRuntime} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
class AssignmentSelfScopedReadRuntimeIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T12:00:00Z");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:assignment-self-scoped;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private AssignmentSelfScopedReadController assignmentSelfScopedReadController;
    @Autowired
    private UtcClock utcClock;
    @Autowired
    private SpringDataAssignmentJpaRepository assignmentJpaRepository;
    @Autowired
    private SpringDataAssignmentTestJpaRepository assignmentTestJpaRepository;
    @Autowired
    private SpringDataCourseJpaRepository courseJpaRepository;
    @Autowired
    private SpringDataTopicJpaRepository topicJpaRepository;
    @Autowired
    private SpringDataMaterialJpaRepository materialJpaRepository;
    @Autowired
    private SpringDataTestJpaRepository testJpaRepository;
    @Autowired
    private SpringDataQuestionJpaRepository questionJpaRepository;
    @Autowired
    private SpringDataAnswerOptionJpaRepository answerOptionJpaRepository;
    @Autowired
    private SpringDataTestQuestionJpaRepository testQuestionJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Autowired
    private AccessFoundationStateReadService accessFoundationStateReadService;

    private MockMvc mockMvc;
    private MockMvc routingOnlyMockMvc;

    @BeforeEach
    void setUp() {
        createNoSideEffectProbeTablesIfMissing();
        mockMvc = MockMvcBuilders.standaloneSetup(assignmentSelfScopedReadController)
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
        routingOnlyMockMvc = MockMvcBuilders.standaloneSetup(assignmentSelfScopedReadController)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(userOrgFoundationStateReadService, accessFoundationStateReadService);
        truncateNoSideEffectProbeTables();
        assignmentTestJpaRepository.deleteAllInBatch();
        assignmentJpaRepository.deleteAllInBatch();
        testQuestionJpaRepository.deleteAllInBatch();
        answerOptionJpaRepository.deleteAllInBatch();
        questionJpaRepository.deleteAllInBatch();
        testJpaRepository.deleteAllInBatch();
        materialJpaRepository.deleteAllInBatch();
        topicJpaRepository.deleteAllInBatch();
        courseJpaRepository.deleteAllInBatch();
    }

    @Test
    void selfScopedListReturnsOnlyAuthenticatedActorsAssignmentsEndToEnd() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        CourseEntity ownedFirstCourse = courseJpaRepository.saveAndFlush(courseEntity("Owned First", ContentStatus.PUBLISHED));
        CourseEntity foreignCourse = courseJpaRepository.saveAndFlush(courseEntity("Foreign", ContentStatus.PUBLISHED));
        CourseEntity ownedSecondCourse = courseJpaRepository.saveAndFlush(courseEntity("Owned Second", ContentStatus.PUBLISHED));
        AssignmentEntity ownedFirst = assignmentJpaRepository.saveAndFlush(
            assignmentEntity(11L, 101L, ownedFirstCourse.getId())
        );
        AssignmentEntity foreign = assignmentJpaRepository.saveAndFlush(assignmentEntity(12L, 202L, foreignCourse.getId()));
        AssignmentEntity ownedSecond = assignmentJpaRepository.saveAndFlush(
            assignmentEntity(13L, 101L, ownedSecondCourse.getId())
        );

        mockMvc.perform(get("/api/v1/assigned-learning/assignments").queryParam("userId", "202"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(ownedFirst.getId()))
            .andExpect(jsonPath("$[0].userId").value(101))
            .andExpect(jsonPath("$[1].id").value(ownedSecond.getId()))
            .andExpect(jsonPath("$[1].userId").value(101))
            .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(foreign.getId().intValue()))));
    }

    @Test
    void validOperatorLearnerCanReadOnlyOwnedAssignmentsThroughSelfScopedEndpoint() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L, Set.of("OPERATOR"));

        CourseEntity ownedFirstCourse = courseJpaRepository.saveAndFlush(courseEntity("Owned First", ContentStatus.PUBLISHED));
        CourseEntity foreignCourse = courseJpaRepository.saveAndFlush(courseEntity("Foreign", ContentStatus.PUBLISHED));
        CourseEntity ownedSecondCourse = courseJpaRepository.saveAndFlush(courseEntity("Owned Second", ContentStatus.PUBLISHED));
        AssignmentEntity ownedFirst = assignmentJpaRepository.saveAndFlush(
            assignmentEntity(111L, 101L, ownedFirstCourse.getId())
        );
        AssignmentEntity foreign = assignmentJpaRepository.saveAndFlush(assignmentEntity(112L, 202L, foreignCourse.getId()));
        AssignmentEntity ownedSecond = assignmentJpaRepository.saveAndFlush(
            assignmentEntity(113L, 101L, ownedSecondCourse.getId())
        );

        mockMvc.perform(get("/api/v1/assigned-learning/assignments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(ownedFirst.getId()))
            .andExpect(jsonPath("$[0].userId").value(101))
            .andExpect(jsonPath("$[1].id").value(ownedSecond.getId()))
            .andExpect(jsonPath("$[1].userId").value(101))
            .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(foreign.getId().intValue()))));
    }

    @Test
    void selfScopedDetailAllowsOwnAssignmentButKeepsForeignAssignmentOwnerSafe() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        CourseEntity ownedCourse = courseJpaRepository.saveAndFlush(courseEntity("Owned Detail", ContentStatus.PUBLISHED));
        CourseEntity foreignCourse = courseJpaRepository.saveAndFlush(courseEntity("Foreign Detail", ContentStatus.PUBLISHED));
        AssignmentEntity owned = assignmentJpaRepository.saveAndFlush(assignmentEntity(21L, 101L, ownedCourse.getId()));
        AssignmentEntity foreign = assignmentJpaRepository.saveAndFlush(
            assignmentEntity(22L, 202L, foreignCourse.getId())
        );

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/{assignmentId}", owned.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(owned.getId()))
            .andExpect(jsonPath("$.userId").value(101));

        mockMvc.perform(get("/api/v1/assigned-learning/assignments/{assignmentId}", foreign.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Assignment not found in self scope: actorUserId=101, assignmentId=" + foreign.getId()
            ));
    }

    @Test
    void assignedLearningContextReturnsOwnedAssignmentWithSubordinateAssignmentTestsEndToEnd() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        CourseEntity publishedCourse = courseJpaRepository.saveAndFlush(courseEntity("Course", ContentStatus.PUBLISHED));
        TopicEntity publishedTopic = topicJpaRepository.saveAndFlush(topicEntity(publishedCourse.getId(), "Topic", ContentStatus.PUBLISHED));
        materialJpaRepository.saveAndFlush(materialEntity(publishedTopic.getId(), "Material", ContentStatus.PUBLISHED));
        AssignmentEntity owned = assignmentJpaRepository.saveAndFlush(assignmentEntity(21L, 101L, publishedCourse.getId()));
        TestEntity finalTest = testJpaRepository.saveAndFlush(
            testEntity(publishedTopic.getId(), "Final topic test")
        );
        TestEntity secondaryTest = testJpaRepository.saveAndFlush(
            testEntity(publishedTopic.getId(), "Secondary topic test")
        );
        assignmentTestJpaRepository.saveAndFlush(assignmentTestEntity(owned.getId(), finalTest.getId(), null, false));
        assignmentTestJpaRepository.saveAndFlush(assignmentTestEntity(owned.getId(), secondaryTest.getId(), 9002L, true));
        mockMvc.perform(get("/api/v1/assigned-learning/assignments/{assignmentId}/learning-context", owned.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignment.id").value(owned.getId()))
            .andExpect(jsonPath("$.assignment.userId").value(101))
            .andExpect(jsonPath("$.assignmentTests.length()").value(2))
            .andExpect(jsonPath("$.assignmentTests[0].id").isNumber())
            .andExpect(jsonPath("$.assignmentTests[0].assignmentId").value(owned.getId()))
            .andExpect(jsonPath("$.assignmentTests[0].testName").value("Final topic test"))
            .andExpect(jsonPath("$.assignmentTests[0].topicName").value("Topic"))
            .andExpect(jsonPath("$.assignmentTests[1].countedResultId").value(9002))
            .andExpect(jsonPath("$.assignmentTests[1].isClosed").value(true))
            .andExpect(jsonPath("$.publishedCourse.id").value(publishedCourse.getId()))
            .andExpect(jsonPath("$.publishedCourse.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.publishedTopics.length()").value(1))
            .andExpect(jsonPath("$.publishedTopics[0].id").value(publishedTopic.getId()))
            .andExpect(jsonPath("$.publishedMaterials.length()").value(1))
            .andExpect(jsonPath("$.publishedMaterials[0].topicId").value(publishedTopic.getId()))
            .andExpect(jsonPath("$.publishedMaterials[0].status").value("PUBLISHED"));
    }

    @Test
    void oldBroadAssignmentsRootIsAbsentAtRuntime() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        routingOnlyMockMvc.perform(get("/api/v1/assignments"))
            .andExpect(status().isNotFound());
    }

    @Test
    void selfScopedListReadDoesNotMaterializeAttemptOrResultArtifactsOrMutateOwnerFacts() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("No Side Effects List", ContentStatus.PUBLISHED));
        assignmentJpaRepository.saveAndFlush(assignmentEntity(31L, 101L, course.getId()));

        assertReadLeavesCountsUntouched(get("/api/v1/assigned-learning/assignments"))
            .andExpect(status().isOk());
    }

    @Test
    void selfScopedDetailReadDoesNotMaterializeAttemptOrResultArtifactsOrMutateOwnerFacts() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        CourseEntity course = courseJpaRepository.saveAndFlush(courseEntity("No Side Effects Detail", ContentStatus.PUBLISHED));
        AssignmentEntity owned = assignmentJpaRepository.saveAndFlush(assignmentEntity(32L, 101L, course.getId()));

        assertReadLeavesCountsUntouched(get("/api/v1/assigned-learning/assignments/{assignmentId}", owned.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(owned.getId()));
    }

    @Test
    void assignedLearningContextReadDoesNotMaterializeAttemptOrResultArtifactsOrMutateOwnerFacts() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        CourseEntity publishedCourse = courseJpaRepository.saveAndFlush(courseEntity("Course", ContentStatus.PUBLISHED));
        TopicEntity publishedTopic = topicJpaRepository.saveAndFlush(topicEntity(publishedCourse.getId(), "Topic", ContentStatus.PUBLISHED));
        materialJpaRepository.saveAndFlush(materialEntity(publishedTopic.getId(), "Material", ContentStatus.PUBLISHED));
        AssignmentEntity owned = assignmentJpaRepository.saveAndFlush(assignmentEntity(33L, 101L, publishedCourse.getId()));
        TestEntity assignedTest = testJpaRepository.saveAndFlush(testEntity(publishedTopic.getId(), "Assigned context test"));
        assignmentTestJpaRepository.saveAndFlush(assignmentTestEntity(owned.getId(), assignedTest.getId(), null, false));

        assertReadLeavesCountsUntouched(
            get("/api/v1/assigned-learning/assignments/{assignmentId}/learning-context", owned.getId())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignment.id").value(owned.getId()))
            .andExpect(jsonPath("$.assignmentTests.length()").value(1))
            .andExpect(jsonPath("$.publishedCourse.id").value(publishedCourse.getId()))
            .andExpect(jsonPath("$.publishedTopics.length()").value(1))
            .andExpect(jsonPath("$.publishedMaterials.length()").value(1));
    }

    @Test
    void assignedTestContextReturnsOwnedAssignmentBoundCompositionInStableOrderWithoutCorrectnessLeak() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        AssignmentEntity owned = assignmentJpaRepository.saveAndFlush(assignmentEntity(41L, 101L, 700L));
        TestEntity test = testJpaRepository.saveAndFlush(testEntity(801L, "Assigned final test"));
        AssignmentTestEntity assignmentTest = assignmentTestJpaRepository.saveAndFlush(
            assignmentTestEntity(owned.getId(), test.getId(), null, false)
        );
        QuestionEntity secondQuestion = questionJpaRepository.saveAndFlush(questionEntity(901L, "Question B"));
        QuestionEntity firstQuestion = questionJpaRepository.saveAndFlush(questionEntity(901L, "Question A"));
        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(test.getId(), secondQuestion.getId(), 2));
        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(test.getId(), firstQuestion.getId(), 0));
        answerOptionJpaRepository.saveAndFlush(answerOptionEntity(firstQuestion.getId(), "Option B", 2));
        answerOptionJpaRepository.saveAndFlush(answerOptionEntity(firstQuestion.getId(), "Option A", 0));

        mockMvc.perform(get(
                "/api/v1/assigned-learning/assignments/{assignmentId}/assignment-tests/{assignmentTestId}/test-context",
                owned.getId(),
                assignmentTest.getId()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignmentId").value(owned.getId()))
            .andExpect(jsonPath("$.assignmentTestId").value(assignmentTest.getId()))
            .andExpect(jsonPath("$.testId").value(test.getId()))
            .andExpect(jsonPath("$.testName").value("Assigned final test"))
            .andExpect(jsonPath("$.questions.length()").value(2))
            .andExpect(jsonPath("$.questions[0].body").value("Question A"))
            .andExpect(jsonPath("$.questions[0].displayOrder").value(0))
            .andExpect(jsonPath("$.questions[0].answerOptions[0].body").value("Option A"))
            .andExpect(jsonPath("$.questions[0].answerOptions[0].displayOrder").value(0))
            .andExpect(jsonPath("$.questions[0].answerOptions[1].body").value("Option B"))
            .andExpect(jsonPath("$.questions[1].body").value("Question B"))
            .andExpect(jsonPath("$.questions[0].answerOptions[0].isCorrect").doesNotExist())
            .andExpect(jsonPath("$.questions[0].answerOptions[0].correct").doesNotExist())
            .andExpect(jsonPath("$.questions[0].weight").doesNotExist())
            .andExpect(jsonPath("$.scoringPolicyCode").doesNotExist());
    }

    @Test
    void foreignAssignmentTestContextDoesNotMaterializeOwnedOrTestingArtifacts() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        AssignmentEntity foreign = assignmentJpaRepository.saveAndFlush(assignmentEntity(42L, 202L, 701L));
        TestEntity test = testJpaRepository.saveAndFlush(testEntity(802L, "Foreign test"));
        AssignmentTestEntity foreignAssignmentTest = assignmentTestJpaRepository.saveAndFlush(
            assignmentTestEntity(foreign.getId(), test.getId(), null, false)
        );

        assertReadLeavesCountsUntouched(get(
            "/api/v1/assigned-learning/assignments/{assignmentId}/assignment-tests/{assignmentTestId}/test-context",
            foreign.getId(),
            foreignAssignmentTest.getId()
        ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Assignment not found in self scope: actorUserId=101, assignmentId=" + foreign.getId()
            ));
    }

    @Test
    void assignedTestContextRequiresAssignmentTestToBelongToSpecifiedAssignment() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        AssignmentEntity owned = assignmentJpaRepository.saveAndFlush(assignmentEntity(43L, 101L, 702L));
        AssignmentEntity otherOwned = assignmentJpaRepository.saveAndFlush(assignmentEntity(44L, 101L, 703L));
        TestEntity test = testJpaRepository.saveAndFlush(testEntity(803L, "Mismatch test"));
        AssignmentTestEntity foreignWithinActorScope = assignmentTestJpaRepository.saveAndFlush(
            assignmentTestEntity(otherOwned.getId(), test.getId(), null, false)
        );

        assertReadLeavesCountsUntouched(get(
            "/api/v1/assigned-learning/assignments/{assignmentId}/assignment-tests/{assignmentTestId}/test-context",
            owned.getId(),
            foreignWithinActorScope.getId()
        ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Assignment test not found in self-scoped assignment context: assignmentId="
                    + owned.getId()
                    + ", assignmentTestId="
                    + foreignWithinActorScope.getId()
            ));
    }

    @Test
    void assignedTestContextReadDoesNotMaterializeAttemptOrResultArtifactsOrMutateOwnerFacts() throws Exception {
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        allowAssignmentReadForActor(101L);

        AssignmentEntity owned = assignmentJpaRepository.saveAndFlush(assignmentEntity(45L, 101L, 704L));
        TestEntity test = testJpaRepository.saveAndFlush(testEntity(804L, "No side effects test"));
        AssignmentTestEntity assignmentTest = assignmentTestJpaRepository.saveAndFlush(
            assignmentTestEntity(owned.getId(), test.getId(), null, false)
        );
        QuestionEntity question = questionJpaRepository.saveAndFlush(questionEntity(902L, "Question"));
        testQuestionJpaRepository.saveAndFlush(testQuestionEntity(test.getId(), question.getId(), 0));
        answerOptionJpaRepository.saveAndFlush(answerOptionEntity(question.getId(), "Option", 0));

        assertReadLeavesCountsUntouched(get(
            "/api/v1/assigned-learning/assignments/{assignmentId}/assignment-tests/{assignmentTestId}/test-context",
            owned.getId(),
            assignmentTest.getId()
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignmentTestId").value(assignmentTest.getId()))
            .andExpect(jsonPath("$.questions.length()").value(1));
    }

    private void allowAssignmentReadForActor(Long actorUserId) {
        allowAssignmentReadForActor(actorUserId, Set.of("OPERATOR"));
    }

    private void allowAssignmentReadForActor(Long actorUserId, Set<String> roleCodes) {
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(eq(actorUserId), any(Instant.class)))
            .thenReturn(new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(actorUserId, true, roleCodes));
    }

    private void setAuthentication(TestingAuthenticationToken authentication) {
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private TestingAuthenticationToken authenticatedToken(Long userId, String... authorities) {
        return new TestingAuthenticationToken(userId, null, authorities);
    }

    private AssignmentEntity assignmentEntity(Long campaignId, Long userId, Long courseId) {
        AssignmentEntity entity = instantiate(AssignmentEntity.class);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setStatus(AssignmentStatus.ASSIGNED);
        entity.setAssignedAt(FIXED_INSTANT);
        entity.setDeadlineAt(FIXED_INSTANT.plusSeconds(86400));
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentTestEntity assignmentTestEntity(Long assignmentId, Long testId, Long countedResultId, boolean closed) {
        AssignmentTestEntity entity = instantiate(AssignmentTestEntity.class);
        entity.setAssignmentId(assignmentId);
        entity.setTestId(testId);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(countedResultId);
        entity.setClosed(closed);
        entity.setClosedAt(closed ? FIXED_INSTANT : null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private CourseEntity courseEntity(String name, ContentStatus status) {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName(name);
        entity.setDescription("Published course");
        entity.setStatus(status);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId, String name, ContentStatus status) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName(name);
        entity.setDescription("Published topic");
        entity.setStatus(status);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private MaterialEntity materialEntity(Long topicId, String name, ContentStatus status) {
        MaterialEntity entity = instantiate(MaterialEntity.class);
        entity.setTopicId(topicId);
        entity.setName(name);
        entity.setDescription("Published material");
        entity.setMaterialType(MaterialType.TEXT);
        entity.setStatus(status);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestEntity testEntity(Long topicId, String name) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName(name);
        entity.setDescription("Assigned test description");
        entity.setTestType(TestType.CONTROL);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new java.math.BigDecimal("70.00"));
        entity.setScoringPolicyCode("STANDARD");
        entity.setActiveFinalForTopic(false);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestEntity testEntity(Long id, Long topicId, String name, ContentStatus ignoredStatus) {
        TestEntity entity = testEntity(topicId, name);
        entity.setId(id);
        return entity;
    }

    private QuestionEntity questionEntity(Long topicId, String body) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody(body);
        entity.setQuestionType(QuestionType.SINGLE_CHOICE);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AnswerOptionEntity answerOptionEntity(Long questionId, String body, int displayOrder) {
        AnswerOptionEntity entity = instantiate(AnswerOptionEntity.class);
        entity.setQuestionId(questionId);
        entity.setBody(body);
        entity.setAnswerOptionRole(AnswerOptionRole.CHOICE_OPTION);
        entity.setIsCorrect(displayOrder == 0);
        entity.setDisplayOrder(displayOrder);
        entity.setPairingKey(null);
        entity.setCanonicalOrderPosition(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestQuestionEntity testQuestionEntity(Long testId, Long questionId, int displayOrder) {
        TestQuestionEntity entity = instantiate(TestQuestionEntity.class);
        entity.setTestId(testId);
        entity.setQuestionId(questionId);
        entity.setDisplayOrder(displayOrder);
        entity.setWeight(new java.math.BigDecimal("1.00"));
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

    private org.springframework.test.web.servlet.ResultActions assertReadLeavesCountsUntouched(
        MockHttpServletRequestBuilder requestBuilder
    ) throws Exception {
        Map<String, Long> countsBefore = captureRelevantRowCounts();
        org.springframework.test.web.servlet.ResultActions result = mockMvc.perform(requestBuilder);
        assertRowCountsUnchanged(countsBefore);
        return result;
    }

    private Map<String, Long> captureRelevantRowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("assignment", assignmentJpaRepository.count());
        counts.put("assignment_test", assignmentTestJpaRepository.count());
        counts.put("test_attempt", countRows("test_attempt"));
        counts.put("user_answer", countRows("user_answer"));
        counts.put("user_answer_item", countRows("user_answer_item"));
        counts.put("result", countRows("result"));
        counts.put("result_question_snapshot", countRows("result_question_snapshot"));
        counts.put("result_answer_option_snapshot", countRows("result_answer_option_snapshot"));
        return counts;
    }

    private void assertRowCountsUnchanged(Map<String, Long> countsBefore) {
        org.assertj.core.api.Assertions.assertThat(captureRelevantRowCounts())
            
            .isEqualTo(countsBefore);
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private void createNoSideEffectProbeTablesIfMissing() {
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

    private void truncateNoSideEffectProbeTables() {
        jdbcTemplate.execute("truncate table test_attempt");
        jdbcTemplate.execute("truncate table user_answer");
        jdbcTemplate.execute("truncate table user_answer_item");
        jdbcTemplate.execute("truncate table result");
        jdbcTemplate.execute("truncate table result_question_snapshot");
        jdbcTemplate.execute("truncate table result_answer_option_snapshot");
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AssignmentEntity.class,
        CourseEntity.class,
        TestEntity.class,
        QuestionEntity.class,
        AnswerOptionEntity.class,
        TestQuestionEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAssignmentJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataQuestionJpaRepository.class,
        SpringDataAnswerOptionJpaRepository.class,
        SpringDataTestQuestionJpaRepository.class
    })
    @Import({
        AssignmentPersistenceMapper.class,
        JpaAssignmentReadRepositoryAdapter.class,
        ContentMapper.class,
        JpaCourseRepositoryAdapter.class,
        JpaTopicRepositoryAdapter.class,
        JpaMaterialRepositoryAdapter.class,
        JpaTestRepositoryAdapter.class,
        JpaQuestionRepositoryAdapter.class,
        JpaAnswerOptionRepositoryAdapter.class,
        JpaTestQuestionRepositoryAdapter.class,
        PublishedCourseLearningContextReader.class,
        RepositoryBackedAssignedTestContextProjectionReader.class,
        AssignmentSelfScopedQueryServiceImpl.class,
        AssignmentSelfScopedReadController.class,
        AuthenticatedActorAdapter.class,
        InteractiveActorResolver.class,
        AccessPolicyQueryContextResolver.class,
        JpaAccessSpecificationPolicy.class,
        SystemUtcClock.class
    })
    static class AssignmentSelfScopedReadRuntimeTestApplication {

        @Bean
        UserOrgFoundationStateReadService userOrgFoundationStateReadService() {
            return org.mockito.Mockito.mock(UserOrgFoundationStateReadService.class);
        }

        @Bean
        AccessFoundationStateReadService accessFoundationStateReadService() {
            return org.mockito.Mockito.mock(AccessFoundationStateReadService.class);
        }

        @Bean
        CourseQueryService courseQueryService(
            SpringDataCourseJpaRepository courseJpaRepository,
            ContentMapper contentMapper
        ) {
            return new RepositoryBackedCourseQueryService(courseJpaRepository, contentMapper);
        }
    }

    private static final class RepositoryBackedCourseQueryService implements CourseQueryService {

        private final SpringDataCourseJpaRepository courseJpaRepository;
        private final ContentMapper contentMapper;

        private RepositoryBackedCourseQueryService(
            SpringDataCourseJpaRepository courseJpaRepository,
            ContentMapper contentMapper
        ) {
            this.courseJpaRepository = courseJpaRepository;
            this.contentMapper = contentMapper;
        }

        @Override
        public com.vladislav.training.platform.content.domain.Course findCourseById(Long courseId) {
            return courseJpaRepository.findById(courseId)
                .map(contentMapper::toDomain)
                .orElseThrow(() -> new com.vladislav.training.platform.common.exception.NotFoundException(
                    "Course not found: id=" + courseId
                ));
        }

        @Override
        public java.util.List<com.vladislav.training.platform.content.domain.Course> findAllCourses() {
            return courseJpaRepository.findAll().stream()
                .map(contentMapper::toDomain)
                .toList();
        }

        @Override
        public java.util.List<com.vladislav.training.platform.content.domain.Course> findCoursesByStatus(
            ContentStatus status
        ) {
            return courseJpaRepository.findAll().stream()
                .filter(course -> course.getStatus() == status)
                .map(contentMapper::toDomain)
                .toList();
        }
    }
}
