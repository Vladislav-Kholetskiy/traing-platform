package com.vladislav.training.platform.analytics.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.analytics.infrastructure.persistence.AnalyticsDepartmentTopicAggregateEntity;
import com.vladislav.training.platform.analytics.infrastructure.persistence.AnalyticsUserTopicAggregateEntity;
import com.vladislav.training.platform.analytics.infrastructure.persistence.JpaManagerialHistoricalAnalyticsReadRepositoryAdapter;
import com.vladislav.training.platform.analytics.infrastructure.persistence.SpringDataAnalyticsDepartmentTopicAggregateJpaRepository;
import com.vladislav.training.platform.analytics.infrastructure.persistence.SpringDataAnalyticsUserTopicAggregateJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitTypeEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataOrganizationalUnitJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataOrganizationalUnitTypeJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
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
    classes = ManagerialHistoricalAnalyticsReadRepositoryContractTest.RepositoryTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет договорённости вокруг {@code ManagerialHistoricalAnalyticsReadRepository}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@Testcontainers(disabledWithoutDocker = true)
class ManagerialHistoricalAnalyticsReadRepositoryContractTest {

    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-25T10:00:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");
    private static final Instant OUTSIDE_PERIOD_START = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant OUTSIDE_PERIOD_END = Instant.parse("2026-05-31T23:59:59Z");
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
    private ManagerialHistoricalAnalyticsReadRepository managerialHistoricalAnalyticsReadRepository;
    @Autowired
    private SpringDataAnalyticsUserTopicAggregateJpaRepository userTopicAggregateRepository;
    @Autowired
    private SpringDataAnalyticsDepartmentTopicAggregateJpaRepository departmentTopicAggregateRepository;
    @Autowired
    private SpringDataAppUserJpaRepository appUserRepository;
    @Autowired
    private SpringDataCourseJpaRepository courseRepository;
    @Autowired
    private SpringDataTopicJpaRepository topicRepository;
    @Autowired
    private SpringDataResultJpaRepository resultRepository;
    @Autowired
    private SpringDataTestAttemptJpaRepository testAttemptRepository;
    @Autowired
    private SpringDataTestJpaRepository testRepository;
    @Autowired
    private SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Autowired
    private SpringDataOrganizationalUnitTypeJpaRepository organizationalUnitTypeRepository;

    @AfterEach
    void cleanDatabase() {
        departmentTopicAggregateRepository.deleteAllInBatch();
        userTopicAggregateRepository.deleteAllInBatch();
        resultRepository.deleteAllInBatch();
        testAttemptRepository.deleteAllInBatch();
        testRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        organizationalUnitRepository.deleteAllInBatch();
        organizationalUnitTypeRepository.deleteAllInBatch();
    }

    @Test
    void criteriaRejectsNullMandatoryFieldsAndInvalidPeriod() {
        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
            null,
            PERIOD_START,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("managerialReadScope must not be null");

        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
            denyAllScope(),
            PERIOD_START,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be before periodEnd");
    }

    @Test
    void userTopicDenyScopeReturnsEmptyRows() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("UT-DENY"));
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("UT-DENY")).getId(),
            "UT-DENY"
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            user.getId(),
            topic.getId(),
            PERIOD_START,
            PERIOD_END,
            "84.2500",
            "91.5000",
            12,
            3
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findUserTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                denyAllScope(),
                PERIOD_START,
                PERIOD_END
            )
        )).isEmpty();
    }

    @Test
    void userTopicFullAccessReturnsOnlyRowsInsideRequestedPeriod() {
        AppUserEntity firstUser = appUserRepository.saveAndFlush(userEntity("UT-FULL-1"));
        AppUserEntity secondUser = appUserRepository.saveAndFlush(userEntity("UT-FULL-2"));
        AppUserEntity thirdUser = appUserRepository.saveAndFlush(userEntity("UT-FULL-3"));
        AppUserEntity fourthUser = appUserRepository.saveAndFlush(userEntity("UT-FULL-4"));
        CourseEntity firstCourse = courseRepository.saveAndFlush(courseEntity("UT-FULL-1"));
        CourseEntity secondCourse = courseRepository.saveAndFlush(courseEntity("UT-FULL-2"));
        CourseEntity thirdCourse = courseRepository.saveAndFlush(courseEntity("UT-FULL-3"));
        CourseEntity fourthCourse = courseRepository.saveAndFlush(courseEntity("UT-FULL-4"));
        TopicEntity firstTopic = topicRepository.saveAndFlush(topicEntity(firstCourse.getId(), "UT-FULL-1"));
        TopicEntity secondTopic = topicRepository.saveAndFlush(topicEntity(secondCourse.getId(), "UT-FULL-2"));
        TopicEntity thirdTopic = topicRepository.saveAndFlush(topicEntity(thirdCourse.getId(), "UT-FULL-3"));
        TopicEntity fourthTopic = topicRepository.saveAndFlush(topicEntity(fourthCourse.getId(), "UT-FULL-4"));

        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            firstUser.getId(),
            firstTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "84.2500",
            "91.5000",
            12,
            3
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            secondUser.getId(),
            secondTopic.getId(),
            PERIOD_START.plusSeconds(86_400),
            PERIOD_END.minusSeconds(86_400),
            "77.2500",
            "61.5000",
            25,
            4
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            thirdUser.getId(),
            thirdTopic.getId(),
            OUTSIDE_PERIOD_START,
            PERIOD_END,
            "66.0000",
            "55.0000",
            7,
            2
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            fourthUser.getId(),
            fourthTopic.getId(),
            PERIOD_START,
            OUTSIDE_PERIOD_END,
            "88.0000",
            "79.0000",
            9,
            1
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findUserTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                fullAccessScope(),
                PERIOD_START,
                PERIOD_END
            )
        )).containsExactly(
            userTopicReadRow(firstUser, firstTopic, PERIOD_START, PERIOD_END, "84.2500", "91.5000", 12, 3),
            userTopicReadRow(
                secondUser,
                secondTopic,
                PERIOD_START.plusSeconds(86_400),
                PERIOD_END.minusSeconds(86_400),
                "77.2500",
                "61.5000",
                25,
                4
            )
        );
    }

    @Test
    void userTopicUnitScopeReturnsOnlyAllowedOrganizationalUnits() {
        AppUserEntity allowedUser = appUserRepository.saveAndFlush(userEntity("UT-SCOPED-ALLOWED"));
        AppUserEntity blockedUser = appUserRepository.saveAndFlush(userEntity("UT-SCOPED-BLOCKED"));
        TopicEntity allowedTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("UT-SCOPED-ALLOWED")).getId(),
            "UT-SCOPED-ALLOWED"
        ));
        TopicEntity blockedTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("UT-SCOPED-BLOCKED")).getId(),
            "UT-SCOPED-BLOCKED"
        ));
        TestEntity allowedTest = testRepository.saveAndFlush(testEntity(allowedTopic.getId(), "UT-SCOPED-ALLOWED"));
        TestEntity blockedTest = testRepository.saveAndFlush(testEntity(blockedTopic.getId(), "UT-SCOPED-BLOCKED"));
        TestAttemptEntity allowedAttempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            allowedUser.getId(),
            allowedTest.getId()
        ));
        TestAttemptEntity blockedAttempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            blockedUser.getId(),
            blockedTest.getId()
        ));
        ResultEntity allowedResult = resultRepository.saveAndFlush(resultEntity(
            allowedAttempt.getId(),
            allowedUser.getId(),
            42L,
            "/company/division/department"
        ));
        ResultEntity blockedResult = resultRepository.saveAndFlush(resultEntity(
            blockedAttempt.getId(),
            blockedUser.getId(),
            43L,
            "/company/division/other"
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            allowedUser.getId(),
            allowedTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "84.2500",
            "91.5000",
            12,
            3,
            allowedResult
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            blockedUser.getId(),
            blockedTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "66.2500",
            "51.5000",
            9,
            2,
            blockedResult
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findUserTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                scopedUnitScope(Set.of(42L)),
                PERIOD_START,
                PERIOD_END
            )
        )).containsExactly(
            userTopicReadRow(allowedUser, allowedTopic, PERIOD_START, PERIOD_END, "84.2500", "91.5000", 12, 3)
        );
    }

    @Test
    void userTopicSubtreeScopeReturnsOnlyAllowedPathSubtreeRows() {
        AppUserEntity rootUser = appUserRepository.saveAndFlush(userEntity("UT-SUBTREE-ROOT"));
        AppUserEntity childUser = appUserRepository.saveAndFlush(userEntity("UT-SUBTREE-CHILD"));
        AppUserEntity blockedUser = appUserRepository.saveAndFlush(userEntity("UT-SUBTREE-BLOCKED"));
        TopicEntity rootTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("UT-SUBTREE-ROOT")).getId(),
            "UT-SUBTREE-ROOT"
        ));
        TopicEntity childTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("UT-SUBTREE-CHILD")).getId(),
            "UT-SUBTREE-CHILD"
        ));
        TopicEntity blockedTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("UT-SUBTREE-BLOCKED")).getId(),
            "UT-SUBTREE-BLOCKED"
        ));
        TestEntity rootTest = testRepository.saveAndFlush(testEntity(rootTopic.getId(), "UT-SUBTREE-ROOT"));
        TestEntity childTest = testRepository.saveAndFlush(testEntity(childTopic.getId(), "UT-SUBTREE-CHILD"));
        TestEntity blockedTest = testRepository.saveAndFlush(testEntity(blockedTopic.getId(), "UT-SUBTREE-BLOCKED"));
        TestAttemptEntity rootAttempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            rootUser.getId(),
            rootTest.getId()
        ));
        TestAttemptEntity childAttempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            childUser.getId(),
            childTest.getId()
        ));
        TestAttemptEntity blockedAttempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            blockedUser.getId(),
            blockedTest.getId()
        ));
        ResultEntity rootResult = resultRepository.saveAndFlush(resultEntity(
            rootAttempt.getId(),
            rootUser.getId(),
            42L,
            "/company/division/department"
        ));
        ResultEntity childResult = resultRepository.saveAndFlush(resultEntity(
            childAttempt.getId(),
            childUser.getId(),
            43L,
            "/company/division/department/team-a"
        ));
        ResultEntity blockedResult = resultRepository.saveAndFlush(resultEntity(
            blockedAttempt.getId(),
            blockedUser.getId(),
            44L,
            "/company/division/other"
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            rootUser.getId(),
            rootTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "84.2500",
            "91.5000",
            12,
            3,
            rootResult
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            childUser.getId(),
            childTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "77.2500",
            "61.5000",
            25,
            4,
            childResult
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            blockedUser.getId(),
            blockedTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "66.2500",
            "51.5000",
            9,
            2,
            blockedResult
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findUserTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                subtreeScope(Set.of("/company/division/department/")),
                PERIOD_START,
                PERIOD_END
            )
        )).containsExactly(
            userTopicReadRow(rootUser, rootTopic, PERIOD_START, PERIOD_END, "84.2500", "91.5000", 12, 3),
            userTopicReadRow(childUser, childTopic, PERIOD_START, PERIOD_END, "77.2500", "61.5000", 25, 4)
        );
    }

    @Test
    void userTopicScopedReadWithoutAnyAllowedScopeReturnsEmpty() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("UT-EMPTY-SCOPE"));
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("UT-EMPTY-SCOPE")).getId(),
            "UT-EMPTY-SCOPE"
        ));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId(), "UT-EMPTY-SCOPE"));
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(testAttemptEntity(
            user.getId(),
            test.getId()
        ));
        ResultEntity result = resultRepository.saveAndFlush(resultEntity(
            attempt.getId(),
            user.getId(),
            42L,
            "/company/division/department"
        ));
        userTopicAggregateRepository.saveAndFlush(userTopicEntity(
            user.getId(),
            topic.getId(),
            PERIOD_START,
            PERIOD_END,
            "84.2500",
            "91.5000",
            12,
            3,
            result
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findUserTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                scopedUnitScope(Set.of()),
                PERIOD_START,
                PERIOD_END
            )
        )).isEmpty();
    }

    @Test
    void departmentTopicDenyScopeReturnsEmptyRows() {
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-DENY")).getId(),
            "DT-DENY"
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            42L,
            "/company/division/department",
            topic.getId(),
            PERIOD_START,
            PERIOD_END,
            "77.2500",
            "61.5000",
            25,
            4
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findDepartmentTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                denyAllScope(),
                PERIOD_START,
                PERIOD_END
            )
        )).isEmpty();
    }

    @Test
    void departmentTopicFullAccessReturnsExpectedRowsInsideRequestedPeriod() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("DT-FULL"));
        OrganizationalUnitEntity firstUnit = organizationalUnitRepository.saveAndFlush(
            organizationalUnitEntity(unitType.getId(), "Unit 42", "/company/division/department")
        );
        OrganizationalUnitEntity secondUnit = organizationalUnitRepository.saveAndFlush(
            organizationalUnitEntity(unitType.getId(), "Unit 43", "/company/division/other")
        );
        OrganizationalUnitEntity thirdUnit = organizationalUnitRepository.saveAndFlush(
            organizationalUnitEntity(unitType.getId(), "Unit 44", "/company/division/outside")
        );
        TopicEntity firstTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-FULL-1")).getId(),
            "DT-FULL-1"
        ));
        TopicEntity secondTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-FULL-2")).getId(),
            "DT-FULL-2"
        ));
        TopicEntity thirdTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-FULL-3")).getId(),
            "DT-FULL-3"
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            firstUnit.getId(),
            "/company/division/department",
            firstTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "77.2500",
            "61.5000",
            25,
            4
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            secondUnit.getId(),
            "/company/division/other",
            secondTopic.getId(),
            PERIOD_START.plusSeconds(86_400),
            PERIOD_END.minusSeconds(86_400),
            "88.5000",
            "93.0000",
            11,
            1
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            thirdUnit.getId(),
            "/company/division/outside",
            thirdTopic.getId(),
            OUTSIDE_PERIOD_START,
            PERIOD_END,
            "55.0000",
            "45.0000",
            8,
            2
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findDepartmentTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                fullAccessScope(),
                PERIOD_START,
                PERIOD_END
            )
        )).containsExactly(
            departmentTopicReadRow(
                firstUnit.getId(),
                firstUnit.getName(),
                "/company/division/department",
                firstTopic,
                PERIOD_START,
                PERIOD_END,
                "77.2500",
                "61.5000",
                25,
                4
            ),
            departmentTopicReadRow(
                secondUnit.getId(),
                secondUnit.getName(),
                "/company/division/other",
                secondTopic,
                PERIOD_START.plusSeconds(86_400),
                PERIOD_END.minusSeconds(86_400),
                "88.5000",
                "93.0000",
                11,
                1
            )
        );
    }

    @Test
    void departmentTopicUnitScopeReturnsOnlyAllowedOrganizationalUnits() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("DT-UNIT"));
        OrganizationalUnitEntity firstUnit = organizationalUnitRepository.saveAndFlush(
            organizationalUnitEntity(unitType.getId(), "Unit 42", "/company/division/department")
        );
        OrganizationalUnitEntity secondUnit = organizationalUnitRepository.saveAndFlush(
            organizationalUnitEntity(unitType.getId(), "Unit 43", "/company/division/other")
        );
        TopicEntity firstTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-UNIT-1")).getId(),
            "DT-UNIT-1"
        ));
        TopicEntity secondTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-UNIT-2")).getId(),
            "DT-UNIT-2"
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            firstUnit.getId(),
            "/company/division/department",
            firstTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "77.2500",
            "61.5000",
            25,
            4
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            secondUnit.getId(),
            "/company/division/other",
            secondTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "88.5000",
            "93.0000",
            11,
            1
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findDepartmentTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                scopedUnitScope(Set.of(secondUnit.getId())),
                PERIOD_START,
                PERIOD_END
            )
        )).containsExactly(
            departmentTopicReadRow(
                secondUnit.getId(),
                secondUnit.getName(),
                "/company/division/other",
                secondTopic,
                PERIOD_START,
                PERIOD_END,
                "88.5000",
                "93.0000",
                11,
                1
            )
        );
    }

    @Test
    void departmentTopicSubtreeScopeReturnsOnlyAllowedPathSubtreeRows() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("DT-SUBTREE"));
        OrganizationalUnitEntity firstUnit = organizationalUnitRepository.saveAndFlush(
            organizationalUnitEntity(unitType.getId(), "Unit 42", "/company/division/department")
        );
        OrganizationalUnitEntity secondUnit = organizationalUnitRepository.saveAndFlush(
            organizationalUnitEntity(unitType.getId(), "Unit 43", "/company/division/department/team-a")
        );
        OrganizationalUnitEntity thirdUnit = organizationalUnitRepository.saveAndFlush(
            organizationalUnitEntity(unitType.getId(), "Unit 44", "/company/division/other")
        );
        TopicEntity firstTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-SUBTREE-1")).getId(),
            "DT-SUBTREE-1"
        ));
        TopicEntity secondTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-SUBTREE-2")).getId(),
            "DT-SUBTREE-2"
        ));
        TopicEntity thirdTopic = topicRepository.saveAndFlush(topicEntity(
            courseRepository.saveAndFlush(courseEntity("DT-SUBTREE-3")).getId(),
            "DT-SUBTREE-3"
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            firstUnit.getId(),
            "/company/division/department",
            firstTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "77.2500",
            "61.5000",
            25,
            4
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            secondUnit.getId(),
            "/company/division/department/team-a",
            secondTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "88.5000",
            "93.0000",
            11,
            1
        ));
        departmentTopicAggregateRepository.saveAndFlush(departmentTopicEntity(
            thirdUnit.getId(),
            "/company/division/other",
            thirdTopic.getId(),
            PERIOD_START,
            PERIOD_END,
            "55.0000",
            "45.0000",
            8,
            2
        ));

        assertThat(managerialHistoricalAnalyticsReadRepository.findDepartmentTopicRows(
            new ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria(
                subtreeScope(Set.of("/company/division/department/")),
                PERIOD_START,
                PERIOD_END
            )
        )).containsExactly(
            departmentTopicReadRow(
                firstUnit.getId(),
                firstUnit.getName(),
                "/company/division/department",
                firstTopic,
                PERIOD_START,
                PERIOD_END,
                "77.2500",
                "61.5000",
                25,
                4
            ),
            departmentTopicReadRow(
                secondUnit.getId(),
                secondUnit.getName(),
                "/company/division/department/team-a",
                secondTopic,
                PERIOD_START,
                PERIOD_END,
                "88.5000",
                "93.0000",
                11,
                1
            )
        );
    }

    @Test
    void adapterSourceStaysHistoricalDerivedReadOnlyAndAvoidsForbiddenDependencies() throws IOException {
        assertThat(componentNames(ManagerialHistoricalAnalyticsReadRepository.ManagerialUserTopicAnalyticsReadRow.class))
            .containsExactly(
                "userId",
                "employeeNumber",
                "lastName",
                "firstName",
                "middleName",
                "topicId",
                "topicName",
                "periodStart",
                "periodEnd",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "calculatedAt",
                "refreshedAt"
            );
        assertThat(componentNames(ManagerialHistoricalAnalyticsReadRepository.ManagerialDepartmentTopicAnalyticsReadRow.class))
            .containsExactly(
                "organizationalUnitIdSnapshot",
                "organizationalUnitName",
                "organizationalPathSnapshot",
                "topicId",
                "topicName",
                "periodStart",
                "periodEnd",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "calculatedAt",
                "refreshedAt"
            );

        String source = read(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                + "JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.java"
        );
        assertThat(fieldTypes(JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.class))
            .containsExactly(EntityManager.class);
        assertThat(source)
            .contains("AnalyticsUserTopicAggregateEntity")
            .contains("AnalyticsDepartmentTopicAggregateEntity")
            .contains("periodRangePredicate")
            .doesNotContain("assignment.status")
            .doesNotContain("AssignmentEntity")
            .doesNotContain("AssignmentTestEntity")
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("AnalyticsCampaignAggregateRepository")
            .doesNotContain("AnalyticsCampaignAggregate")
            .doesNotContain("analytics_campaign_aggregate")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("recalculate")
            .doesNotContain("refresh(")
            .doesNotContain("rebuild")
            .doesNotContain("recordResult")
            .doesNotContain("save(")
            .doesNotContain("delete(");
    }

    private ManagerialReadScope denyAllScope() {
        return new ManagerialReadScope(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.denyAll()
        );
    }

    private ManagerialReadScope fullAccessScope() {
        return new ManagerialReadScope(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.fullAccess()
        );
    }

    private ManagerialReadScope scopedUnitScope(Set<Long> unitIds) {
        return new ManagerialReadScope(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.scoped(unitIds, Set.of())
        );
    }

    private ManagerialReadScope subtreeScope(Set<String> subtreePaths) {
        return new ManagerialReadScope(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.scoped(Set.of(), subtreePaths)
        );
    }

    private AnalyticsUserTopicAggregateEntity userTopicEntity(
        Long userId,
        Long topicId,
        Instant periodStart,
        Instant periodEnd,
        String averageScorePercent,
        String passRatePercent,
        Integer attemptCount,
        Integer errorCount
    ) {
        return userTopicEntity(
            userId,
            topicId,
            periodStart,
            periodEnd,
            averageScorePercent,
            passRatePercent,
            attemptCount,
            errorCount,
            null
        );
    }

    private AnalyticsUserTopicAggregateEntity userTopicEntity(
        Long userId,
        Long topicId,
        Instant periodStart,
        Instant periodEnd,
        String averageScorePercent,
        String passRatePercent,
        Integer attemptCount,
        Integer errorCount,
        ResultEntity lastAssignedFinalResult
    ) {
        AnalyticsUserTopicAggregateEntity entity = instantiate(AnalyticsUserTopicAggregateEntity.class);
        entity.setUserId(userId);
        entity.setTopicId(topicId);
        entity.setPeriodStart(periodStart);
        entity.setPeriodEnd(periodEnd);
        entity.setLastAssignedFinalResultId(lastAssignedFinalResult == null ? null : lastAssignedFinalResult.getId());
        entity.setLastAssignedFinalCompletedAt(lastAssignedFinalResult == null ? null : lastAssignedFinalResult.getCompletedAt());
        entity.setLastAssignedFinalScorePercent(lastAssignedFinalResult == null ? null : lastAssignedFinalResult.getScorePercent());
        entity.setLastAssignedFinalPassed(lastAssignedFinalResult == null ? null : lastAssignedFinalResult.isPassed());
        entity.setAverageScorePercent(new BigDecimal(averageScorePercent));
        entity.setPassRatePercent(new BigDecimal(passRatePercent));
        entity.setAttemptCount(attemptCount);
        entity.setErrorCount(errorCount);
        entity.setCalculatedAt(CALCULATED_AT);
        entity.setRefreshedAt(REFRESHED_AT);
        entity.setReconciledAt(null);
        return entity;
    }

    private AnalyticsDepartmentTopicAggregateEntity departmentTopicEntity(
        Long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot,
        Long topicId,
        Instant periodStart,
        Instant periodEnd,
        String averageScorePercent,
        String passRatePercent,
        Integer attemptCount,
        Integer errorCount
    ) {
        AnalyticsDepartmentTopicAggregateEntity entity = instantiate(AnalyticsDepartmentTopicAggregateEntity.class);
        entity.setOrganizationalUnitIdSnapshot(organizationalUnitIdSnapshot);
        entity.setOrganizationalPathSnapshot(organizationalPathSnapshot);
        entity.setTopicId(topicId);
        entity.setPeriodStart(periodStart);
        entity.setPeriodEnd(periodEnd);
        entity.setAverageScorePercent(new BigDecimal(averageScorePercent));
        entity.setPassRatePercent(new BigDecimal(passRatePercent));
        entity.setAttemptCount(attemptCount);
        entity.setErrorCount(errorCount);
        entity.setCalculatedAt(CALCULATED_AT);
        entity.setRefreshedAt(REFRESHED_AT);
        entity.setReconciledAt(null);
        return entity;
    }

    private AppUserEntity userEntity(String employeeNumber) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(employeeNumber + "-EXT");
        entity.setLastName("Last");
        entity.setFirstName("First");
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(CALCULATED_AT);
        entity.setUpdatedAt(CALCULATED_AT);
        return entity;
    }

    private ManagerialHistoricalAnalyticsReadRepository.ManagerialUserTopicAnalyticsReadRow userTopicReadRow(
        AppUserEntity user,
        TopicEntity topic,
        Instant periodStart,
        Instant periodEnd,
        String averageScorePercent,
        String passRatePercent,
        Integer attemptCount,
        Integer errorCount
    ) {
        return new ManagerialHistoricalAnalyticsReadRepository.ManagerialUserTopicAnalyticsReadRow(
            user.getId(),
            user.getEmployeeNumber(),
            user.getLastName(),
            user.getFirstName(),
            user.getMiddleName(),
            topic.getId(),
            topic.getName(),
            periodStart,
            periodEnd,
            new BigDecimal(averageScorePercent),
            new BigDecimal(passRatePercent),
            attemptCount,
            errorCount,
            CALCULATED_AT,
            REFRESHED_AT
        );
    }

    private ManagerialHistoricalAnalyticsReadRepository.ManagerialDepartmentTopicAnalyticsReadRow departmentTopicReadRow(
        Long organizationalUnitIdSnapshot,
        String organizationalUnitName,
        String organizationalPathSnapshot,
        TopicEntity topic,
        Instant periodStart,
        Instant periodEnd,
        String averageScorePercent,
        String passRatePercent,
        Integer attemptCount,
        Integer errorCount
    ) {
        return new ManagerialHistoricalAnalyticsReadRepository.ManagerialDepartmentTopicAnalyticsReadRow(
            organizationalUnitIdSnapshot,
            organizationalUnitName,
            organizationalPathSnapshot,
            topic.getId(),
            topic.getName(),
            periodStart,
            periodEnd,
            new BigDecimal(averageScorePercent),
            new BigDecimal(passRatePercent),
            attemptCount,
            errorCount,
            CALCULATED_AT,
            REFRESHED_AT
        );
    }

    private ResultEntity resultEntity(
        Long testAttemptId,
        Long userIdSnapshot,
        Long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot
    ) {
        ResultEntity entity = instantiate(ResultEntity.class);
        entity.setTestAttemptId(testAttemptId);
        entity.setUserIdSnapshot(userIdSnapshot);
        entity.setAttemptMode(com.vladislav.training.platform.common.model.AttemptMode.SELF);
        entity.setAssignmentId(null);
        entity.setAssignmentTestId(null);
        entity.setTestIdSnapshot(3000L + testAttemptId);
        entity.setTestNameSnapshot("Snapshot " + testAttemptId);
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setEarnedScore(new BigDecimal("8.0000"));
        entity.setMaxScore(new BigDecimal("10.0000"));
        entity.setScorePercent(new BigDecimal("80.0000"));
        entity.setPassed(true);
        entity.setWithinDeadline(null);
        entity.setCountedInAssignment(null);
        entity.setScoringPolicyCode("LATEST");
        entity.setScoringPolicySnapshot("{\"policy\":\"LATEST\"}");
        entity.setCompletedAt(CALCULATED_AT.minusSeconds(60));
        entity.setOrganizationalUnitIdSnapshot(organizationalUnitIdSnapshot);
        entity.setOrganizationalPathSnapshot(organizationalPathSnapshot);
        entity.setSnapshotFinalTopicControlFlag(true);
        entity.setCreatedAt(CALCULATED_AT.minusSeconds(120));
        return entity;
    }

    private TestAttemptEntity testAttemptEntity(Long userId, Long testId) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(null);
        entity.setAttemptMode(com.vladislav.training.platform.common.model.AttemptMode.SELF);
        entity.setStatus(TestAttemptStatus.COMPLETED);
        entity.setStartedAt(CALCULATED_AT.minusSeconds(300));
        entity.setCompletedAt(CALCULATED_AT.minusSeconds(60));
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(CALCULATED_AT.minusSeconds(60));
        entity.setCreatedAt(CALCULATED_AT.minusSeconds(300));
        entity.setUpdatedAt(CALCULATED_AT.minusSeconds(60));
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

    private TestEntity testEntity(Long topicId, String suffix) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName("Test " + suffix);
        entity.setDescription("Test");
        entity.setTestType(com.vladislav.training.platform.content.domain.TestType.CONTROL);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setScoringPolicyCode("LATEST");
        entity.setActiveFinalForTopic(true);
        entity.setSortOrder(0);
        entity.setCreatedAt(CALCULATED_AT);
        entity.setUpdatedAt(CALCULATED_AT);
        return entity;
    }

    private OrganizationalUnitTypeEntity unitTypeEntity(String code) {
        OrganizationalUnitTypeEntity entity = instantiate(OrganizationalUnitTypeEntity.class);
        entity.setCode(code);
        entity.setName(code + " Name");
        entity.setNodeKind(com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind.LINEAR);
        entity.setCanBeOperatorHomeUnit(true);
        entity.setCanBeCampaignTarget(true);
        entity.setParticipatesInSubtreeScope(true);
        entity.setCanHaveManagementRelation(true);
        entity.setCanHaveAccessArea(true);
        entity.setDescription(null);
        entity.setCreatedAt(CALCULATED_AT);
        entity.setUpdatedAt(CALCULATED_AT);
        return entity;
    }

    private OrganizationalUnitEntity organizationalUnitEntity(Long unitTypeId, String name, String path) {
        OrganizationalUnitEntity entity = instantiate(OrganizationalUnitEntity.class);
        entity.setParentId(null);
        entity.setOrganizationalUnitTypeId(unitTypeId);
        entity.setName(name);
        entity.setStatus(com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus.ACTIVE);
        entity.setPath(path);
        entity.setDepth(Math.max(0, (int) path.chars().filter(ch -> ch == '/').count() - 1));
        entity.setExternalId("EXT-" + name.replace(" ", "-"));
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
        AnalyticsUserTopicAggregateEntity.class,
        AnalyticsDepartmentTopicAggregateEntity.class,
        ResultEntity.class,
        TestAttemptEntity.class,
        AppUserEntity.class,
        OrganizationalUnitEntity.class,
        OrganizationalUnitTypeEntity.class,
        CourseEntity.class,
        TestEntity.class,
        TopicEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAnalyticsUserTopicAggregateJpaRepository.class,
        SpringDataAnalyticsDepartmentTopicAggregateJpaRepository.class,
        SpringDataResultJpaRepository.class,
        SpringDataTestAttemptJpaRepository.class,
        SpringDataAppUserJpaRepository.class,
        SpringDataOrganizationalUnitJpaRepository.class,
        SpringDataOrganizationalUnitTypeJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataTopicJpaRepository.class
    })
    @ComponentScan(
        basePackageClasses = JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.class
        )
    )
    static class RepositoryTestApplication {
    }
}
