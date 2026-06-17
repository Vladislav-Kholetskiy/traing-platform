package com.vladislav.training.platform.analytics.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.analytics.infrastructure.persistence.AnalyticsQuestionAggregateEntity;
import com.vladislav.training.platform.analytics.infrastructure.persistence.JpaExpertQuestionAnalyticsReadRepositoryAdapter;
import com.vladislav.training.platform.analytics.infrastructure.persistence.SpringDataAnalyticsQuestionAggregateJpaRepository;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.QuestionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataQuestionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = ExpertQuestionAnalyticsReadRepositoryContractTest.RepositoryTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет договорённости вокруг {@code ExpertQuestionAnalyticsReadRepository}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@Testcontainers(disabledWithoutDocker = true)
class ExpertQuestionAnalyticsReadRepositoryContractTest {

    private static final Instant REQUEST_START = Instant.parse("2026-04-10T00:00:00Z");
    private static final Instant REQUEST_END = Instant.parse("2026-04-20T00:00:00Z");
    private static final Instant CALCULATED_AT = Instant.parse("2026-05-01T00:10:00Z");
    private static final Instant REFRESHED_AT = Instant.parse("2026-05-01T00:15:00Z");

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
    private ExpertQuestionAnalyticsReadRepository expertQuestionAnalyticsReadRepository;
    @Autowired
    private SpringDataAnalyticsQuestionAggregateJpaRepository questionAggregateRepository;
    @Autowired
    private SpringDataQuestionJpaRepository questionRepository;
    @Autowired
    private SpringDataTopicJpaRepository topicRepository;
    @Autowired
    private SpringDataCourseJpaRepository courseRepository;

    @AfterEach
    void cleanDatabase() {
        questionAggregateRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
    }

    @Test
    void criteriaRejectsNullMandatoryFieldsAndInvalidPeriod() {
        assertThatThrownBy(() -> new ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria(
            null,
            REQUEST_START,
            REQUEST_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("accessReadScope must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria(
            AccessReadScope.fullAccess(),
            REQUEST_START,
            REQUEST_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be strictly before periodEnd");
    }

    @Test
    void denyScopeReturnsEmptyRows() {
        Long questionId = persistQuestion("DENY").getId();
        questionAggregateRepository.saveAndFlush(questionAggregate(
            questionId,
            REQUEST_START.plusSeconds(3_600),
            REQUEST_END.minusSeconds(3_600),
            "4.2500",
            12,
            7,
            3
        ));

        assertThat(expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(
            new ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria(
                AccessReadScope.denyAll(),
                REQUEST_START,
                REQUEST_END
            )
        )).isEmpty();
    }

    @Test
    void nonFullScopedAccessIsExplicitlyFailClosedAtRepositoryLevel() {
        Long questionId = persistQuestion("SCOPED").getId();
        questionAggregateRepository.saveAndFlush(questionAggregate(
            questionId,
            REQUEST_START.plusSeconds(3_600),
            REQUEST_END.minusSeconds(3_600),
            "3.5000",
            8,
            5,
            2
        ));

        assertThat(expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(
            new ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria(
                AccessReadScope.scoped(Set.of(42L), Set.of("/company/division")),
                REQUEST_START,
                REQUEST_END
            )
        )).isEmpty();
    }

    @Test
    void fullAccessReturnsExpectedQuestionAnalyticsRowsFromAggregateSource() {
        QuestionEntity firstQuestion = persistQuestion("FULL-1");
        QuestionEntity secondQuestion = persistQuestion("FULL-2");
        QuestionEntity outsideQuestion = persistQuestion("FULL-OUT");

        questionAggregateRepository.saveAndFlush(questionAggregate(
            secondQuestion.getId(),
            REQUEST_START.plusSeconds(86_400),
            REQUEST_END.minusSeconds(86_400),
            "4.7500",
            25,
            17,
            4
        ));
        questionAggregateRepository.saveAndFlush(questionAggregate(
            firstQuestion.getId(),
            REQUEST_START.plusSeconds(3_600),
            REQUEST_END.minusSeconds(3_600),
            "3.2500",
            10,
            6,
            1
        ));
        questionAggregateRepository.saveAndFlush(questionAggregate(
            outsideQuestion.getId(),
            REQUEST_END,
            REQUEST_END.plusSeconds(86_400),
            "2.5000",
            3,
            1,
            1
        ));

        assertThat(expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(
            new ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria(
                AccessReadScope.fullAccess(),
                REQUEST_START,
                REQUEST_END
            )
        )).containsExactly(
            new ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadRow(
                firstQuestion.getId(),
                REQUEST_START.plusSeconds(3_600),
                REQUEST_END.minusSeconds(3_600),
                10,
                6,
                1,
                new BigDecimal("3.2500"),
                CALCULATED_AT,
                REFRESHED_AT
            ),
            new ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadRow(
                secondQuestion.getId(),
                REQUEST_START.plusSeconds(86_400),
                REQUEST_END.minusSeconds(86_400),
                25,
                17,
                4,
                new BigDecimal("4.7500"),
                CALCULATED_AT,
                REFRESHED_AT
            )
        );
    }

    @Test
    void periodPredicateUsesActualOverlapSemanticsWithExclusiveBoundaries() {
        QuestionEntity leftOverlap = persistQuestion("LEFT");
        QuestionEntity inside = persistQuestion("INSIDE");
        QuestionEntity rightOverlap = persistQuestion("RIGHT");
        QuestionEntity boundaryAtStart = persistQuestion("BOUNDARY-START");
        QuestionEntity boundaryAtEnd = persistQuestion("BOUNDARY-END");
        QuestionEntity outsideBefore = persistQuestion("OUTSIDE-BEFORE");
        QuestionEntity outsideAfter = persistQuestion("OUTSIDE-AFTER");

        questionAggregateRepository.saveAndFlush(questionAggregate(
            leftOverlap.getId(),
            REQUEST_START.minusSeconds(86_400),
            REQUEST_START.plusSeconds(3_600),
            "3.0000",
            11,
            7,
            2
        ));
        questionAggregateRepository.saveAndFlush(questionAggregate(
            inside.getId(),
            REQUEST_START.plusSeconds(3_600),
            REQUEST_END.minusSeconds(3_600),
            "4.0000",
            12,
            8,
            2
        ));
        questionAggregateRepository.saveAndFlush(questionAggregate(
            rightOverlap.getId(),
            REQUEST_END.minusSeconds(3_600),
            REQUEST_END.plusSeconds(86_400),
            "5.0000",
            13,
            9,
            2
        ));
        questionAggregateRepository.saveAndFlush(questionAggregate(
            boundaryAtStart.getId(),
            REQUEST_START.minusSeconds(86_400),
            REQUEST_START,
            "1.0000",
            1,
            1,
            0
        ));
        questionAggregateRepository.saveAndFlush(questionAggregate(
            boundaryAtEnd.getId(),
            REQUEST_END,
            REQUEST_END.plusSeconds(86_400),
            "1.5000",
            2,
            1,
            1
        ));
        questionAggregateRepository.saveAndFlush(questionAggregate(
            outsideBefore.getId(),
            REQUEST_START.minusSeconds(172_800),
            REQUEST_START.minusSeconds(86_401),
            "2.0000",
            3,
            2,
            1
        ));
        questionAggregateRepository.saveAndFlush(questionAggregate(
            outsideAfter.getId(),
            REQUEST_END.plusSeconds(1),
            REQUEST_END.plusSeconds(172_800),
            "2.5000",
            4,
            2,
            2
        ));

        assertThat(expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(
            new ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria(
                AccessReadScope.fullAccess(),
                REQUEST_START,
                REQUEST_END
            )
        ))
            .extracting(ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadRow::questionId)
            .containsExactly(leftOverlap.getId(), inside.getId(), rightOverlap.getId());
    }

    @Test
    void adapterStaysBoundToQuestionAggregateReadPathAndAvoidsManagerialAuthoringAndRebuildDrift() throws IOException {
        assertThat(ExpertQuestionAnalyticsReadRepository.class.isAssignableFrom(
            JpaExpertQuestionAnalyticsReadRepositoryAdapter.class
        )).isTrue();
        assertThat(fieldTypes(JpaExpertQuestionAnalyticsReadRepositoryAdapter.class))
            .containsExactly(EntityManager.class);
        assertThat(componentNames(ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadRow.class))
            .containsExactly(
                "questionId",
                "periodStart",
                "periodEnd",
                "attemptCount",
                "correctCount",
                "incorrectCount",
                "averageEarnedScore",
                "calculatedAt",
                "refreshedAt"
            );

        String source = read(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                + "JpaExpertQuestionAnalyticsReadRepositoryAdapter.java"
        );
        assertThat(source)
            .contains("AnalyticsQuestionAggregateEntity")
            .contains("EntityManager")
            .contains("periodOverlapPredicate")
            .doesNotContain("AnalyticsUserTopicAggregateEntity")
            .doesNotContain("AnalyticsDepartmentTopicAggregateEntity")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsReadRepository")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("ContentLifecycleController")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("TestLifecycleService")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("publish")
            .doesNotContain("archive")
            .doesNotContain("assignActiveFinal")
            .doesNotContain("refresh(")
            .doesNotContain("rebuild")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult");
    }

    private QuestionEntity persistQuestion(String suffix) {
        CourseEntity course = courseRepository.saveAndFlush(courseEntity(suffix));
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId(), suffix));
        return questionRepository.saveAndFlush(questionEntity(topic.getId(), suffix));
    }

    private AnalyticsQuestionAggregateEntity questionAggregate(
        Long questionId,
        Instant periodStart,
        Instant periodEnd,
        String averageEarnedScore,
        Integer attemptCount,
        Integer correctCount,
        Integer incorrectCount
    ) {
        AnalyticsQuestionAggregateEntity entity = instantiate(AnalyticsQuestionAggregateEntity.class);
        entity.setQuestionId(questionId);
        entity.setPeriodStart(periodStart);
        entity.setPeriodEnd(periodEnd);
        entity.setAttemptCount(attemptCount);
        entity.setCorrectCount(correctCount);
        entity.setIncorrectCount(incorrectCount);
        entity.setAverageEarnedScore(new BigDecimal(averageEarnedScore));
        entity.setCalculatedAt(CALCULATED_AT);
        entity.setRefreshedAt(REFRESHED_AT);
        entity.setReconciledAt(null);
        return entity;
    }

    private CourseEntity courseEntity(String suffix) {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Course " + suffix);
        entity.setDescription("Course");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(CALCULATED_AT);
        entity.setUpdatedAt(CALCULATED_AT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId, String suffix) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName("Topic " + suffix);
        entity.setDescription("Topic");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(CALCULATED_AT);
        entity.setUpdatedAt(CALCULATED_AT);
        return entity;
    }

    private QuestionEntity questionEntity(Long topicId, String suffix) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody("Question " + suffix);
        entity.setQuestionType(QuestionType.SINGLE_CHOICE);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(CALCULATED_AT);
        entity.setUpdatedAt(CALCULATED_AT);
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

    private List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AnalyticsQuestionAggregateEntity.class,
        CourseEntity.class,
        TopicEntity.class,
        QuestionEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAnalyticsQuestionAggregateJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataQuestionJpaRepository.class
    })
    @ComponentScan(
        basePackageClasses = JpaExpertQuestionAnalyticsReadRepositoryAdapter.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JpaExpertQuestionAnalyticsReadRepositoryAdapter.class
        )
    )
    static class RepositoryTestApplication {
    }
}
