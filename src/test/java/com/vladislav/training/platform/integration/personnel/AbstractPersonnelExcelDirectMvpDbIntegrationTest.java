package com.vladislav.training.platform.integration.personnel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.TrainingPlatformApplication;
import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.infrastructure.persistence.ManagementRelationEntity;
import com.vladislav.training.platform.access.infrastructure.persistence.ManagementRelationTypeEntity;
import com.vladislav.training.platform.access.infrastructure.persistence.SpringDataManagementRelationJpaRepository;
import com.vladislav.training.platform.access.infrastructure.persistence.SpringDataManagementRelationTypeJpaRepository;
import com.vladislav.training.platform.access.infrastructure.persistence.SpringDataTemporaryAccessAreaJpaRepository;
import com.vladislav.training.platform.access.infrastructure.persistence.SpringDataTemporaryManagementDelegationJpaRepository;
import com.vladislav.training.platform.access.infrastructure.persistence.SpringDataTemporaryRoleAssignmentJpaRepository;
import com.vladislav.training.platform.access.infrastructure.persistence.SpringDataUserAccessAreaJpaRepository;
import com.vladislav.training.platform.access.infrastructure.persistence.TemporaryAccessAreaEntity;
import com.vladislav.training.platform.access.infrastructure.persistence.TemporaryManagementDelegationEntity;
import com.vladislav.training.platform.access.infrastructure.persistence.TemporaryRoleAssignmentEntity;
import com.vladislav.training.platform.access.infrastructure.persistence.UserAccessAreaEntity;
import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataAnswerOptionJpaRepository;
import com.vladislav.training.platform.integration.infrastructure.persistence.SpringDataImportJobItemJpaRepository;
import com.vladislav.training.platform.integration.infrastructure.persistence.SpringDataImportJobJpaRepository;
import com.vladislav.training.platform.integration.personnel.controller.PersonnelExcelImportController;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.service.OwnerReadPersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelOwnerMutationExecutor;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookDryRunFacade;
import com.vladislav.training.platform.notification.infrastructure.persistence.SpringDataNotificationJpaRepository;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultAnswerOptionSnapshotJpaRepository;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultQuestionSnapshotJpaRepository;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
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
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataUserOrganizationAssignmentJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataUserRoleAssignmentJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.UserOrganizationAssignmentEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.UserRoleAssignmentEntity;
import com.vladislav.training.platform.userorg.service.UserAdministrationCommandService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
/**
 * Проверяет {@code AbstractPersonnelExcelDirectMvpDb} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@SpringBootTest(classes = TrainingPlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(Lifecycle.PER_CLASS)
abstract class AbstractPersonnelExcelDirectMvpDbIntegrationTest {

    protected static final Instant NOW = Instant.parse("2026-05-11T09:00:00Z");
    protected static final Instant EARLIER = NOW.minusSeconds(3600);

    private static final String[] HEADERS = {
        "employeeNumber",
        "externalId",
        "lastName",
        "firstName",
        "middleName",
        "employmentStatus",
        "homeOrgUnitCode",
        "basePositionCode",
        "temporaryPositionCode",
        "temporaryOrgUnitCode",
        "temporaryValidFrom",
        "temporaryValidTo",
        "comment"
    };

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (!postgres.isRunning()) {
            postgres.start();
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    protected PersonnelWorkbookDryRunFacade personnelWorkbookDryRunFacade;
    @Autowired
    protected PersonnelApplyService personnelApplyService;
    @Autowired
    protected OwnerReadPersonnelCurrentStateReader ownerReadPersonnelCurrentStateReader;
    @Autowired
    protected PersonnelExcelImportController personnelExcelImportController;
    @Autowired
    protected SpringDataAppUserJpaRepository appUserRepository;
    @Autowired
    protected SpringDataAppRoleJpaRepository appRoleRepository;
    @Autowired
    protected SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Autowired
    protected SpringDataOrganizationalUnitTypeJpaRepository organizationalUnitTypeRepository;
    @Autowired
    protected SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository;
    @Autowired
    protected SpringDataUserRoleAssignmentJpaRepository userRoleAssignmentRepository;
    @Autowired
    protected SpringDataUserAccessAreaJpaRepository userAccessAreaRepository;
    @Autowired
    protected SpringDataManagementRelationJpaRepository managementRelationRepository;
    @Autowired
    protected SpringDataManagementRelationTypeJpaRepository managementRelationTypeRepository;
    @Autowired
    protected SpringDataTemporaryRoleAssignmentJpaRepository temporaryRoleAssignmentRepository;
    @Autowired
    protected SpringDataTemporaryAccessAreaJpaRepository temporaryAccessAreaRepository;
    @Autowired
    protected SpringDataTemporaryManagementDelegationJpaRepository temporaryManagementDelegationRepository;
    @Autowired
    protected SpringDataImportJobJpaRepository importJobRepository;
    @Autowired
    protected SpringDataImportJobItemJpaRepository importJobItemRepository;
    @Autowired
    protected SpringDataResultJpaRepository resultRepository;
    @Autowired
    protected SpringDataResultQuestionSnapshotJpaRepository resultQuestionSnapshotRepository;
    @Autowired
    protected SpringDataResultAnswerOptionSnapshotJpaRepository resultAnswerOptionSnapshotRepository;
    @Autowired
    protected SpringDataAnswerOptionJpaRepository answerOptionRepository;
    @Autowired
    protected SpringDataNotificationJpaRepository notificationRepository;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @MockitoBean
    protected InteractiveActorResolver interactiveActorResolver;
    @MockitoBean
    protected AccessSpecificationPolicy accessSpecificationPolicy;
    @MockitoBean
    protected CriticalCommandAuditSupport criticalCommandAuditSupport;
    @MockitoBean
    protected UtcClock utcClock;

    @MockitoSpyBean
    protected UserAdministrationCommandService userAdministrationCommandService;
    @MockitoSpyBean
    protected AccessAdministrationCommandService accessAdministrationCommandService;
    @MockitoSpyBean
    protected PersonnelOwnerMutationExecutor personnelOwnerMutationExecutor;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpDirectMvpDbSupport() {
        when(utcClock.now()).thenReturn(NOW);
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(9000L);
        when(accessSpecificationPolicy.resolveReadScope(any())).thenReturn(AccessReadScope.fullAccess());
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(9000L);
        clearInvocations(userAdministrationCommandService, accessAdministrationCommandService, personnelOwnerMutationExecutor);
        mockMvc = MockMvcBuilders.standaloneSetup(personnelExcelImportController).build();
    }

    @AfterEach
    void cleanDirectMvpDbState() {
        List<String> tableNames = jdbcTemplate.queryForList(
            "select tablename from pg_tables where schemaname = 'public' and tablename <> 'flyway_schema_history'",
            String.class
        );
        if (tableNames.isEmpty()) {
            return;
        }
        String joinedTableNames = tableNames.stream()
            .map(tableName -> "\"" + tableName + "\"")
            .collect(Collectors.joining(", "));
        jdbcTemplate.execute("TRUNCATE TABLE " + joinedTableNames + " RESTART IDENTITY CASCADE");
    }

    protected AppUserEntity saveUser(String employeeNumber, String externalId, UserStatus status) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(externalId);
        entity.setLastName("Ivanov");
        entity.setFirstName("Ivan");
        entity.setMiddleName("Ivanovich");
        entity.setStatus(status);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return appUserRepository.saveAndFlush(entity);
    }

    protected AppRoleEntity saveRole(String code) {
        AppRoleEntity entity = instantiate(AppRoleEntity.class);
        entity.setCode(code);
        entity.setName(code);
        entity.setDescription(code);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return appRoleRepository.saveAndFlush(entity);
    }

    protected OrganizationalUnitTypeEntity saveUnitType(String code) {
        OrganizationalUnitTypeEntity entity = instantiate(OrganizationalUnitTypeEntity.class);
        entity.setCode(code);
        entity.setName(code);
        entity.setNodeKind(OrganizationalNodeKind.LINEAR);
        entity.setCanBeOperatorHomeUnit(true);
        entity.setCanBeCampaignTarget(true);
        entity.setParticipatesInSubtreeScope(true);
        entity.setCanHaveManagementRelation(true);
        entity.setCanHaveAccessArea(true);
        entity.setDescription(null);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return organizationalUnitTypeRepository.saveAndFlush(entity);
    }

    protected OrganizationalUnitEntity saveUnit(Long typeId, String name, String path) {
        OrganizationalUnitEntity entity = instantiate(OrganizationalUnitEntity.class);
        entity.setParentId(null);
        entity.setOrganizationalUnitTypeId(typeId);
        entity.setName(name);
        entity.setStatus(OrganizationalUnitStatus.ACTIVE);
        entity.setPath(path);
        entity.setDepth(Math.max(0, (int) path.chars().filter(ch -> ch == '/').count() - 1));
        entity.setExternalId(path);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return organizationalUnitRepository.saveAndFlush(entity);
    }

    protected ManagementRelationTypeEntity saveManagementRelationType(String code) {
        ManagementRelationTypeEntity entity = instantiate(ManagementRelationTypeEntity.class);
        entity.setCode(code);
        entity.setName(code);
        entity.setDescription(code);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return managementRelationTypeRepository.saveAndFlush(entity);
    }

    protected UserOrganizationAssignmentEntity savePrimaryHome(Long userId, Long unitId) {
        UserOrganizationAssignmentEntity entity = instantiate(UserOrganizationAssignmentEntity.class);
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(unitId);
        entity.setAssignmentType(OrganizationAssignmentType.PRIMARY);
        entity.setValidFrom(EARLIER);
        entity.setValidTo(null);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return userOrganizationAssignmentRepository.saveAndFlush(entity);
    }

    protected UserRoleAssignmentEntity saveRoleAssignment(Long userId, Long roleId) {
        UserRoleAssignmentEntity entity = instantiate(UserRoleAssignmentEntity.class);
        entity.setUserId(userId);
        entity.setRoleId(roleId);
        entity.setValidFrom(EARLIER);
        entity.setValidTo(null);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return userRoleAssignmentRepository.saveAndFlush(entity);
    }

    protected UserAccessAreaEntity saveUserAccessArea(Long userId, Long unitId, AccessScopeType scopeType) {
        UserAccessAreaEntity entity = instantiate(UserAccessAreaEntity.class);
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(unitId);
        entity.setAccessScopeType(scopeType);
        entity.setValidFrom(EARLIER);
        entity.setValidTo(null);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return userAccessAreaRepository.saveAndFlush(entity);
    }

    protected ManagementRelationEntity saveManagementRelation(Long userId, Long unitId, Long typeId) {
        ManagementRelationEntity entity = instantiate(ManagementRelationEntity.class);
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(unitId);
        entity.setManagementRelationTypeId(typeId);
        entity.setValidFrom(EARLIER);
        entity.setValidTo(null);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return managementRelationRepository.saveAndFlush(entity);
    }

    protected TemporaryRoleAssignmentEntity saveTemporaryRoleAssignment(Long userId, Long roleId) {
        TemporaryRoleAssignmentEntity entity = instantiate(TemporaryRoleAssignmentEntity.class);
        entity.setUserId(userId);
        entity.setRoleId(roleId);
        entity.setValidFrom(EARLIER);
        entity.setValidTo(null);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return temporaryRoleAssignmentRepository.saveAndFlush(entity);
    }

    protected TemporaryAccessAreaEntity saveTemporaryAccessArea(Long userId, Long unitId, AccessScopeType scopeType) {
        TemporaryAccessAreaEntity entity = instantiate(TemporaryAccessAreaEntity.class);
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(unitId);
        entity.setAccessScopeType(scopeType);
        entity.setValidFrom(EARLIER);
        entity.setValidTo(null);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return temporaryAccessAreaRepository.saveAndFlush(entity);
    }

    protected TemporaryManagementDelegationEntity saveTemporaryManagementDelegation(Long userId, Long unitId, Long typeId) {
        TemporaryManagementDelegationEntity entity = instantiate(TemporaryManagementDelegationEntity.class);
        entity.setUserId(userId);
        entity.setOrganizationalUnitId(unitId);
        entity.setManagementRelationTypeId(typeId);
        entity.setValidFrom(EARLIER);
        entity.setValidTo(null);
        entity.setCreatedAt(EARLIER);
        entity.setUpdatedAt(EARLIER);
        return temporaryManagementDelegationRepository.saveAndFlush(entity);
    }

    protected PersonnelBusinessIntent businessIntent(
        String employeeNumber,
        String externalIdGuard,
        String targetStatus,
        String homeOrgUnitCode,
        String basePositionCode
    ) {
        return new PersonnelBusinessIntent(
            2,
            employeeNumber,
            externalIdGuard,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            true,
            "INACTIVE".equals(targetStatus) ? PersonnelEmploymentAction.DEACTIVATE : PersonnelEmploymentAction.ENSURE_ACTIVE,
            targetStatus,
            homeOrgUnitCode,
            new PersonnelPositionMapping(basePositionCode, roleCodesFor(basePositionCode), managementFor(basePositionCode), accessFor(basePositionCode), false, true),
            null
        );
    }

    private java.util.Set<String> roleCodesFor(String positionCode) {
        return switch (positionCode) {
            case "HEAD" -> java.util.Set.of("ROLE_USER", "ROLE_MANAGER");
            case "OPS" -> java.util.Set.of("ROLE_USER", "ROLE_OPERATIONS");
            default -> java.util.Set.of("ROLE_USER");
        };
    }

    private boolean managementFor(String positionCode) {
        return "HEAD".equals(positionCode);
    }

    private String accessFor(String positionCode) {
        return "HEAD".equals(positionCode) || "OPS".equals(positionCode) ? "UNIT" : "SELF";
    }

    protected byte[] workbook(String[]... rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("personnel");
            var header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                header.createCell(column).setCellValue(HEADERS[column]);
            }
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                var sheetRow = sheet.createRow(rowIndex + 1);
                for (int column = 0; column < HEADERS.length; column++) {
                    sheetRow.createCell(column).setCellValue(rows[rowIndex][column] == null ? "" : rows[rowIndex][column]);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build workbook fixture", exception);
        }
    }

    protected String[] row(
        String employeeNumber,
        String employmentStatus,
        String homeOrgUnitCode,
        String basePositionCode
    ) {
        return row(employeeNumber, employmentStatus, homeOrgUnitCode, basePositionCode, null, null, null, null);
    }

    protected String[] rowWithExternalId(
        String employeeNumber,
        String externalId,
        String employmentStatus,
        String homeOrgUnitCode,
        String basePositionCode
    ) {
        return rowWithExternalId(employeeNumber, externalId, employmentStatus, homeOrgUnitCode, basePositionCode, null, null, null, null);
    }

    protected String[] row(
        String employeeNumber,
        String employmentStatus,
        String homeOrgUnitCode,
        String basePositionCode,
        String temporaryPositionCode,
        String temporaryOrgUnitCode,
        String temporaryValidFrom,
        String temporaryValidTo
    ) {
        return rowWithExternalId(
            employeeNumber,
            "",
            employmentStatus,
            homeOrgUnitCode,
            basePositionCode,
            temporaryPositionCode,
            temporaryOrgUnitCode,
            temporaryValidFrom,
            temporaryValidTo
        );
    }

    protected String[] rowWithExternalId(
        String employeeNumber,
        String externalId,
        String employmentStatus,
        String homeOrgUnitCode,
        String basePositionCode,
        String temporaryPositionCode,
        String temporaryOrgUnitCode,
        String temporaryValidFrom,
        String temporaryValidTo
    ) {
        return new String[] {
            employeeNumber,
            externalId == null ? "" : externalId,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            employmentStatus,
            homeOrgUnitCode,
            basePositionCode,
            temporaryPositionCode == null ? "" : temporaryPositionCode,
            temporaryOrgUnitCode == null ? "" : temporaryOrgUnitCode,
            temporaryValidFrom == null ? "" : temporaryValidFrom,
            temporaryValidTo == null ? "" : temporaryValidTo,
            ""
        };
    }

    protected RuntimeCounts runtimeCounts() {
        return new RuntimeCounts(
            importJobRepository.count(),
            importJobItemRepository.count(),
            resultRepository.count(),
            resultQuestionSnapshotRepository.count(),
            resultAnswerOptionSnapshotRepository.count(),
            answerOptionRepository.count(),
            notificationRepository.count()
        );
    }

    protected long tableCount(String tableName) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return count == null ? 0L : count;
    }

    protected List<String> activeRoleCodes(Long userId, Instant activeAt) {
        List<String> roleCodes = new ArrayList<>();
        for (UserRoleAssignmentEntity assignment : userRoleAssignmentRepository.findActiveByUserId(userId, activeAt)) {
            roleCodes.add(appRoleRepository.findById(assignment.getRoleId()).orElseThrow().getCode());
        }
        return roleCodes;
    }

    protected record RuntimeCounts(
        long importJobs,
        long importJobItems,
        long results,
        long resultQuestionSnapshots,
        long resultAnswerOptionSnapshots,
        long answerOptions,
        long notifications
    ) {
    }

    protected <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity: " + type.getName(), exception);
        }
    }
}
