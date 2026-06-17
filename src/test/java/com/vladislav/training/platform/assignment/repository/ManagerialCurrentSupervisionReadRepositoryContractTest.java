package com.vladislav.training.platform.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentTestEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.JpaManagerialCurrentSupervisionReadRepositoryAdapter;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentTestJpaRepository;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitTypeEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataOrganizationalUnitJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataOrganizationalUnitTypeJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataUserOrganizationAssignmentJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.UserOrganizationAssignmentEntity;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = ManagerialCurrentSupervisionReadRepositoryContractTest.ManagerialCurrentSupervisionReadRepositoryTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет договорённости вокруг {@code ManagerialCurrentSupervisionReadRepository}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@Testcontainers(disabledWithoutDocker = true)
class ManagerialCurrentSupervisionReadRepositoryContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T23:00:00Z");

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
    private ManagerialCurrentSupervisionReadRepository managerialCurrentSupervisionReadRepository;
    @Autowired
    private SpringDataAssignmentTestJpaRepository assignmentTestRepository;
    @Autowired
    private SpringDataAssignmentJpaRepository assignmentRepository;
    @Autowired
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignRepository;
    @Autowired
    private SpringDataCourseJpaRepository courseRepository;
    @Autowired
    private SpringDataTopicJpaRepository topicRepository;
    @Autowired
    private SpringDataTestJpaRepository testRepository;
    @Autowired
    private SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository;
    @Autowired
    private SpringDataAppUserJpaRepository appUserRepository;
    @Autowired
    private SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Autowired
    private SpringDataOrganizationalUnitTypeJpaRepository organizationalUnitTypeRepository;

    @AfterEach
    void cleanDatabase() {
        assignmentTestRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        testRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        assignmentCampaignRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userOrganizationAssignmentRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        organizationalUnitRepository.deleteAllInBatch();
        organizationalUnitTypeRepository.deleteAllInBatch();
    }

    @Test
    void readCriteriaRequiresManagerialReadScope() {
        ManagerialReadScope scope = ManagerialReadScope.denyAll(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );

        var criteria = new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(scope);

        assertThat(criteria.managerialReadScope()).isSameAs(scope);
        assertThatThrownBy(() -> new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("managerialReadScope must not be null");
    }

    @Test
    void readRowShapeMatchesCurrentSupervisionFields() {
        var row = new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow(
            77L,
            101L,
            "Ivanov Ivan Ivanovich",
            501L,
            "Labor Safety",
            701L,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.plusSeconds(7200),
            AssignmentStatus.ASSIGNED
        );

        assertThat(row.assignmentId()).isEqualTo(77L);
        assertThat(row.userId()).isEqualTo(101L);
        assertThat(row.userDisplayName()).isEqualTo("Ivanov Ivan Ivanovich");
        assertThat(row.courseId()).isEqualTo(501L);
        assertThat(row.courseName()).isEqualTo("Labor Safety");
        assertThat(row.assignmentTestCount()).isEqualTo(701L);
        assertThat(row.assignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(componentNames(ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow.class))
            .containsExactly(
                "assignmentId",
                "userId",
                "userDisplayName",
                "courseId",
                "courseName",
                "assignmentTestCount",
                "assignedAt",
                "deadlineAt",
                "assignmentStatus"
            );
    }

    @Test
    void denyAllScopeReturnsEmptyRowsBeforeJpaQueryCreation() {
        EntityManager entityManager = Mockito.mock(EntityManager.class);
        var adapter = new JpaManagerialCurrentSupervisionReadRepositoryAdapter(entityManager);
        var criteria = new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(
            ManagerialReadScope.denyAll(101L, FIXED_INSTANT, AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION)
        );

        assertThat(adapter.findCurrentSupervisionRows(criteria)).isEmpty();
        verifyNoInteractions(entityManager);
    }

    @Test
    void fullAccessScopeReturnsCurrentSupervisionRowsFromAssignmentAndAssignmentTestState() {
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(campaignEntity("FULL"));
        CourseEntity firstCourse = courseRepository.saveAndFlush(courseEntity("FULL-1"));
        CourseEntity secondCourse = courseRepository.saveAndFlush(courseEntity("FULL-2"));
        TopicEntity firstTopic = topicRepository.saveAndFlush(topicEntity(firstCourse.getId(), "FULL-T1"));
        TopicEntity secondTopic = topicRepository.saveAndFlush(topicEntity(secondCourse.getId(), "FULL-T2"));
        TestEntity firstTest = testRepository.saveAndFlush(testEntity(firstTopic.getId(), "FULL-TEST-1"));
        TestEntity secondTest = testRepository.saveAndFlush(testEntity(secondTopic.getId(), "FULL-TEST-2"));
        AppUserEntity firstUser = appUserRepository.saveAndFlush(userEntity("EMP-FULL-1"));
        AppUserEntity secondUser = appUserRepository.saveAndFlush(userEntity("EMP-FULL-2"));
        AssignmentEntity firstAssignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), firstUser.getId(), firstCourse.getId(), FIXED_INSTANT.minusSeconds(7200))
        );
        AssignmentEntity secondAssignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), secondUser.getId(), secondCourse.getId(), FIXED_INSTANT.minusSeconds(3600))
        );
        AssignmentTestEntity firstAssignmentTest = assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(firstAssignment.getId(), firstTest.getId(), FIXED_INSTANT.minusSeconds(7200))
        );
        AssignmentTestEntity secondAssignmentTest = assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(secondAssignment.getId(), secondTest.getId(), FIXED_INSTANT.minusSeconds(3600))
        );

        List<ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow> rows =
            managerialCurrentSupervisionReadRepository.findCurrentSupervisionRows(
                new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(
                    managerialScope(AccessReadScope.fullAccess())
                )
            );

        assertThat(rows).containsExactly(
            supervisionRow(firstAssignment, firstAssignmentTest, firstUser, firstCourse),
            supervisionRow(secondAssignment, secondAssignmentTest, secondUser, secondCourse)
        );
    }

    @Test
    void scopedUnitVisibilityReturnsOnlyRowsInsideAllowedManagerialScope() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity visibleUnit = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Visible", "/root/visible")
        );
        OrganizationalUnitEntity hiddenUnit = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Hidden", "/root/hidden")
        );
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(campaignEntity("SCOPED"));
        CourseEntity visibleCourse = courseRepository.saveAndFlush(courseEntity("SCOPED-1"));
        CourseEntity hiddenCourse = courseRepository.saveAndFlush(courseEntity("SCOPED-2"));
        TopicEntity visibleTopic = topicRepository.saveAndFlush(topicEntity(visibleCourse.getId(), "SCOPED-T1"));
        TopicEntity hiddenTopic = topicRepository.saveAndFlush(topicEntity(hiddenCourse.getId(), "SCOPED-T2"));
        TestEntity visibleTest = testRepository.saveAndFlush(testEntity(visibleTopic.getId(), "SCOPED-TEST-1"));
        TestEntity hiddenTest = testRepository.saveAndFlush(testEntity(hiddenTopic.getId(), "SCOPED-TEST-2"));
        AppUserEntity visibleUser = appUserRepository.saveAndFlush(userEntity("EMP-VISIBLE"));
        AppUserEntity hiddenUser = appUserRepository.saveAndFlush(userEntity("EMP-HIDDEN"));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            visibleUser.getId(),
            visibleUnit.getId(),
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            hiddenUser.getId(),
            hiddenUnit.getId(),
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));
        AssignmentEntity visibleAssignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), visibleUser.getId(), visibleCourse.getId(), FIXED_INSTANT.minusSeconds(7200))
        );
        AssignmentEntity hiddenAssignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), hiddenUser.getId(), hiddenCourse.getId(), FIXED_INSTANT.minusSeconds(3600))
        );
        AssignmentTestEntity visibleAssignmentTest = assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(visibleAssignment.getId(), visibleTest.getId(), FIXED_INSTANT.minusSeconds(7200))
        );
        assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(hiddenAssignment.getId(), hiddenTest.getId(), FIXED_INSTANT.minusSeconds(3600))
        );

        List<ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow> rows =
            managerialCurrentSupervisionReadRepository.findCurrentSupervisionRows(
                new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(
                    managerialScope(AccessReadScope.scoped(Set.of(visibleUnit.getId()), Set.of()))
                )
            );

        assertThat(rows).containsExactly(
            supervisionRow(visibleAssignment, visibleAssignmentTest, visibleUser, visibleCourse)
        );
    }

    @Test
    void adapterUsesManagerialVisibleUsersRestrictionAsPreFilterAndAvoidsForbiddenCollaborators() throws IOException {
        String source = read(
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/"
                + "JpaManagerialCurrentSupervisionReadRepositoryAdapter.java"
        );

        assertThat(source)
            .contains("ManagerialVisibleUsersRestrictionBuilder.build")
            .contains("restriction.toSpecification(\"userId\")")
            .contains("AssignmentEntity")
            .contains("AssignmentTestEntity")
            .doesNotContain("analytics.repository")
            .doesNotContain("AnalyticsCampaignAggregateRepository")
            .doesNotContain("AnalyticsCampaignAggregate")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("recalculate")
            .doesNotContain("rebuild")
            .doesNotContain("refresh")
            .doesNotContain("recordResult")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("update(")
            .doesNotContain("flush(")
            .doesNotContain("CommandService");

        assertThat(fieldTypes(JpaManagerialCurrentSupervisionReadRepositoryAdapter.class))
            .containsExactly(EntityManager.class)
            .doesNotContain(
                AssignmentCommandService.class,
                AssignmentStatusRecalculationService.class,
                ResultRecordingService.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class,
                CapabilityAdmissionPolicy.class,
                ManagementRelationRepository.class
            );
    }

    @Test
    void readPortStaysIndependentFromCommandResultAnalyticsAdmissionAndManagementRelationDependencies() throws IOException {
        String source = read(
            "src/main/java/com/vladislav/training/platform/assignment/repository/"
                + "ManagerialCurrentSupervisionReadRepository.java"
        );

        assertThat(source)
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("recordResult")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("update(")
            .doesNotContain("flush(");
    }

    @Test
    void managerialCurrentSupervisionReadPathStaysPureQueryAcrossContractPortAndAdapterSources() throws IOException {
        assertPureReadSource("src/main/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryService.java");
        assertPureReadSource("src/main/java/com/vladislav/training/platform/assignment/repository/ManagerialCurrentSupervisionReadRepository.java");
        assertPureReadSource(
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/"
                + "JpaManagerialCurrentSupervisionReadRepositoryAdapter.java"
        );
    }

    private java.util.List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private void assertPureReadSource(String relativePath) throws IOException {
        assertThat(read(relativePath))
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AssignmentAdministrativeActionService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("recordResult")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("update(")
            .doesNotContain("flush(")
            .doesNotContain("CommandService");
    }

    private ManagerialReadScope managerialScope(AccessReadScope readScope) {
        return new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            readScope
        );
    }

    private AppUserEntity userEntity(String employeeNumber) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(employeeNumber + "-EXT");
        entity.setLastName("Last");
        entity.setFirstName("First");
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignEntity campaignEntity(String suffix) {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Campaign " + suffix);
        entity.setDescription("Campaign");
        entity.setSourceType("MANUAL");
        entity.setSourceRef("SRC-" + suffix);
        entity.setSourceNameSnapshot("Campaign " + suffix);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private CourseEntity courseEntity(String suffix) {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Course " + suffix);
        entity.setDescription("Course");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId, String suffix) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName("Topic " + suffix);
        entity.setDescription("Topic");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestEntity testEntity(Long topicId, String suffix) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName("Test " + suffix);
        entity.setDescription("Test");
        entity.setTestType(TestType.CONTROL);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(BigDecimal.valueOf(70));
        entity.setScoringPolicyCode("DEFAULT");
        entity.setActiveFinalForTopic(true);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentEntity assignmentEntity(Long campaignId, Long userId, Long courseId, Instant assignedAt) {
        AssignmentEntity entity = instantiate(AssignmentEntity.class);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setStatus(AssignmentStatus.ASSIGNED);
        entity.setAssignedAt(assignedAt);
        entity.setDeadlineAt(assignedAt.plusSeconds(86_400));
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
        entity.setCreatedAt(assignedAt);
        entity.setUpdatedAt(assignedAt.plusSeconds(60));
        return entity;
    }

    private AssignmentTestEntity assignmentTestEntity(Long assignmentId, Long testId, Instant createdAt) {
        AssignmentTestEntity entity = instantiate(AssignmentTestEntity.class);
        entity.setAssignmentId(assignmentId);
        entity.setTestId(testId);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(null);
        entity.setClosedAt(null);
        entity.setClosed(false);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt.plusSeconds(60));
        return entity;
    }

    private UserOrganizationAssignmentEntity orgAssignmentEntity(
        Long userId,
        Long organizationalUnitId,
        Instant validFrom,
        Instant validTo
    ) {
        UserOrganizationAssignmentEntity entity = instantiate(UserOrganizationAssignmentEntity.class);
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(organizationalUnitId);
        entity.setAssignmentType(OrganizationAssignmentType.PRIMARY);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private OrganizationalUnitTypeEntity unitTypeEntity(String code) {
        OrganizationalUnitTypeEntity entity = instantiate(OrganizationalUnitTypeEntity.class);
        entity.setCode(code);
        entity.setName(code + " Name");
        entity.setNodeKind(OrganizationalNodeKind.LINEAR);
        entity.setCanBeOperatorHomeUnit(true);
        entity.setCanBeCampaignTarget(true);
        entity.setParticipatesInSubtreeScope(true);
        entity.setCanHaveManagementRelation(true);
        entity.setCanHaveAccessArea(true);
        entity.setDescription(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private OrganizationalUnitEntity unitEntity(Long parentId, Long unitTypeId, String name, String path) {
        OrganizationalUnitEntity entity = instantiate(OrganizationalUnitEntity.class);
        entity.setParentId(parentId);
        entity.setOrganizationalUnitTypeId(unitTypeId);
        entity.setName(name);
        entity.setStatus(OrganizationalUnitStatus.ACTIVE);
        entity.setPath(path);
        entity.setDepth(Math.max(0, (int) path.chars().filter(ch -> ch == '/').count() - 1));
        entity.setExternalId(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow supervisionRow(
        AssignmentEntity assignment,
        AssignmentTestEntity assignmentTest,
        AppUserEntity user,
        CourseEntity course
    ) {
        return new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow(
            assignment.getId(),
            user.getId(),
            user.getLastName() + " " + user.getFirstName(),
            assignment.getCourseId(),
            course.getName(),
            1L,
            assignment.getAssignedAt(),
            assignment.getDeadlineAt(),
            assignment.getStatus()
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

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AssignmentCampaignEntity.class,
        AssignmentEntity.class,
        AssignmentTestEntity.class,
        CourseEntity.class,
        TopicEntity.class,
        TestEntity.class,
        AppUserEntity.class,
        OrganizationalUnitEntity.class,
        OrganizationalUnitTypeEntity.class,
        UserOrganizationAssignmentEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAssignmentCampaignJpaRepository.class,
        SpringDataAssignmentJpaRepository.class,
        SpringDataAssignmentTestJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataTopicJpaRepository.class,
        SpringDataTestJpaRepository.class,
        SpringDataAppUserJpaRepository.class,
        SpringDataOrganizationalUnitJpaRepository.class,
        SpringDataOrganizationalUnitTypeJpaRepository.class,
        SpringDataUserOrganizationAssignmentJpaRepository.class
    })
    @ComponentScan(
        basePackageClasses = JpaManagerialCurrentSupervisionReadRepositoryAdapter.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JpaManagerialCurrentSupervisionReadRepositoryAdapter.class
        )
    )
    static class ManagerialCurrentSupervisionReadRepositoryTestApplication {
    }
}
