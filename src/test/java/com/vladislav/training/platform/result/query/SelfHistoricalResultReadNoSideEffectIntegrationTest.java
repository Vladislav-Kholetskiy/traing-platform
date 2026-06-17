package com.vladislav.training.platform.result.query;

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
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.SystemUtcClock;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(
    classes = SelfHistoricalResultReadNoSideEffectIntegrationTest.SelfHistoricalResultReadNoSideEffectTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code SelfHistoricalResultReadNoSideEffect} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
class SelfHistoricalResultReadNoSideEffectIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T09:00:00Z");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:self-history-read;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private SelfHistoricalResultController selfHistoricalResultController;
    @Autowired
    private UtcClock utcClock;
    @Autowired
    private SpringDataTestJpaRepository testJpaRepository;
    @Autowired
    private SpringDataTestAttemptJpaRepository testAttemptJpaRepository;
    @Autowired
    private SpringDataResultJpaRepository resultJpaRepository;
    @Autowired
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Autowired
    private AccessPolicyQueryContextResolver accessPolicyQueryContextResolver;
    @Autowired
    private InteractiveActorResolver interactiveActorResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(selfHistoricalResultController)
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @AfterEach
    void tearDown() {
        reset(accessSpecificationPolicy, accessPolicyQueryContextResolver, interactiveActorResolver);
        resultJpaRepository.deleteAllInBatch();
        testAttemptJpaRepository.deleteAllInBatch();
        testJpaRepository.deleteAllInBatch();
    }

    @Test
    void selfHistoryReadReturnsOnlyActorOwnedResultRowsAndDoesNotMutateResultOrAttemptState() throws Exception {
        Long firstTestId = testJpaRepository.saveAndFlush(testEntity("Assigned Test", TestType.CONTROL)).getId();
        Long secondTestId = testJpaRepository.saveAndFlush(testEntity("Self Test", TestType.TRAINING)).getId();
        Long foreignTestId = testJpaRepository.saveAndFlush(testEntity("Foreign Test", TestType.TRAINING)).getId();

        TestAttemptEntity assignedAttempt = testAttemptJpaRepository.saveAndFlush(
            testAttemptEntity(101L, firstTestId, 8001L, AttemptMode.ASSIGNED, FIXED_INSTANT)
        );
        TestAttemptEntity selfAttempt = testAttemptJpaRepository.saveAndFlush(
            testAttemptEntity(101L, secondTestId, null, AttemptMode.SELF, FIXED_INSTANT.minusSeconds(60))
        );
        TestAttemptEntity foreignAttempt = testAttemptJpaRepository.saveAndFlush(
            testAttemptEntity(202L, foreignTestId, null, AttemptMode.SELF, FIXED_INSTANT.plusSeconds(60))
        );

        ResultEntity assignedResult = resultJpaRepository.saveAndFlush(
            resultEntity(
                101L,
                assignedAttempt.getId(),
                AttemptMode.ASSIGNED,
                3001L,
                8001L,
                firstTestId,
                "Assigned Test",
                new BigDecimal("80.0000"),
                new BigDecimal("8.0000"),
                true,
                FIXED_INSTANT
            )
        );
        ResultEntity selfResult = resultJpaRepository.saveAndFlush(
            resultEntity(
                101L,
                selfAttempt.getId(),
                AttemptMode.SELF,
                null,
                null,
                secondTestId,
                "Self Test",
                new BigDecimal("40.0000"),
                new BigDecimal("4.0000"),
                false,
                FIXED_INSTANT.minusSeconds(60)
            )
        );
        resultJpaRepository.saveAndFlush(
            resultEntity(
                202L,
                foreignAttempt.getId(),
                AttemptMode.SELF,
                null,
                null,
                foreignTestId,
                "Foreign Test",
                new BigDecimal("90.0000"),
                new BigDecimal("9.0000"),
                true,
                FIXED_INSTANT.plusSeconds(60)
            )
        );

        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(accessPolicyQueryContextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        )).thenReturn(selfHistoryContext(101L));
        when(accessSpecificationPolicy.canRead(any())).thenReturn(true);

        assertReadLeavesStateUntouched(get("/api/v1/self/results/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].resultId").value(assignedResult.getId()))
            .andExpect(jsonPath("$[0].recordedAt").value(FIXED_INSTANT.toString()))
            .andExpect(jsonPath("$[0].testAttemptId").value(assignedAttempt.getId()))
            .andExpect(jsonPath("$[0].testId").value(firstTestId))
            .andExpect(jsonPath("$[0].testName").value("Assigned Test"))
            .andExpect(jsonPath("$[0].scorePercent").value(80.0000))
            .andExpect(jsonPath("$[0].score").value(8.0000))
            .andExpect(jsonPath("$[0].passed").value(true))
            .andExpect(jsonPath("$[0].attemptMode").value("ASSIGNED"))
            .andExpect(jsonPath("$[0].assignmentId").value(3001))
            .andExpect(jsonPath("$[1].resultId").value(selfResult.getId()))
            .andExpect(jsonPath("$[1].testAttemptId").value(selfAttempt.getId()))
            .andExpect(jsonPath("$[1].testId").value(secondTestId))
            .andExpect(jsonPath("$[1].testName").value("Self Test"))
            .andExpect(jsonPath("$[1].scorePercent").value(40.0000))
            .andExpect(jsonPath("$[1].score").value(4.0000))
            .andExpect(jsonPath("$[1].passed").value(false))
            .andExpect(jsonPath("$[1].attemptMode").value("SELF"))
            .andExpect(jsonPath("$[1].assignmentId").isEmpty())
            .andExpect(jsonPath("$[0].scoringPolicyCode").doesNotExist())
            .andExpect(jsonPath("$[0].correctAnswers").doesNotExist())
            .andExpect(jsonPath("$[0].isCorrect").doesNotExist());
    }

    @Test
    void unauthenticatedSelfHistoryReadFailsPredictablyWithoutMutatingResultOrAttemptState() throws Exception {
        Long testId = testJpaRepository.saveAndFlush(testEntity("Self Test", TestType.TRAINING)).getId();
        TestAttemptEntity attempt = testAttemptJpaRepository.saveAndFlush(
            testAttemptEntity(101L, testId, null, AttemptMode.SELF, FIXED_INSTANT)
        );
        resultJpaRepository.saveAndFlush(
            resultEntity(
                101L,
                attempt.getId(),
                AttemptMode.SELF,
                null,
                null,
                testId,
                "Self Test",
                new BigDecimal("70.0000"),
                new BigDecimal("7.0000"),
                true,
                FIXED_INSTANT
            )
        );

        when(interactiveActorResolver.resolveActorUserId())
            .thenThrow(new PolicyViolationException("ACTOR_UNAUTHENTICATED", "Authenticated principal is required"));

        assertReadLeavesStateUntouched(get("/api/v1/self/results/history"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Authenticated principal is required"));
    }

    private org.springframework.test.web.servlet.ResultActions assertReadLeavesStateUntouched(
        MockHttpServletRequestBuilder requestBuilder
    ) throws Exception {
        Map<String, Object> before = captureState();
        org.springframework.test.web.servlet.ResultActions result = mockMvc.perform(requestBuilder);
        org.assertj.core.api.Assertions.assertThat(captureState())
            
            .isEqualTo(before);
        return result;
    }

    private Map<String, Object> captureState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("results", resultJpaRepository.findAll().stream()
            .map(entity -> Arrays.asList(
                entity.getId(),
                entity.getUserIdSnapshot(),
                entity.getTestAttemptId(),
                entity.getAttemptMode(),
                entity.getAssignmentId(),
                entity.getScorePercent(),
                entity.getEarnedScore(),
                entity.isPassed(),
                entity.getCompletedAt()
            ))
            .toList());
        state.put("attempts", testAttemptJpaRepository.findAll().stream()
            .map(entity -> Arrays.asList(
                entity.getId(),
                entity.getUserId(),
                entity.getTestId(),
                entity.getAssignmentTestId(),
                entity.getAttemptMode(),
                entity.getStatus(),
                entity.getCompletedAt(),
                entity.getUpdatedAt()
            ))
            .toList());
        return state;
    }

    private AccessPolicyQueryContext selfHistoryContext(Long actorUserId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "self_result_history",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private TestEntity testEntity(String name, TestType type) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(501L);
        entity.setName(name);
        entity.setDescription("History summary test");
        entity.setTestType(type);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setScoringPolicyCode("STANDARD");
        entity.setActiveFinalForTopic(false);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(300));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(300));
        return entity;
    }

    private TestAttemptEntity testAttemptEntity(
        Long userId,
        Long testId,
        Long assignmentTestId,
        AttemptMode attemptMode,
        Instant completedAt
    ) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(attemptMode);
        entity.setStatus(TestAttemptStatus.COMPLETED);
        entity.setStartedAt(completedAt.minusSeconds(120));
        entity.setCompletedAt(completedAt);
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(completedAt.minusSeconds(10));
        entity.setCreatedAt(completedAt.minusSeconds(120));
        entity.setUpdatedAt(completedAt);
        return entity;
    }

    private ResultEntity resultEntity(
        Long userIdSnapshot,
        Long testAttemptId,
        AttemptMode attemptMode,
        Long assignmentId,
        Long assignmentTestId,
        Long testIdSnapshot,
        String testNameSnapshot,
        BigDecimal scorePercent,
        BigDecimal score,
        boolean passed,
        Instant completedAt
    ) {
        ResultEntity entity = instantiate(ResultEntity.class);
        entity.setTestAttemptId(testAttemptId);
        entity.setUserIdSnapshot(userIdSnapshot);
        entity.setAttemptMode(attemptMode);
        entity.setAssignmentId(assignmentId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setTestIdSnapshot(testIdSnapshot);
        entity.setTestNameSnapshot(testNameSnapshot);
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setEarnedScore(score);
        entity.setMaxScore(new BigDecimal("10.0000"));
        entity.setScorePercent(scorePercent);
        entity.setPassed(passed);
        entity.setWithinDeadline(attemptMode == AttemptMode.ASSIGNED ? Boolean.TRUE : null);
        entity.setCountedInAssignment(attemptMode == AttemptMode.ASSIGNED ? Boolean.TRUE : null);
        entity.setScoringPolicyCode("STANDARD");
        entity.setScoringPolicySnapshot("{\"policy\":\"history-summary\"}");
        entity.setCompletedAt(completedAt);
        entity.setOrganizationalUnitIdSnapshot(700L);
        entity.setOrganizationalPathSnapshot("/company/history");
        entity.setSnapshotFinalTopicControlFlag(false);
        entity.setCreatedAt(completedAt.plusSeconds(5));
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

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        TestEntity.class,
        TestAttemptEntity.class,
        ResultEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataTestJpaRepository.class,
        SpringDataTestAttemptJpaRepository.class,
        SpringDataResultJpaRepository.class
    })
    @Import({
        SelfHistoricalResultQueryServiceImpl.class,
        SelfHistoricalResultController.class,
        SystemUtcClock.class
    })
    static class SelfHistoricalResultReadNoSideEffectTestApplication {

        @Bean
        com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader SelfHistoricalResultReader(
            SpringDataResultJpaRepository repository
        ) {
            try {
                Class<?> type = Class.forName(
                    "com.vladislav.training.platform.result.infrastructure.persistence.JpaSelfHistoricalResultReader"
                );
                var constructor = type.getDeclaredConstructor(SpringDataResultJpaRepository.class);
                constructor.setAccessible(true);
                return (com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader)
                    constructor.newInstance(repository);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to instantiate JpaSelfHistoricalResultReader", exception);
            }
        }

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
    }
}

