package com.vladislav.training.platform.userorg.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.infrastructure.persistence.ManagementRelationEntity;
import com.vladislav.training.platform.access.infrastructure.persistence.ManagementRelationTypeEntity;
import com.vladislav.training.platform.access.infrastructure.persistence.SpringDataManagementRelationJpaRepository;
import com.vladislav.training.platform.access.infrastructure.persistence.SpringDataManagementRelationTypeJpaRepository;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = ManagerialVisibleUsersRestrictionBuilderIntegrationTest.ManagerialVisibleUsersRestrictionTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code ManagerialVisibleUsersRestrictionBuilder} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class ManagerialVisibleUsersRestrictionBuilderIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T21:00:00Z");

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
    private SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository;
    @Autowired
    private SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Autowired
    private SpringDataOrganizationalUnitTypeJpaRepository organizationalUnitTypeRepository;
    @Autowired
    private SpringDataManagementRelationJpaRepository managementRelationRepository;
    @Autowired
    private SpringDataManagementRelationTypeJpaRepository managementRelationTypeRepository;

    @AfterEach
    void cleanDatabase() {
        managementRelationRepository.deleteAllInBatch();
        managementRelationTypeRepository.deleteAllInBatch();
        userOrganizationAssignmentRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        organizationalUnitRepository.deleteAllInBatch();
        organizationalUnitTypeRepository.deleteAllInBatch();
    }

    @Test
    void denyAllScopeReturnsNoUsers() {
        AppUserEntity user = appUserRepository.saveAndFlush(userEntity("EMP-DENY"));
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity unit = organizationalUnitRepository.saveAndFlush(unitEntity(null, unitType.getId(), "Team", "/root/team"));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            user.getId(),
            unit.getId(),
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(ManagerialReadScope.denyAll(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        ));

        assertThat(appUserRepository.findAll(restriction.toSpecification("id"), Sort.by("id"))).isEmpty();
    }

    @Test
    void unitOnlyScopeSeesOnlyUsersWithCurrentActiveAssignmentsInThatUnit() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity targetUnit = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Target", "/root/target")
        );
        OrganizationalUnitEntity otherUnit = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Other", "/root/other")
        );
        AppUserEntity visibleUser = appUserRepository.saveAndFlush(userEntity("EMP-U1"));
        AppUserEntity hiddenOtherUnitUser = appUserRepository.saveAndFlush(userEntity("EMP-U2"));
        AppUserEntity expiredUser = appUserRepository.saveAndFlush(userEntity("EMP-U3"));

        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            visibleUser.getId(),
            targetUnit.getId(),
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            hiddenOtherUnitUser.getId(),
            otherUnit.getId(),
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            expiredUser.getId(),
            targetUnit.getId(),
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(60)
        ));

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.scoped(java.util.Set.of(targetUnit.getId()), java.util.Set.of())
        ));

        assertThat(appUserRepository.findAll(restriction.toSpecification("id"), Sort.by("id")))
            .extracting(AppUserEntity::getEmployeeNumber)
            .containsExactly("EMP-U1");
    }

    @Test
    void subtreeScopeSeesOnlyUsersWithinOrganizationalPathPrefix() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity root = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Root", "/root")
        );
        OrganizationalUnitEntity subtreeUnit = organizationalUnitRepository.saveAndFlush(
            unitEntity(root.getId(), unitType.getId(), "Subtree", "/root/branch/sub")
        );
        OrganizationalUnitEntity samePrefixStartButOutside = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Outside", "/root-other")
        );
        AppUserEntity subtreeUser = appUserRepository.saveAndFlush(userEntity("EMP-SUB"));
        AppUserEntity outsideUser = appUserRepository.saveAndFlush(userEntity("EMP-OUT"));

        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            subtreeUser.getId(),
            subtreeUnit.getId(),
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            outsideUser.getId(),
            samePrefixStartButOutside.getId(),
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.scoped(java.util.Set.of(), java.util.Set.of("/root"))
        ));

        assertThat(appUserRepository.findAll(restriction.toSpecification("id"), Sort.by("id")))
            .extracting(AppUserEntity::getEmployeeNumber)
            .containsExactly("EMP-SUB");
    }

    @Test
    void fullAccessScopeReturnsAllUsersWithinAppUserSpecificationSurface() {
        AppUserEntity firstUser = appUserRepository.saveAndFlush(userEntity("EMP-A"));
        AppUserEntity secondUser = appUserRepository.saveAndFlush(userEntity("EMP-B"));

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.fullAccess()
        ));

        assertThat(appUserRepository.findAll(restriction.toSpecification("id"), Sort.by("id")))
            .extracting(AppUserEntity::getEmployeeNumber)
            .containsExactly("EMP-A", "EMP-B");
    }

    @Test
    void managementRelationWithoutCurrentAccessAreaOrOrgAssignmentDoesNotGiveVisibility() {
        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity unit = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Managed", "/root/managed")
        );
        AppUserEntity managedOnlyByRelation = appUserRepository.saveAndFlush(userEntity("EMP-MGR-ONLY"));
        ManagementRelationTypeEntity relationType = managementRelationTypeRepository.saveAndFlush(
            managementRelationTypeEntity("SUPERVISOR")
        );
        managementRelationRepository.saveAndFlush(managementRelationEntity(
            managedOnlyByRelation.getId(),
            unit.getId(),
            relationType.getId(),
            FIXED_INSTANT.minusSeconds(3600),
            null
        ));

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.scoped(java.util.Set.of(unit.getId()), java.util.Set.of())
        ));

        assertThat(appUserRepository.findAll(restriction.toSpecification("id"), Sort.by("id"))).isEmpty();
    }

    private AppUserEntity userEntity(String employeeNumber) {
        AppUserEntity entity = new AppUserEntity();
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
        String path
    ) {
        OrganizationalUnitEntity entity = new OrganizationalUnitEntity();
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

    private ManagementRelationTypeEntity managementRelationTypeEntity(String code) {
        ManagementRelationTypeEntity entity = instantiate(ManagementRelationTypeEntity.class);
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
        ManagementRelationEntity entity = instantiate(ManagementRelationEntity.class);
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
        OrganizationalUnitEntity.class,
        OrganizationalUnitTypeEntity.class,
        UserOrganizationAssignmentEntity.class,
        ManagementRelationEntity.class,
        ManagementRelationTypeEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAppUserJpaRepository.class,
        SpringDataOrganizationalUnitJpaRepository.class,
        SpringDataOrganizationalUnitTypeJpaRepository.class,
        SpringDataUserOrganizationAssignmentJpaRepository.class,
        SpringDataManagementRelationJpaRepository.class,
        SpringDataManagementRelationTypeJpaRepository.class
    })
    static class ManagerialVisibleUsersRestrictionTestApplication {
    }
}
