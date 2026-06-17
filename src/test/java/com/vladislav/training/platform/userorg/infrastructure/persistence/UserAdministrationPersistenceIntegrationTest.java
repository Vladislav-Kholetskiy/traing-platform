package com.vladislav.training.platform.userorg.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = UserAdministrationPersistenceIntegrationTest.UserAdministrationPersistenceTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code UserAdministrationPersistence} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class UserAdministrationPersistenceIntegrationTest {

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
    private SpringDataAppUserJpaRepository appUserRepository;
    @Autowired
    private SpringDataAppRoleJpaRepository appRoleRepository;
    @Autowired
    private SpringDataUserRoleAssignmentJpaRepository userRoleAssignmentRepository;
    @Autowired
    private SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository;
    @Autowired
    private SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Autowired
    private SpringDataOrganizationalUnitTypeJpaRepository organizationalUnitTypeRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        userOrganizationAssignmentRepository.deleteAllInBatch();
        userRoleAssignmentRepository.deleteAllInBatch();
        appRoleRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        organizationalUnitRepository.deleteAllInBatch();
        organizationalUnitTypeRepository.deleteAllInBatch();
    }

    @Test
    void migrationStackDoesNotExposeIdentifierReservationTableAtRuntime() {
        Integer tableCount = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where table_schema = 'public'
              and table_name = 'app_user_identifier_reservation'
            """,
            Integer.class
        );

        assertThat(tableCount).isZero();
    }

    @Test
    void saveAndFlushRejectsDuplicateEmployeeNumber() {
        appUserRepository.saveAndFlush(userEntity("EMP-1", null));

        assertThatThrownBy(() -> appUserRepository.saveAndFlush(userEntity("EMP-1", null)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsDuplicateExternalIdWhenNotNull() {
        appUserRepository.saveAndFlush(userEntity("EMP-1", "EXT-1"));

        assertThatThrownBy(() -> appUserRepository.saveAndFlush(userEntity("EMP-2", "EXT-1")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsOverlappingUserRolePeriod() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1", null));
        AppRoleEntity role = appRoleRepository.saveAndFlush(roleEntity("OPERATOR"));
        userRoleAssignmentRepository.saveAndFlush(roleAssignmentEntity(user.getId(), role.getId(), FIXED_INSTANT.minusSeconds(7200), null));

        assertThatThrownBy(() -> userRoleAssignmentRepository.saveAndFlush(
            roleAssignmentEntity(user.getId(), role.getId(), FIXED_INSTANT.minusSeconds(3600), null)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsOverlappingOrganizationAssignmentPeriod() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1", null));
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity unit = organizationalUnitRepository.saveAndFlush(unitEntity(null, unitType.getId(), "Team", "/team", null));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            user.getId(),
            unit.getId(),
            OrganizationAssignmentType.SECONDARY,
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));

        assertThatThrownBy(() -> userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            user.getId(),
            unit.getId(),
            OrganizationAssignmentType.SECONDARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlushRejectsSecondActivePrimaryAssignment() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-1", null));
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity firstUnit = organizationalUnitRepository.saveAndFlush(unitEntity(null, unitType.getId(), "Team A", "/team-a", null));
        OrganizationalUnitEntity secondUnit = organizationalUnitRepository.saveAndFlush(unitEntity(null, unitType.getId(), "Team B", "/team-b", null));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            user.getId(),
            firstUnit.getId(),
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));

        assertThatThrownBy(() -> userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            user.getId(),
            secondUnit.getId(),
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private AppUserEntity userEntity(String employeeNumber, String externalId) {
        AppUserEntity entity = new AppUserEntity();
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(externalId);
        entity.setLastName("Last");
        entity.setFirstName("First");
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AppRoleEntity roleEntity(String code) {
        AppRoleEntity entity = new AppRoleEntity();
        entity.setCode(code);
        entity.setName(code + " Name");
        entity.setDescription(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private UserRoleAssignmentEntity roleAssignmentEntity(Long userId, Long roleId, Instant validFrom, Instant validTo) {
        UserRoleAssignmentEntity entity = new UserRoleAssignmentEntity();
        entity.setUserId(userId);
        entity.setRoleId(roleId);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private UserOrganizationAssignmentEntity orgAssignmentEntity(
        Long userId,
        Long organizationalUnitId,
        OrganizationAssignmentType assignmentType,
        Instant validFrom,
        Instant validTo
    ) {
        UserOrganizationAssignmentEntity entity = new UserOrganizationAssignmentEntity();
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(organizationalUnitId);
        entity.setAssignmentType(assignmentType);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
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
        SpringDataAppUserJpaRepository.class,
        SpringDataAppRoleJpaRepository.class,
        SpringDataUserRoleAssignmentJpaRepository.class,
        SpringDataUserOrganizationAssignmentJpaRepository.class,
        SpringDataOrganizationalUnitJpaRepository.class,
        SpringDataOrganizationalUnitTypeJpaRepository.class
    })
    static class UserAdministrationPersistenceTestApplication {
    }
}
