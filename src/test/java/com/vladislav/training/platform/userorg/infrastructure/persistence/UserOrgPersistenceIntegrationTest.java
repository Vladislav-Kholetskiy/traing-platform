package com.vladislav.training.platform.userorg.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = UserOrgPersistenceIntegrationTest.UserOrgPersistenceTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code UserOrgPersistence} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class UserOrgPersistenceIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

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
    private SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Autowired
    private SpringDataOrganizationalUnitTypeJpaRepository organizationalUnitTypeRepository;

    @AfterEach
    void cleanDatabase() {
        organizationalUnitRepository.deleteAllInBatch();
        organizationalUnitTypeRepository.deleteAllInBatch();
    }

    @Test
    void saveAndFlushRejectsDuplicatePath() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("DIVISION"));
        organizationalUnitRepository.saveAndFlush(unitEntity(null, unitType.getId(), "Root", "/root", null));

        assertThatThrownBy(() -> organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Other Root", "/root", null)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsDuplicateExternalIdWhenNotNull() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        organizationalUnitRepository.saveAndFlush(unitEntity(null, unitType.getId(), "Root", "/root", "ext-1"));

        assertThatThrownBy(() -> organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Other Root", "/other", "ext-1")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsBrokenTypeForeignKey() {
        assertThatThrownBy(() -> organizationalUnitRepository.saveAndFlush(
            unitEntity(null, 999_999L, "Broken Type", "/broken-type", null)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsBrokenParentForeignKey() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("REGION"));

        assertThatThrownBy(() -> organizationalUnitRepository.saveAndFlush(
            unitEntity(999_999L, unitType.getId(), "Broken Parent", "/broken-parent", null)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private OrganizationalUnitTypeEntity unitTypeEntity(String code) {
        OrganizationalUnitTypeEntity entity = new OrganizationalUnitTypeEntity();
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

    private OrganizationalUnitEntity unitEntity(
        Long parentId,
        Long unitTypeId,
        String name,
        String path,
        String externalId
    ) {
        OrganizationalUnitEntity entity = new OrganizationalUnitEntity();
        entity.setParentId(parentId);
        entity.setOrganizationalUnitTypeId(unitTypeId);
        entity.setName(name);
        entity.setStatus(OrganizationalUnitStatus.ACTIVE);
        entity.setPath(path);
        entity.setDepth(Math.max(0, (int) path.chars().filter(ch -> ch == '/').count() - 1));
        entity.setExternalId(externalId);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }
@SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataOrganizationalUnitJpaRepository.class,
        SpringDataOrganizationalUnitTypeJpaRepository.class
    })
    static class UserOrgPersistenceTestApplication {
    }
}