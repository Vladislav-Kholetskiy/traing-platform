package com.vladislav.training.platform.access.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagementRelationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentAdminFilter;
import com.vladislav.training.platform.access.service.UserAccessAreaAdminFilter;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppRoleEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.JpaAccessScopeProjectionService;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitTypeEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppRoleJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataOrganizationalUnitJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataOrganizationalUnitTypeJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataUserOrganizationAssignmentJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.UserOrgMapper;
import com.vladislav.training.platform.userorg.infrastructure.persistence.UserOrganizationAssignmentEntity;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = AccessPolicyScopedReadRepositoryContractTest.AccessPolicyScopedReadRepositoryContractTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет договорённости вокруг {@code AccessPolicyScopedReadRepository}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@Testcontainers(disabledWithoutDocker = true)
class AccessPolicyScopedReadRepositoryContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T15:00:00Z");

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
    private SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Autowired
    private SpringDataOrganizationalUnitTypeJpaRepository organizationalUnitTypeRepository;
    @Autowired
    private SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository;
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

    @Autowired
    private JpaAccessScopeProjectionService accessScopeProjectionService;
    @Autowired
    private PolicyScopedUserAccessAreaReadRepository userAccessAreaReadRepository;
    @Autowired
    private PolicyScopedManagementRelationReadRepository managementRelationReadRepository;
    @Autowired
    private PolicyScopedTemporaryAccessAreaReadRepository temporaryAccessAreaReadRepository;
    @Autowired
    private PolicyScopedTemporaryManagementDelegationReadRepository temporaryManagementDelegationReadRepository;
    @Autowired
    private PolicyScopedTemporaryRoleAssignmentReadRepository temporaryRoleAssignmentReadRepository;
    @Autowired
    private JpaAccessAdministrationPolicyReadService accessAdministrationPolicyReadService;

    @AfterEach
    void cleanDatabase() {
        temporaryManagementDelegationRepository.deleteAllInBatch();
        temporaryAccessAreaRepository.deleteAllInBatch();
        temporaryRoleAssignmentRepository.deleteAllInBatch();
        managementRelationRepository.deleteAllInBatch();
        userAccessAreaRepository.deleteAllInBatch();
        managementRelationTypeRepository.deleteAllInBatch();
        appRoleRepository.deleteAllInBatch();
        userOrganizationAssignmentRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        organizationalUnitRepository.deleteAllInBatch();
        organizationalUnitTypeRepository.deleteAllInBatch();
    }

    @Test
    void denyAllScopeFailsClosedAcrossProjectionAndAccessRepositories() {
        seedAccessFixture();
        AccessReadScope scope = AccessReadScope.denyAll();

        assertThat(accessScopeProjectionService.resolveOrganizationalUnitIds(scope)).isEmpty();
        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT)).isEmpty();
        assertThat(userAccessAreaReadRepository.findUserAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new UserAccessAreaAdminFilter(null, null, null, FIXED_INSTANT)
        )).isEmpty();
        assertThat(managementRelationReadRepository.findManagementRelationsWithinScope(
            scope,
            new ManagementRelationAdminFilter(null, null, null, FIXED_INSTANT)
        )).isEmpty();
        assertThat(temporaryAccessAreaReadRepository.findTemporaryAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryAccessAreaAdminFilter(null, null, null, FIXED_INSTANT)
        )).isEmpty();
        assertThat(temporaryManagementDelegationReadRepository.findTemporaryManagementDelegationsWithinScope(
            scope,
            new TemporaryManagementDelegationAdminFilter(null, null, null, FIXED_INSTANT)
        )).isEmpty();
        assertThat(temporaryRoleAssignmentReadRepository.findTemporaryRoleAssignmentsWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryRoleAssignmentAdminFilter(null, null, FIXED_INSTANT)
        )).isEmpty();
    }

    @Test
    void fullAccessScopeDoesNotRestrictCurrentEligibleAccessFacts() {
        Fixture fixture = seedAccessFixture();
        AccessReadScope scope = AccessReadScope.fullAccess();

        assertThat(accessScopeProjectionService.resolveOrganizationalUnitIds(scope)).isEmpty();
        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT)).isEmpty();

        assertThat(accessAdministrationPolicyReadService.findUserAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new UserAccessAreaAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(UserAccessArea::id)
            .containsExactlyInAnyOrder(
                fixture.activeSubAreaId(),
                fixture.activeTeamAreaId(),
                fixture.activeSiblingAreaId(),
                fixture.activeVisibleGlobalAreaId(),
                fixture.activeHiddenGlobalAreaId()
            );

        assertThat(accessAdministrationPolicyReadService.findManagementRelationsWithinScope(
            scope,
            new ManagementRelationAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(ManagementRelation::id)
            .containsExactlyInAnyOrder(
                fixture.activeSubRelationId(),
                fixture.activeTeamRelationId(),
                fixture.activeSiblingRelationId()
            );

        assertThat(accessAdministrationPolicyReadService.findTemporaryAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryAccessAreaAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(TemporaryAccessArea::id)
            .containsExactlyInAnyOrder(
                fixture.activeSubTemporaryAreaId(),
                fixture.activeTeamTemporaryAreaId(),
                fixture.activeSiblingTemporaryAreaId(),
                fixture.activeVisibleGlobalTemporaryAreaId(),
                fixture.activeHiddenGlobalTemporaryAreaId()
            );

        assertThat(accessAdministrationPolicyReadService.findTemporaryManagementDelegationsWithinScope(
            scope,
            new TemporaryManagementDelegationAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(TemporaryManagementDelegation::id)
            .containsExactlyInAnyOrder(
                fixture.activeSubDelegationId(),
                fixture.activeTeamDelegationId(),
                fixture.activeSiblingDelegationId()
            );

        assertThat(accessAdministrationPolicyReadService.findTemporaryRoleAssignmentsWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryRoleAssignmentAdminFilter(null, null, FIXED_INSTANT)
        )).extracting(TemporaryRoleAssignment::id)
            .containsExactlyInAnyOrder(
                fixture.activeVisibleRoleAssignmentId(),
                fixture.activeHiddenRoleAssignmentId()
            );
    }

    @Test
    void subtreeScopeIncludesAllowedBranchBlocksSamePrefixSiblingAndRespectsVisibleUserGlobalRows() {
        Fixture fixture = seedAccessFixture();
        AccessReadScope scope = AccessReadScope.scoped(Set.of(), Set.of("/root/team"));

        assertThat(accessScopeProjectionService.resolveOrganizationalUnitIds(scope))
            .containsExactlyInAnyOrder(fixture.teamUnitId(), fixture.subUnitId());
        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT))
            .containsExactlyInAnyOrder(fixture.teamUserId(), fixture.subUserId());

        assertThat(accessAdministrationPolicyReadService.findUserAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new UserAccessAreaAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(UserAccessArea::id)
            .containsExactlyInAnyOrder(
                fixture.activeSubAreaId(),
                fixture.activeTeamAreaId(),
                fixture.activeVisibleGlobalAreaId()
            );

        assertThat(accessAdministrationPolicyReadService.findManagementRelationsWithinScope(
            scope,
            new ManagementRelationAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(ManagementRelation::id)
            .containsExactlyInAnyOrder(
                fixture.activeSubRelationId(),
                fixture.activeTeamRelationId()
            );

        assertThat(accessAdministrationPolicyReadService.findTemporaryAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryAccessAreaAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(TemporaryAccessArea::id)
            .containsExactlyInAnyOrder(
                fixture.activeSubTemporaryAreaId(),
                fixture.activeTeamTemporaryAreaId(),
                fixture.activeVisibleGlobalTemporaryAreaId()
            );

        assertThat(accessAdministrationPolicyReadService.findTemporaryManagementDelegationsWithinScope(
            scope,
            new TemporaryManagementDelegationAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(TemporaryManagementDelegation::id)
            .containsExactlyInAnyOrder(
                fixture.activeSubDelegationId(),
                fixture.activeTeamDelegationId()
            );

        assertThat(accessAdministrationPolicyReadService.findTemporaryRoleAssignmentsWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryRoleAssignmentAdminFilter(null, null, FIXED_INSTANT)
        )).extracting(TemporaryRoleAssignment::id)
            .containsExactlyInAnyOrder(fixture.activeVisibleRoleAssignmentId());
    }

    @Test
    void exactUnitScopeDoesNotExpandToDescendantsAndFiltersAccessFactsToDirectUnitOnly() {
        Fixture fixture = seedAccessFixture();
        AccessReadScope scope = AccessReadScope.scoped(Set.of(fixture.teamUnitId()), Set.of());

        assertThat(accessScopeProjectionService.resolveOrganizationalUnitIds(scope))
            .containsExactlyInAnyOrder(fixture.teamUnitId());
        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT))
            .containsExactlyInAnyOrder(fixture.teamUserId());

        assertThat(accessAdministrationPolicyReadService.findUserAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new UserAccessAreaAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(UserAccessArea::id)
            .containsExactlyInAnyOrder(
                fixture.activeTeamAreaId(),
                fixture.activeVisibleGlobalAreaId()
            );

        assertThat(accessAdministrationPolicyReadService.findManagementRelationsWithinScope(
            scope,
            new ManagementRelationAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(ManagementRelation::id)
            .containsExactlyInAnyOrder(fixture.activeTeamRelationId());

        assertThat(accessAdministrationPolicyReadService.findTemporaryManagementDelegationsWithinScope(
            scope,
            new TemporaryManagementDelegationAdminFilter(null, null, null, FIXED_INSTANT)
        )).extracting(TemporaryManagementDelegation::id)
            .containsExactlyInAnyOrder(fixture.activeTeamDelegationId());

        assertThat(accessAdministrationPolicyReadService.findTemporaryRoleAssignmentsWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryRoleAssignmentAdminFilter(null, null, FIXED_INSTANT)
        )).extracting(TemporaryRoleAssignment::id)
            .containsExactlyInAnyOrder(fixture.activeVisibleRoleAssignmentId());
    }

    @Test
    void activeAtFiltersExcludeExpiredAndFutureTemporalRows() {
        Fixture fixture = seedAccessFixture();
        AccessReadScope scope = AccessReadScope.fullAccess();

        assertThat(accessAdministrationPolicyReadService.findUserAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new UserAccessAreaAdminFilter(fixture.teamUserId(), null, null, FIXED_INSTANT)
        )).extracting(UserAccessArea::id)
            .containsExactlyInAnyOrder(fixture.activeVisibleGlobalAreaId(), fixture.activeTeamAreaId());

        assertThat(accessAdministrationPolicyReadService.findTemporaryAccessAreasWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryAccessAreaAdminFilter(fixture.teamUserId(), null, null, FIXED_INSTANT)
        )).extracting(TemporaryAccessArea::id)
            .containsExactlyInAnyOrder(
                fixture.activeVisibleGlobalTemporaryAreaId(),
                fixture.activeTeamTemporaryAreaId()
            );

        assertThat(accessAdministrationPolicyReadService.findTemporaryManagementDelegationsWithinScope(
            scope,
            new TemporaryManagementDelegationAdminFilter(fixture.teamUserId(), null, null, FIXED_INSTANT)
        )).extracting(TemporaryManagementDelegation::id)
            .containsExactlyInAnyOrder(fixture.activeTeamDelegationId());

        assertThat(accessAdministrationPolicyReadService.findTemporaryRoleAssignmentsWithinScope(
            scope,
            FIXED_INSTANT,
            new TemporaryRoleAssignmentAdminFilter(fixture.teamUserId(), fixture.operatorRoleId(), FIXED_INSTANT)
        )).extracting(TemporaryRoleAssignment::id)
            .containsExactlyInAnyOrder(fixture.activeVisibleRoleAssignmentId());
    }

    private Fixture seedAccessFixture() {
        Instant currentFrom = FIXED_INSTANT.minusSeconds(1800);
        Instant currentTo = FIXED_INSTANT.plusSeconds(1800);
        Instant expiredFrom = FIXED_INSTANT.minusSeconds(7200);
        Instant expiredTo = FIXED_INSTANT.minusSeconds(3600);
        Instant futureFrom = FIXED_INSTANT.plusSeconds(3600);

        OrganizationalUnitTypeEntity unitType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));
        OrganizationalUnitEntity root = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, unitType.getId(), "Root", "/root")
        );
        OrganizationalUnitEntity team = organizationalUnitRepository.saveAndFlush(
            unitEntity(root.getId(), unitType.getId(), "Team", "/root/team")
        );
        OrganizationalUnitEntity sub = organizationalUnitRepository.saveAndFlush(
            unitEntity(team.getId(), unitType.getId(), "Sub", "/root/team/sub")
        );
        OrganizationalUnitEntity sibling = organizationalUnitRepository.saveAndFlush(
            unitEntity(root.getId(), unitType.getId(), "Team X", "/root/team-x")
        );

        AppUserEntity teamUser = appUserRepository.saveAndFlush(userEntity("EMP-TEAM"));
        AppUserEntity subUser = appUserRepository.saveAndFlush(userEntity("EMP-SUB"));
        AppUserEntity siblingUser = appUserRepository.saveAndFlush(userEntity("EMP-SIBLING"));

        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(teamUser.getId(), team.getId(), FIXED_INSTANT.minusSeconds(7200), null));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(subUser.getId(), sub.getId(), FIXED_INSTANT.minusSeconds(7200), null));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(siblingUser.getId(), sibling.getId(), FIXED_INSTANT.minusSeconds(7200), null));

        AppRoleEntity operatorRole = appRoleRepository.saveAndFlush(roleEntity("OPERATOR"));
        ManagementRelationTypeEntity relationType = managementRelationTypeRepository.saveAndFlush(managementRelationTypeEntity("SUPERVISOR"));

        UserAccessAreaEntity activeVisibleGlobalArea = userAccessAreaRepository.saveAndFlush(
            accessAreaEntity(teamUser.getId(), null, AccessScopeType.GLOBAL, currentFrom, currentTo)
        );
        UserAccessAreaEntity activeHiddenGlobalArea = userAccessAreaRepository.saveAndFlush(
            accessAreaEntity(siblingUser.getId(), null, AccessScopeType.GLOBAL, currentFrom, currentTo)
        );
        UserAccessAreaEntity activeTeamArea = userAccessAreaRepository.saveAndFlush(
            accessAreaEntity(teamUser.getId(), team.getId(), AccessScopeType.UNIT_ONLY, currentFrom, currentTo)
        );
        UserAccessAreaEntity activeSubArea = userAccessAreaRepository.saveAndFlush(
            accessAreaEntity(subUser.getId(), sub.getId(), AccessScopeType.UNIT_SUBTREE, currentFrom, currentTo)
        );
        UserAccessAreaEntity activeSiblingArea = userAccessAreaRepository.saveAndFlush(
            accessAreaEntity(siblingUser.getId(), sibling.getId(), AccessScopeType.UNIT_ONLY, currentFrom, currentTo)
        );
        userAccessAreaRepository.saveAndFlush(
            accessAreaEntity(teamUser.getId(), team.getId(), AccessScopeType.UNIT_ONLY, expiredFrom, expiredTo)
        );
        userAccessAreaRepository.saveAndFlush(
            accessAreaEntity(teamUser.getId(), team.getId(), AccessScopeType.UNIT_ONLY, futureFrom, null)
        );

        ManagementRelationEntity activeTeamRelation = managementRelationRepository.saveAndFlush(
            managementRelationEntity(teamUser.getId(), team.getId(), relationType.getId(), currentFrom, currentTo)
        );
        ManagementRelationEntity activeSubRelation = managementRelationRepository.saveAndFlush(
            managementRelationEntity(subUser.getId(), sub.getId(), relationType.getId(), currentFrom, currentTo)
        );
        ManagementRelationEntity activeSiblingRelation = managementRelationRepository.saveAndFlush(
            managementRelationEntity(siblingUser.getId(), sibling.getId(), relationType.getId(), currentFrom, currentTo)
        );
        managementRelationRepository.saveAndFlush(
            managementRelationEntity(teamUser.getId(), team.getId(), relationType.getId(), expiredFrom, expiredTo)
        );

        TemporaryAccessAreaEntity activeVisibleGlobalTemporaryArea = temporaryAccessAreaRepository.saveAndFlush(
            temporaryAccessAreaEntity(teamUser.getId(), null, AccessScopeType.GLOBAL, currentFrom, currentTo)
        );
        TemporaryAccessAreaEntity activeHiddenGlobalTemporaryArea = temporaryAccessAreaRepository.saveAndFlush(
            temporaryAccessAreaEntity(siblingUser.getId(), null, AccessScopeType.GLOBAL, currentFrom, currentTo)
        );
        TemporaryAccessAreaEntity activeTeamTemporaryArea = temporaryAccessAreaRepository.saveAndFlush(
            temporaryAccessAreaEntity(teamUser.getId(), team.getId(), AccessScopeType.UNIT_ONLY, currentFrom, currentTo)
        );
        TemporaryAccessAreaEntity activeSubTemporaryArea = temporaryAccessAreaRepository.saveAndFlush(
            temporaryAccessAreaEntity(subUser.getId(), sub.getId(), AccessScopeType.UNIT_SUBTREE, currentFrom, currentTo)
        );
        TemporaryAccessAreaEntity activeSiblingTemporaryArea = temporaryAccessAreaRepository.saveAndFlush(
            temporaryAccessAreaEntity(siblingUser.getId(), sibling.getId(), AccessScopeType.UNIT_ONLY, currentFrom, currentTo)
        );
        temporaryAccessAreaRepository.saveAndFlush(
            temporaryAccessAreaEntity(teamUser.getId(), team.getId(), AccessScopeType.UNIT_ONLY, expiredFrom, expiredTo)
        );
        temporaryAccessAreaRepository.saveAndFlush(
            temporaryAccessAreaEntity(teamUser.getId(), team.getId(), AccessScopeType.UNIT_ONLY, futureFrom, null)
        );

        TemporaryManagementDelegationEntity activeTeamDelegation = temporaryManagementDelegationRepository.saveAndFlush(
            temporaryManagementDelegationEntity(teamUser.getId(), team.getId(), relationType.getId(), currentFrom, currentTo)
        );
        TemporaryManagementDelegationEntity activeSubDelegation = temporaryManagementDelegationRepository.saveAndFlush(
            temporaryManagementDelegationEntity(subUser.getId(), sub.getId(), relationType.getId(), currentFrom, currentTo)
        );
        TemporaryManagementDelegationEntity activeSiblingDelegation = temporaryManagementDelegationRepository.saveAndFlush(
            temporaryManagementDelegationEntity(siblingUser.getId(), sibling.getId(), relationType.getId(), currentFrom, currentTo)
        );
        temporaryManagementDelegationRepository.saveAndFlush(
            temporaryManagementDelegationEntity(teamUser.getId(), team.getId(), relationType.getId(), expiredFrom, expiredTo)
        );
        temporaryManagementDelegationRepository.saveAndFlush(
            temporaryManagementDelegationEntity(teamUser.getId(), team.getId(), relationType.getId(), futureFrom, null)
        );

        TemporaryRoleAssignmentEntity activeVisibleRoleAssignment = temporaryRoleAssignmentRepository.saveAndFlush(
            temporaryRoleAssignmentEntity(teamUser.getId(), operatorRole.getId(), currentFrom, currentTo)
        );
        TemporaryRoleAssignmentEntity activeHiddenRoleAssignment = temporaryRoleAssignmentRepository.saveAndFlush(
            temporaryRoleAssignmentEntity(siblingUser.getId(), operatorRole.getId(), currentFrom, currentTo)
        );
        temporaryRoleAssignmentRepository.saveAndFlush(
            temporaryRoleAssignmentEntity(teamUser.getId(), operatorRole.getId(), expiredFrom, expiredTo)
        );
        temporaryRoleAssignmentRepository.saveAndFlush(
            temporaryRoleAssignmentEntity(teamUser.getId(), operatorRole.getId(), futureFrom, null)
        );

        return new Fixture(
            team.getId(),
            sub.getId(),
            sibling.getId(),
            teamUser.getId(),
            subUser.getId(),
            siblingUser.getId(),
            operatorRole.getId(),
            activeVisibleGlobalArea.getId(),
            activeHiddenGlobalArea.getId(),
            activeTeamArea.getId(),
            activeSubArea.getId(),
            activeSiblingArea.getId(),
            activeTeamRelation.getId(),
            activeSubRelation.getId(),
            activeSiblingRelation.getId(),
            activeVisibleGlobalTemporaryArea.getId(),
            activeHiddenGlobalTemporaryArea.getId(),
            activeTeamTemporaryArea.getId(),
            activeSubTemporaryArea.getId(),
            activeSiblingTemporaryArea.getId(),
            activeTeamDelegation.getId(),
            activeSubDelegation.getId(),
            activeSiblingDelegation.getId(),
            activeVisibleRoleAssignment.getId(),
            activeHiddenRoleAssignment.getId()
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

    private record Fixture(
        Long teamUnitId,
        Long subUnitId,
        Long siblingUnitId,
        Long teamUserId,
        Long subUserId,
        Long siblingUserId,
        Long operatorRoleId,
        Long activeVisibleGlobalAreaId,
        Long activeHiddenGlobalAreaId,
        Long activeTeamAreaId,
        Long activeSubAreaId,
        Long activeSiblingAreaId,
        Long activeTeamRelationId,
        Long activeSubRelationId,
        Long activeSiblingRelationId,
        Long activeVisibleGlobalTemporaryAreaId,
        Long activeHiddenGlobalTemporaryAreaId,
        Long activeTeamTemporaryAreaId,
        Long activeSubTemporaryAreaId,
        Long activeSiblingTemporaryAreaId,
        Long activeTeamDelegationId,
        Long activeSubDelegationId,
        Long activeSiblingDelegationId,
        Long activeVisibleRoleAssignmentId,
        Long activeHiddenRoleAssignmentId
    ) {
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
        UserOrganizationAssignmentEntity.class,
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
        SpringDataUserOrganizationAssignmentJpaRepository.class,
        SpringDataUserAccessAreaJpaRepository.class,
        SpringDataManagementRelationJpaRepository.class,
        SpringDataManagementRelationTypeJpaRepository.class,
        SpringDataTemporaryRoleAssignmentJpaRepository.class,
        SpringDataTemporaryAccessAreaJpaRepository.class,
        SpringDataTemporaryManagementDelegationJpaRepository.class
    })
    @Import({
        UserOrgMapper.class,
        AccessMapper.class,
        JpaAccessScopeProjectionService.class,
        PolicyScopedUserAccessAreaReadRepository.class,
        PolicyScopedManagementRelationReadRepository.class,
        PolicyScopedTemporaryAccessAreaReadRepository.class,
        PolicyScopedTemporaryManagementDelegationReadRepository.class,
        PolicyScopedTemporaryRoleAssignmentReadRepository.class,
        JpaAccessAdministrationPolicyReadService.class
    })
    static class AccessPolicyScopedReadRepositoryContractTestApplication {
    }
}
