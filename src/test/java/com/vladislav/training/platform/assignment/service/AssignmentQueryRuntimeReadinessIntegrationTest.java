package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.JpaAccessSpecificationPolicy;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignCourseEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentPersistenceMapper;
import com.vladislav.training.platform.assignment.infrastructure.persistence.JpaAssignmentCampaignReadRepositoryAdapter;
import com.vladislav.training.platform.assignment.infrastructure.persistence.JpaAssignmentReadRepositoryAdapter;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignCourseJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentJpaRepository;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.SystemUtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = AssignmentQueryRuntimeReadinessIntegrationTest.AssignmentQueryRuntimeReadinessTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code AssignmentQueryRuntimeReadiness} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class AssignmentQueryRuntimeReadinessIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-08T12:00:00Z");

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
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignJpaRepository;
    @Autowired
    private SpringDataAssignmentCampaignCourseJpaRepository assignmentCampaignCourseJpaRepository;
    @Autowired
    private SpringDataAssignmentJpaRepository assignmentJpaRepository;
    @Autowired
    private SpringDataCourseJpaRepository courseJpaRepository;
    @Autowired
    private SpringDataAppUserJpaRepository appUserJpaRepository;
    @Autowired
    private AssignmentCampaignQueryService assignmentCampaignQueryService;
    @Autowired
    private AssignmentQueryService assignmentQueryService;
    @Autowired
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Autowired
    private AccessFoundationStateReadService accessFoundationStateReadService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(userOrgFoundationStateReadService, accessFoundationStateReadService);
        assignmentJpaRepository.deleteAllInBatch();
        assignmentCampaignCourseJpaRepository.deleteAllInBatch();
        assignmentCampaignJpaRepository.deleteAllInBatch();
        courseJpaRepository.deleteAllInBatch();
        appUserJpaRepository.deleteAllInBatch();
    }

    @Test
    void postLaunchInternalQueryPathsReadThroughRealPersistenceAdaptersWhenPolicyAllows() {
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(eq(101L), any(Instant.class)))
            .thenReturn(new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of()));

        AppUserEntity appUserEntity = appUserJpaRepository.saveAndFlush(appUserEntity());
        CourseEntity courseEntity = courseJpaRepository.saveAndFlush(courseEntity());
        AssignmentCampaignEntity campaignEntity = assignmentCampaignJpaRepository.saveAndFlush(campaignEntity());
        assignmentCampaignCourseJpaRepository.saveAndFlush(campaignCourseEntity(campaignEntity.getId(), courseEntity.getId()));
        AssignmentEntity assignmentEntity = assignmentJpaRepository.saveAndFlush(
            assignmentEntity(campaignEntity.getId(), appUserEntity.getId(), courseEntity.getId())
        );

        assertThat(assignmentCampaignQueryService.findAllAssignmentCampaigns())
            .singleElement()
            .satisfies(campaign -> {
                assertThat(campaign.id()).isEqualTo(campaignEntity.getId());
                assertThat(campaign.sourceType()).isEqualTo("ORG_UNIT");
            });
        assertThat(assignmentCampaignQueryService.findAssignmentCampaignCoursesByCampaignId(campaignEntity.getId()))
            .singleElement()
            .satisfies(course -> {
                assertThat(course.campaignId()).isEqualTo(campaignEntity.getId());
                assertThat(course.courseId()).isEqualTo(courseEntity.getId());
            });
        assertThat(assignmentQueryService.findAssignmentById(assignmentEntity.getId()))
            .satisfies(assignment -> {
                assertThat(assignment.id()).isEqualTo(assignmentEntity.getId());
                assertThat(assignment.campaignId()).isEqualTo(campaignEntity.getId());
                assertThat(assignment.courseId()).isEqualTo(courseEntity.getId());
            });
    }

    @Test
    void postLaunchInternalQueryPathsRemainFailClosedForActorWithoutAssignmentReadAuthority() {
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(eq(101L), any(Instant.class)))
            .thenReturn(new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("OPERATOR")));
        when(accessFoundationStateReadService.findActiveTemporaryRoleIds(eq(101L), any(Instant.class))).thenReturn(Set.of());

        AppUserEntity appUserEntity = appUserJpaRepository.saveAndFlush(appUserEntity());
        CourseEntity courseEntity = courseJpaRepository.saveAndFlush(courseEntity());
        AssignmentCampaignEntity campaignEntity = assignmentCampaignJpaRepository.saveAndFlush(campaignEntity());
        assignmentCampaignCourseJpaRepository.saveAndFlush(campaignCourseEntity(campaignEntity.getId(), courseEntity.getId()));
        AssignmentEntity assignmentEntity = assignmentJpaRepository.saveAndFlush(
            assignmentEntity(campaignEntity.getId(), appUserEntity.getId(), courseEntity.getId())
        );

        assertThatThrownBy(() -> assignmentCampaignQueryService.findAllAssignmentCampaigns())
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized to read post-launch assignment campaign data");
        assertThatThrownBy(() -> assignmentQueryService.findAssignmentById(assignmentEntity.getId()))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized to read assignment detail data");
    }

    private void setAuthentication(TestingAuthenticationToken authentication) {
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private TestingAuthenticationToken authenticatedToken(Long userId, String... authorities) {
        return new TestingAuthenticationToken(userId, null, authorities);
    }

    private AssignmentCampaignEntity campaignEntity() {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Чтение запущенной кампании назначений");
        entity.setDescription("readiness");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("ou-42");
        entity.setSourceNameSnapshot("Operations");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private CourseEntity courseEntity() {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Курс для runtime-чтения назначений");
        entity.setDescription("integration fixture");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(1);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AppUserEntity appUserEntity() {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber("E-101");
        entity.setExternalId("ext-101");
        entity.setLastName("Petrov");
        entity.setFirstName("Petr");
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignCourseEntity campaignCourseEntity(Long campaignId, Long courseId) {
        AssignmentCampaignCourseEntity entity = instantiate(AssignmentCampaignCourseEntity.class);
        entity.setCampaignId(campaignId);
        entity.setCourseId(courseId);
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
        entity.setAssignedAt(FIXED_INSTANT);
        entity.setDeadlineAt(FIXED_INSTANT.plusSeconds(86400));
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
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

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AssignmentCampaignEntity.class,
        AssignmentCampaignCourseEntity.class,
        AssignmentEntity.class,
        CourseEntity.class,
        AppUserEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAssignmentCampaignJpaRepository.class,
        SpringDataAssignmentCampaignCourseJpaRepository.class,
        SpringDataAssignmentJpaRepository.class,
        SpringDataCourseJpaRepository.class,
        SpringDataAppUserJpaRepository.class
    })
    @Import({
        AssignmentPersistenceMapper.class,
        JpaAssignmentCampaignReadRepositoryAdapter.class,
        JpaAssignmentReadRepositoryAdapter.class,
        AssignmentCampaignQueryServiceImpl.class,
        AssignmentQueryServiceImpl.class,
        AuthenticatedActorAdapter.class,
        InteractiveActorResolver.class,
        AccessPolicyQueryContextResolver.class,
        JpaAccessSpecificationPolicy.class,
        SystemUtcClock.class
    })
    static class AssignmentQueryRuntimeReadinessTestApplication {

        @Bean
        UserOrgFoundationStateReadService userOrgFoundationStateReadService() {
            return org.mockito.Mockito.mock(UserOrgFoundationStateReadService.class);
        }

        @Bean
        AccessFoundationStateReadService accessFoundationStateReadService() {
            return org.mockito.Mockito.mock(AccessFoundationStateReadService.class);
        }
    }
}
