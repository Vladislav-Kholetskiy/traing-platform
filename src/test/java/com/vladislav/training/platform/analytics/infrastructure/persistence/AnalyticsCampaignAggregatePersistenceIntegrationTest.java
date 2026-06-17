package com.vladislav.training.platform.analytics.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = AnalyticsCampaignAggregatePersistenceIntegrationTest.AnalyticsCampaignAggregatePersistenceTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code AnalyticsCampaignAggregatePersistence} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class AnalyticsCampaignAggregatePersistenceIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-06T15:00:00Z");

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
    private com.vladislav.training.platform.analytics.repository.AnalyticsCampaignAggregateRepository analyticsCampaignAggregateRepository;
    @Autowired
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignRepository;

    @AfterEach
    void tearDown() {
        analyticsCampaignAggregateRepository.deleteAllCampaignAggregates();
        assignmentCampaignRepository.deleteAllInBatch();
    }

    @Test
    void saveAndReadByIdCampaignIdAndListPreserveNumericAndTimestampFields() {
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(assignmentCampaignEntity("SRC-1"));

        AnalyticsCampaignAggregate saved = analyticsCampaignAggregateRepository.saveCampaignAggregate(
            aggregate(
                null,
                campaign.getId(),
                25,
                20,
                11,
                4,
                9,
                5,
                "55.5000",
                "20.0000",
                FIXED_INSTANT,
                FIXED_INSTANT.plusSeconds(30),
                FIXED_INSTANT.plusSeconds(60)
            )
        );

        AnalyticsCampaignAggregate byId = analyticsCampaignAggregateRepository.findCampaignAggregateById(saved.id());
        AnalyticsCampaignAggregate byCampaignId = analyticsCampaignAggregateRepository.findCampaignAggregateByCampaignId(campaign.getId());
        List<AnalyticsCampaignAggregate> allAggregates = analyticsCampaignAggregateRepository.findAllCampaignAggregates();

        assertThat(saved.id()).isNotNull();
        assertThat(byId).isEqualTo(saved);
        assertThat(byCampaignId).isEqualTo(saved);
        assertThat(allAggregates).containsExactly(saved);
        assertThat(saved.recipientSnapshotCount()).isEqualTo(25);
        assertThat(saved.nonCancelledAssignmentsFromCampaignSnapshot()).isEqualTo(20);
        assertThat(saved.completedAssignments()).isEqualTo(11);
        assertThat(saved.overdueAssignments()).isEqualTo(4);
        assertThat(saved.nonCancelledActivePool()).isEqualTo(9);
        assertThat(saved.cancelledAssignments()).isEqualTo(5);
        assertThat(saved.coveragePercent()).isEqualByComparingTo("55.5000");
        assertThat(saved.overduePercent()).isEqualByComparingTo("20.0000");
        assertThat(saved.calculatedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(saved.refreshedAt()).isEqualTo(FIXED_INSTANT.plusSeconds(30));
        assertThat(saved.reconciledAt()).isEqualTo(FIXED_INSTANT.plusSeconds(60));
    }

    @Test
    void saveWithExistingIdUpdatesStoredAggregateWithoutCreatingSecondRow() {
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(assignmentCampaignEntity("SRC-2"));
        AnalyticsCampaignAggregate initial = analyticsCampaignAggregateRepository.saveCampaignAggregate(
            aggregate(
                null,
                campaign.getId(),
                10,
                9,
                4,
                1,
                5,
                1,
                "40.0000",
                "10.0000",
                FIXED_INSTANT,
                FIXED_INSTANT.plusSeconds(10),
                null
            )
        );

        AnalyticsCampaignAggregate updated = analyticsCampaignAggregateRepository.saveCampaignAggregate(
            aggregate(
                initial.id(),
                campaign.getId(),
                12,
                11,
                7,
                2,
                4,
                1,
                "63.5000",
                "18.2000",
                FIXED_INSTANT.plusSeconds(120),
                FIXED_INSTANT.plusSeconds(180),
                FIXED_INSTANT.plusSeconds(240)
            )
        );

        assertThat(updated.id()).isEqualTo(initial.id());
        assertThat(analyticsCampaignAggregateRepository.findAllCampaignAggregates()).hasSize(1);
        assertThat(analyticsCampaignAggregateRepository.findCampaignAggregateByCampaignId(campaign.getId()))
            .extracting(
                AnalyticsCampaignAggregate::recipientSnapshotCount,
                AnalyticsCampaignAggregate::nonCancelledAssignmentsFromCampaignSnapshot,
                AnalyticsCampaignAggregate::completedAssignments,
                AnalyticsCampaignAggregate::overdueAssignments,
                AnalyticsCampaignAggregate::nonCancelledActivePool,
                AnalyticsCampaignAggregate::cancelledAssignments,
                AnalyticsCampaignAggregate::coveragePercent,
                AnalyticsCampaignAggregate::overduePercent,
                AnalyticsCampaignAggregate::calculatedAt,
                AnalyticsCampaignAggregate::refreshedAt,
                AnalyticsCampaignAggregate::reconciledAt
            )
            .containsExactly(
                12,
                11,
                7,
                2,
                4,
                1,
                new BigDecimal("63.5000"),
                new BigDecimal("18.2000"),
                FIXED_INSTANT.plusSeconds(120),
                FIXED_INSTANT.plusSeconds(180),
                FIXED_INSTANT.plusSeconds(240)
            );
    }

    @Test
    void canonicalCampaignKeyUniquenessAndDeleteAllAreEnforced() {
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(assignmentCampaignEntity("SRC-3"));
        analyticsCampaignAggregateRepository.saveCampaignAggregate(
            aggregate(
                null,
                campaign.getId(),
                9,
                8,
                2,
                1,
                6,
                1,
                "22.2000",
                "11.1000",
                FIXED_INSTANT,
                FIXED_INSTANT.plusSeconds(15),
                null
            )
        );

        assertThatThrownBy(() -> analyticsCampaignAggregateRepository.saveCampaignAggregate(
            aggregate(
                null,
                campaign.getId(),
                10,
                9,
                3,
                2,
                4,
                1,
                "33.3000",
                "22.2000",
                FIXED_INSTANT.plusSeconds(60),
                FIXED_INSTANT.plusSeconds(90),
                null
            )
        )).isInstanceOf(DataIntegrityViolationException.class);

        analyticsCampaignAggregateRepository.deleteAllCampaignAggregates();
        assertThat(analyticsCampaignAggregateRepository.findAllCampaignAggregates()).isEmpty();
    }

    private AnalyticsCampaignAggregate aggregate(
        Long id,
        Long campaignId,
        int recipientSnapshotCount,
        int nonCancelledAssignmentsFromCampaignSnapshot,
        int completedAssignments,
        int overdueAssignments,
        int nonCancelledActivePool,
        int cancelledAssignments,
        String coveragePercent,
        String overduePercent,
        Instant calculatedAt,
        Instant refreshedAt,
        Instant reconciledAt
    ) {
        return new AnalyticsCampaignAggregate(
            id,
            campaignId,
            recipientSnapshotCount,
            nonCancelledAssignmentsFromCampaignSnapshot,
            completedAssignments,
            overdueAssignments,
            nonCancelledActivePool,
            cancelledAssignments,
            new BigDecimal(coveragePercent),
            new BigDecimal(overduePercent),
            calculatedAt,
            refreshedAt,
            reconciledAt
        );
    }

    private AssignmentCampaignEntity assignmentCampaignEntity(String sourceRef) {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Campaign " + sourceRef);
        entity.setDescription("Analytics campaign aggregate persistence test");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef(sourceRef);
        entity.setSourceNameSnapshot("Operations");
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
        AnalyticsCampaignAggregateEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAssignmentCampaignJpaRepository.class,
        SpringDataAnalyticsCampaignAggregateJpaRepository.class
    })
    @Import(JpaAnalyticsCampaignAggregateRepositoryAdapter.class)
    static class AnalyticsCampaignAggregatePersistenceTestApplication {
    }
}
