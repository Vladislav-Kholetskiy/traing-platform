package com.vladislav.training.platform.access.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppRoleEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitTypeEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppRoleJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataOrganizationalUnitJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataOrganizationalUnitTypeJpaRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        classes = AccessManagementPersistenceIntegrationTest.AccessManagementPersistenceTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code AccessManagementPersistence} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class AccessManagementPersistenceIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T10:00:00Z");

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
    private SpringDataAppUserJpaRepository appUserRepository;
    @Autowired
    private SpringDataAppRoleJpaRepository appRoleRepository;
    @Autowired
    private SpringDataOrganizationalUnitTypeJpaRepository organizationalUnitTypeRepository;
    @Autowired
    private SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Autowired
    private SpringDataUserAccessAreaJpaRepository userAccessAreaRepository;
    @Autowired
    private SpringDataManagementRelationJpaRepository managementRelationRepository;
    @Autowired
    private SpringDataManagementRelationTypeJpaRepository managementRelationTypeRepository;
    @Autowired
    private SpringDataTemporaryRoleAssignmentJpaRepository temporaryRoleAssignmentRepository;
    @Autowired
    private SpringDataTemporaryAccessAreaJpaRepository temporaryAccessAreaRepository;
    @Autowired
    private SpringDataTemporaryManagementDelegationJpaRepository temporaryManagementDelegationRepository;

    @AfterEach
    void cleanDatabase() {
        temporaryManagementDelegationRepository.deleteAllInBatch();
        temporaryAccessAreaRepository.deleteAllInBatch();
        temporaryRoleAssignmentRepository.deleteAllInBatch();
        managementRelationRepository.deleteAllInBatch();
        userAccessAreaRepository.deleteAllInBatch();
        managementRelationTypeRepository.deleteAllInBatch();
        appRoleRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        organizationalUnitRepository.deleteAllInBatch();
        organizationalUnitTypeRepository.deleteAllInBatch();
    }

    @Test
    void saveAndFlushRejectsOverlappingGlobalAccessArea() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1"));
        userAccessAreaRepository.saveAndFlush(
                accessAreaEntity(user.getId(), null, AccessScopeType.GLOBAL, FIXED_INSTANT.minusSeconds(7200), null)
        );

        assertThatThrownBy(() -> userAccessAreaRepository.saveAndFlush(
                accessAreaEntity(user.getId(), null, AccessScopeType.GLOBAL, FIXED_INSTANT.minusSeconds(3600), null)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsOverlappingScopedAccessArea() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1"));
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity unit = organizationalUnitRepository.saveAndFlush(unitEntity(unitType.getId(), "Team", "/team"));
        userAccessAreaRepository.saveAndFlush(accessAreaEntity(
                user.getId(),
                unit.getId(),
                AccessScopeType.UNIT_ONLY,
                FIXED_INSTANT.minusSeconds(7200),
                null
        ));

        assertThatThrownBy(() -> userAccessAreaRepository.saveAndFlush(accessAreaEntity(
                user.getId(),
                unit.getId(),
                AccessScopeType.UNIT_ONLY,
                FIXED_INSTANT.minusSeconds(3600),
                null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsOverlappingManagementRelation() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1"));
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity unit = organizationalUnitRepository.saveAndFlush(unitEntity(unitType.getId(), "Team", "/team"));
        ManagementRelationTypeEntity relationType = managementRelationTypeRepository.saveAndFlush(managementRelationTypeEntity("SUPERVISOR"));
        managementRelationRepository.saveAndFlush(managementRelationEntity(
                user.getId(),
                unit.getId(),
                relationType.getId(),
                FIXED_INSTANT.minusSeconds(7200),
                null
        ));

        assertThatThrownBy(() -> managementRelationRepository.saveAndFlush(managementRelationEntity(
                user.getId(),
                unit.getId(),
                relationType.getId(),
                FIXED_INSTANT.minusSeconds(3600),
                null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsOverlappingTemporaryRoleAssignment() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1"));
        AppRoleEntity role = appRoleRepository.saveAndFlush(roleEntity("OPERATOR"));
        temporaryRoleAssignmentRepository.saveAndFlush(temporaryRoleAssignmentEntity(
                user.getId(),
                role.getId(),
                FIXED_INSTANT.minusSeconds(7200),
                null
        ));

        assertThatThrownBy(() -> temporaryRoleAssignmentRepository.saveAndFlush(temporaryRoleAssignmentEntity(
                user.getId(),
                role.getId(),
                FIXED_INSTANT.minusSeconds(3600),
                null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsOverlappingTemporaryAccessArea() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1"));
        temporaryAccessAreaRepository.saveAndFlush(temporaryAccessAreaEntity(
                user.getId(),
                null,
                AccessScopeType.GLOBAL,
                FIXED_INSTANT.minusSeconds(7200),
                null
        ));

        assertThatThrownBy(() -> temporaryAccessAreaRepository.saveAndFlush(temporaryAccessAreaEntity(
                user.getId(),
                null,
                AccessScopeType.GLOBAL,
                FIXED_INSTANT.minusSeconds(3600),
                null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsOverlappingTemporaryManagementDelegation() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1"));
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity unit = organizationalUnitRepository.saveAndFlush(unitEntity(unitType.getId(), "Team", "/team"));
        ManagementRelationTypeEntity relationType = managementRelationTypeRepository.saveAndFlush(managementRelationTypeEntity("SUPERVISOR"));
        temporaryManagementDelegationRepository.saveAndFlush(temporaryManagementDelegationEntity(
                user.getId(),
                unit.getId(),
                relationType.getId(),
                FIXED_INSTANT.minusSeconds(7200),
                null
        ));

        assertThatThrownBy(() -> temporaryManagementDelegationRepository.saveAndFlush(temporaryManagementDelegationEntity(
                user.getId(),
                unit.getId(),
                relationType.getId(),
                FIXED_INSTANT.minusSeconds(3600),
                null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private AppUserEntity userEntity(String employeeNumber) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setLastName("Last");
        entity.setFirstName("First");
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AppRoleEntity roleEntity(String code) {
        AppRoleEntity entity = instantiate(AppRoleEntity.class);
        entity.setCode(code);
        entity.setName(code + " Name");
        entity.setDescription(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private OrganizationalUnitTypeEntity unitTypeEntity(String code) {
        OrganizationalUnitTypeEntity entity = instantiate(OrganizationalUnitTypeEntity.class);
        entity.setCode(code);
        entity.setName(code + " Name");
        entity.setNodeKind(OrganizationalNodeKind.LINEAR);
        entity.setCanBeOperatorHomeUnit(false);
        entity.setCanBeCampaignTarget(true);
        entity.setParticipatesInSubtreeScope(true);
        entity.setCanHaveManagementRelation(true);
        entity.setCanHaveAccessArea(true);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private OrganizationalUnitEntity unitEntity(Long unitTypeId, String name, String path) {
        OrganizationalUnitEntity entity = instantiate(OrganizationalUnitEntity.class);
        entity.setParentId(null);
        entity.setOrganizationalUnitTypeId(unitTypeId);
        entity.setName(name);
        entity.setStatus(OrganizationalUnitStatus.ACTIVE);
        entity.setPath(path);
        entity.setDepth(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private UserAccessAreaEntity accessAreaEntity(
            Long userId,
            Long organizationalUnitId,
            AccessScopeType accessScopeType,
            Instant validFrom,
            Instant validTo
    ) {
        UserAccessAreaEntity entity = new UserAccessAreaEntity();
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(organizationalUnitId);
        entity.setAccessScopeType(accessScopeType);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TemporaryAccessAreaEntity temporaryAccessAreaEntity(
            Long userId,
            Long organizationalUnitId,
            AccessScopeType accessScopeType,
            Instant validFrom,
            Instant validTo
    ) {
        TemporaryAccessAreaEntity entity = new TemporaryAccessAreaEntity();
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(organizationalUnitId);
        entity.setAccessScopeType(accessScopeType);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TemporaryRoleAssignmentEntity temporaryRoleAssignmentEntity(
            Long userId,
            Long roleId,
            Instant validFrom,
            Instant validTo
    ) {
        TemporaryRoleAssignmentEntity entity = new TemporaryRoleAssignmentEntity();
        entity.setUserId(userId);
        entity.setRoleId(roleId);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private ManagementRelationTypeEntity managementRelationTypeEntity(String code) {
        ManagementRelationTypeEntity entity = new ManagementRelationTypeEntity();
        entity.setCode(code);
        entity.setName(code + " Name");
        entity.setDescription(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private ManagementRelationEntity managementRelationEntity(
            Long userId,
            Long organizationalUnitId,
            Long managementRelationTypeId,
            Instant validFrom,
            Instant validTo
    ) {
        ManagementRelationEntity entity = new ManagementRelationEntity();
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(organizationalUnitId);
        entity.setManagementRelationTypeId(managementRelationTypeId);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TemporaryManagementDelegationEntity temporaryManagementDelegationEntity(
            Long userId,
            Long organizationalUnitId,
            Long managementRelationTypeId,
            Instant validFrom,
            Instant validTo
    ) {
        TemporaryManagementDelegationEntity entity = new TemporaryManagementDelegationEntity();
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(organizationalUnitId);
        entity.setManagementRelationTypeId(managementRelationTypeId);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
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
            AppUserEntity.class,
            AppRoleEntity.class,
            OrganizationalUnitEntity.class,
            OrganizationalUnitTypeEntity.class,
            UserAccessAreaEntity.class,
            ManagementRelationEntity.class,
            ManagementRelationTypeEntity.class,
            TemporaryRoleAssignmentEntity.class,
            TemporaryAccessAreaEntity.class,
            TemporaryManagementDelegationEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            SpringDataAppUserJpaRepository.class,
            SpringDataAppRoleJpaRepository.class,
            SpringDataOrganizationalUnitJpaRepository.class,
            SpringDataOrganizationalUnitTypeJpaRepository.class,
            SpringDataUserAccessAreaJpaRepository.class,
            SpringDataManagementRelationJpaRepository.class,
            SpringDataManagementRelationTypeJpaRepository.class,
            SpringDataTemporaryRoleAssignmentJpaRepository.class,
            SpringDataTemporaryAccessAreaJpaRepository.class,
            SpringDataTemporaryManagementDelegationJpaRepository.class
    })
    static class AccessManagementPersistenceTestApplication {
    }
}