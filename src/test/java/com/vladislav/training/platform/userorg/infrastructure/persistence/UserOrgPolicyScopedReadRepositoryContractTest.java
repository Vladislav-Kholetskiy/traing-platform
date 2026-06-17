package com.vladislav.training.platform.userorg.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.service.OrganizationPolicyReadService;
import com.vladislav.training.platform.userorg.service.UserAdministrationPolicyReadService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = UserOrgPolicyScopedReadRepositoryContractTest.UserOrgPolicyScopedReadRepositoryContractTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет договорённости вокруг {@code UserOrgPolicyScopedReadRepository}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@Testcontainers(disabledWithoutDocker = true)
class UserOrgPolicyScopedReadRepositoryContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T12:00:00Z");

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
    private JpaAccessScopeProjectionService accessScopeProjectionService;
    @Autowired
    private PolicyScopedOrganizationalUnitReadRepository organizationalUnitReadRepository;
    @Autowired
    private PolicyScopedAppUserReadRepository appUserReadRepository;
    @Autowired
    private PolicyScopedUserOrganizationAssignmentReadRepository userOrganizationAssignmentReadRepository;
    @Autowired
    private PolicyScopedUserRoleAssignmentReadRepository userRoleAssignmentReadRepository;
    @Autowired
    private OrganizationPolicyReadService organizationPolicyReadService;
    @Autowired
    private UserAdministrationPolicyReadService userAdministrationPolicyReadService;

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
    void denyAllScopeFailsClosedAcrossProjectionAndPolicyScopedRepositories() {
        Fixture fixture = seedScopeFixture();
        AccessReadScope scope = AccessReadScope.denyAll();

        assertThat(accessScopeProjectionService.resolveOrganizationalUnitIds(scope)).isEmpty();
        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT)).isEmpty();
        assertThat(organizationalUnitReadRepository.findUnitsWithinScope(scope, OrganizationalUnitStatus.ACTIVE)).isEmpty();
        assertThat(organizationalUnitReadRepository.findOrganizationalUnitByIdWithinScope(scope, fixture.teamUnitId())).isEmpty();
        assertThat(appUserReadRepository.findUsersWithinScope(scope, UserStatus.ACTIVE, FIXED_INSTANT)).isEmpty();
        assertThat(appUserReadRepository.findUserByIdWithinScope(scope, FIXED_INSTANT, fixture.teamUserId())).isEmpty();
        assertThat(userOrganizationAssignmentReadRepository.findActiveOrganizationAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.teamUserId()
        )).isEmpty();
        assertThat(userRoleAssignmentReadRepository.findActiveRoleAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.teamUserId()
        )).isEmpty();
    }

    @Test
    void fullAccessScopeKeepsProjectionMarkerEmptyButDoesNotRestrictEligibleRows() {
        Fixture fixture = seedScopeFixture();
        AccessReadScope scope = AccessReadScope.fullAccess();

        assertThat(accessScopeProjectionService.resolveOrganizationalUnitIds(scope)).isEmpty();
        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT)).isEmpty();

        assertThat(organizationPolicyReadService.findUnitsWithinScope(scope, OrganizationalUnitStatus.ACTIVE))
            .extracting(OrganizationalUnit::path)
            .containsExactly(
                "/root",
                "/root/team",
                "/root/team/sub",
                "/root/team-x"
            );

        assertThat(userAdministrationPolicyReadService.findUsersWithinScope(scope, UserStatus.ACTIVE, FIXED_INSTANT))
            .extracting(AppUser::employeeNumber)
            .containsExactly(
                "EMP-TEAM",
                "EMP-SUB",
                "EMP-SIBLING",
                "EMP-EXPIRED"
            );

        assertThat(userAdministrationPolicyReadService.findActiveOrganizationAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.teamUserId()
        )).hasSize(1);

        assertThat(userAdministrationPolicyReadService.findActiveRoleAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.siblingUserId()
        )).hasSize(1);
    }

    @Test
    void subtreeScopeIncludesDescendantsButBlocksSamePrefixSiblingAndForeignVisibility() {
        Fixture fixture = seedScopeFixture();
        AccessReadScope scope = AccessReadScope.scoped(Set.of(), Set.of("/root/team"));

        assertThat(accessScopeProjectionService.resolveOrganizationalUnitIds(scope))
            .containsExactlyInAnyOrder(fixture.teamUnitId(), fixture.subUnitId());
        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT))
            .containsExactlyInAnyOrder(fixture.teamUserId(), fixture.subUserId());

        assertThat(organizationPolicyReadService.findUnitsWithinScope(scope, OrganizationalUnitStatus.ACTIVE))
            .extracting(OrganizationalUnit::path)
            .containsExactly("/root/team", "/root/team/sub");

        assertThat(organizationPolicyReadService.findChildUnitsWithinScope(scope, fixture.rootUnitId()))
            .extracting(OrganizationalUnit::path)
            .containsExactly("/root/team");

        assertThat(organizationPolicyReadService.findOrganizationalUnitsByIdsWithinScope(
            scope,
            List.of(fixture.teamUnitId(), fixture.subUnitId(), fixture.samePrefixSiblingUnitId())
        )).extracting(OrganizationalUnit::path)
            .containsExactly("/root/team", "/root/team/sub");

        assertThat(userAdministrationPolicyReadService.findUsersWithinScope(scope, UserStatus.ACTIVE, FIXED_INSTANT))
            .extracting(AppUser::employeeNumber)
            .containsExactly("EMP-TEAM", "EMP-SUB");

        assertThat(userAdministrationPolicyReadService.findUserByIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.siblingUserId()
        )).isEmpty();

        assertThat(userAdministrationPolicyReadService.findActiveOrganizationAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.teamUserId()
        )).extracting(UserOrganizationAssignment::organizationalUnitId)
            .containsExactly(fixture.teamUnitId());

        assertThat(userAdministrationPolicyReadService.findActiveRoleAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.subUserId()
        )).extracting(UserRoleAssignment::userId)
            .containsExactly(fixture.subUserId());

        assertThat(userAdministrationPolicyReadService.findActiveRoleAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.siblingUserId()
        )).isEmpty();
    }

    @Test
    void subtreeScopeWithoutStatusFilterReturnsPolicyVisibleUsersInsteadOfFailingOnNullOptionalFilter() {
        seedScopeFixture();
        AccessReadScope scope = AccessReadScope.scoped(Set.of(), Set.of("/root/team"));

        assertThat(appUserReadRepository.findUsersWithinScope(scope, null, FIXED_INSTANT))
            .extracting(AppUser::employeeNumber)
            .containsExactly("EMP-TEAM", "EMP-SUB");
    }

    @Test
    void exactUnitScopeDoesNotExpandToDescendantsAndOnlySeesDirectlyAssignedUsers() {
        Fixture fixture = seedScopeFixture();
        AccessReadScope scope = AccessReadScope.scoped(Set.of(fixture.teamUnitId()), Set.of());

        assertThat(accessScopeProjectionService.resolveOrganizationalUnitIds(scope))
            .containsExactly(fixture.teamUnitId());
        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT))
            .containsExactly(fixture.teamUserId());

        assertThat(organizationalUnitReadRepository.findUnitsWithinScope(scope, OrganizationalUnitStatus.ACTIVE))
            .extracting(OrganizationalUnit::path)
            .containsExactly("/root/team");

        assertThat(appUserReadRepository.findUsersWithinScope(scope, UserStatus.ACTIVE, FIXED_INSTANT))
            .extracting(AppUser::employeeNumber)
            .containsExactly("EMP-TEAM");

        assertThat(userRoleAssignmentReadRepository.findActiveRoleAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.subUserId()
        )).isEmpty();
    }

    @Test
    void expiredOrganizationAssignmentDoesNotCreateCurrentVisibilityOrRoleExposure() {
        Fixture fixture = seedScopeFixture();
        AccessReadScope scope = AccessReadScope.scoped(Set.of(), Set.of("/root/team"));

        assertThat(accessScopeProjectionService.resolveVisibleUserIds(scope, FIXED_INSTANT))
            .doesNotContain(fixture.expiredUserId());

        assertThat(appUserReadRepository.findUserByIdWithinScope(scope, FIXED_INSTANT, fixture.expiredUserId())).isEmpty();

        assertThat(userOrganizationAssignmentReadRepository.findOrganizationAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.expiredUserId()
        )).isEmpty();

        assertThat(userRoleAssignmentReadRepository.findActiveRoleAssignmentsByUserIdWithinScope(
            scope,
            FIXED_INSTANT,
            fixture.expiredUserId()
        )).isEmpty();
    }

    private Fixture seedScopeFixture() {
        OrganizationalUnitTypeEntity teamType = organizationalUnitTypeRepository.saveAndFlush(unitTypeEntity("TEAM"));

        OrganizationalUnitEntity root = organizationalUnitRepository.saveAndFlush(
            unitEntity(null, teamType.getId(), "Root", "/root")
        );
        OrganizationalUnitEntity team = organizationalUnitRepository.saveAndFlush(
            unitEntity(root.getId(), teamType.getId(), "Team", "/root/team")
        );
        OrganizationalUnitEntity sub = organizationalUnitRepository.saveAndFlush(
            unitEntity(team.getId(), teamType.getId(), "Sub", "/root/team/sub")
        );
        OrganizationalUnitEntity samePrefixSibling = organizationalUnitRepository.saveAndFlush(
            unitEntity(root.getId(), teamType.getId(), "Team X", "/root/team-x")
        );

        AppUserEntity teamUser = appUserRepository.saveAndFlush(userEntity("EMP-TEAM", UserStatus.ACTIVE));
        AppUserEntity subUser = appUserRepository.saveAndFlush(userEntity("EMP-SUB", UserStatus.ACTIVE));
        AppUserEntity siblingUser = appUserRepository.saveAndFlush(userEntity("EMP-SIBLING", UserStatus.ACTIVE));
        AppUserEntity expiredUser = appUserRepository.saveAndFlush(userEntity("EMP-EXPIRED", UserStatus.ACTIVE));
        appUserRepository.saveAndFlush(userEntity("EMP-INACTIVE", UserStatus.INACTIVE));

        AppRoleEntity operatorRole = appRoleRepository.saveAndFlush(roleEntity("OPERATOR"));

        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            teamUser.getId(),
            team.getId(),
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            subUser.getId(),
            sub.getId(),
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            siblingUser.getId(),
            samePrefixSibling.getId(),
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));
        userOrganizationAssignmentRepository.saveAndFlush(orgAssignmentEntity(
            expiredUser.getId(),
            team.getId(),
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(60)
        ));

        userRoleAssignmentRepository.saveAndFlush(roleAssignmentEntity(
            teamUser.getId(),
            operatorRole.getId(),
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));
        userRoleAssignmentRepository.saveAndFlush(roleAssignmentEntity(
            subUser.getId(),
            operatorRole.getId(),
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));
        userRoleAssignmentRepository.saveAndFlush(roleAssignmentEntity(
            siblingUser.getId(),
            operatorRole.getId(),
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));
        userRoleAssignmentRepository.saveAndFlush(roleAssignmentEntity(
            expiredUser.getId(),
            operatorRole.getId(),
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));

        return new Fixture(
            root.getId(),
            team.getId(),
            sub.getId(),
            samePrefixSibling.getId(),
            teamUser.getId(),
            subUser.getId(),
            siblingUser.getId(),
            expiredUser.getId()
        );
    }

    private AppUserEntity userEntity(String employeeNumber, UserStatus status) {
        AppUserEntity entity = new AppUserEntity();
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(employeeNumber + "-EXT");
        entity.setLastName("Last");
        entity.setFirstName("First");
        entity.setMiddleName(null);
        entity.setStatus(status);
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
        Instant validFrom,
        Instant validTo
    ) {
        UserOrganizationAssignmentEntity entity = new UserOrganizationAssignmentEntity();
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

    private OrganizationalUnitEntity unitEntity(Long parentId, Long unitTypeId, String name, String path) {
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

    private record Fixture(
        Long rootUnitId,
        Long teamUnitId,
        Long subUnitId,
        Long samePrefixSiblingUnitId,
        Long teamUserId,
        Long subUserId,
        Long siblingUserId,
        Long expiredUserId
    ) {
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
    @Import({
        UserOrgMapper.class,
        JpaAccessScopeProjectionService.class,
        PolicyScopedOrganizationalUnitReadRepository.class,
        PolicyScopedAppUserReadRepository.class,
        PolicyScopedUserOrganizationAssignmentReadRepository.class,
        PolicyScopedUserRoleAssignmentReadRepository.class,
        JpaOrganizationPolicyReadService.class,
        JpaUserAdministrationPolicyReadService.class
    })
    static class UserOrgPolicyScopedReadRepositoryContractTestApplication {
    }
}
