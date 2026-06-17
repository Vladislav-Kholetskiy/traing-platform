package com.vladislav.training.platform.integration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    classes = ImportPersistenceInvariantIntegrationTest.ImportPersistenceInvariantTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code ImportPersistenceInvariant} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class ImportPersistenceInvariantIntegrationTest {

    private static final String PERSISTENCE_PACKAGE =
        "com.vladislav.training.platform.integration.infrastructure.persistence.";
    private static final Path V100_SCHEMA = Path.of("src/main/resources/db/migration/V100__full_schema_stack.sql");
    private static final Path IMPORT_CONTROLLER_PACKAGE =
        Path.of("src/main/java/com/vladislav/training/platform/integration/controller");
    private static final Path IMPORT_PERSISTENCE_PACKAGE =
        Path.of("src/main/java/com/vladislav/training/platform/integration/infrastructure/persistence");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T09:00:00Z");

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
    private SpringDataImportJobJpaRepository importJobRepository;

    @Autowired
    private SpringDataImportJobItemJpaRepository importJobItemRepository;

    @AfterEach
    void cleanDatabase() {
        importJobItemRepository.deleteAllInBatch();
        importJobRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
    }

    @Test
    void importPersistenceContourClassesMustExist() {
        assertThatCode(() -> requireClass("ImportJobEntity")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("ImportJobItemEntity")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("SpringDataImportJobJpaRepository")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("SpringDataImportJobItemJpaRepository")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("JpaImportJobRepositoryAdapter")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("JpaImportJobItemRepositoryAdapter")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("ImportMapper")).doesNotThrowAnyException();
    }

    @Test
    void importEntitiesMustMatchAdministrativeJpaAndJsonbShape() throws Exception {
        Class<?> importJobEntityType = requireClass("ImportJobEntity");
        Class<?> importJobItemEntityType = requireClass("ImportJobItemEntity");

        assertScalarEntityFieldSet(
            importJobEntityType,
            Set.of(
                "id",
                "sourceType",
                "sourceRef",
                "initiatedByUserId",
                "status",
                "payload",
                "startedAt",
                "completedAt",
                "totalItemCount",
                "processedItemCount",
                "appliedItemCount",
                "failedItemCount",
                "requiresReviewItemCount",
                "createdAt",
                "updatedAt"
            )
        );
        assertTable(importJobEntityType, "import_job");
        assertIdField(importJobEntityType, "id");
        assertColumn(importJobEntityType, "sourceType", "source_type", false);
        assertColumn(importJobEntityType, "sourceRef", "source_ref", true);
        assertColumn(importJobEntityType, "initiatedByUserId", "initiated_by_user_id", true);
        assertColumn(importJobEntityType, "status", "status", false);
        assertColumn(importJobEntityType, "payload", "payload", true);
        assertColumn(importJobEntityType, "startedAt", "started_at", true);
        assertColumn(importJobEntityType, "completedAt", "completed_at", true);
        assertColumn(importJobEntityType, "totalItemCount", "total_item_count", true);
        assertColumn(importJobEntityType, "processedItemCount", "processed_item_count", true);
        assertColumn(importJobEntityType, "appliedItemCount", "applied_item_count", true);
        assertColumn(importJobEntityType, "failedItemCount", "failed_item_count", true);
        assertColumn(importJobEntityType, "requiresReviewItemCount", "requires_review_item_count", true);
        assertColumn(importJobEntityType, "createdAt", "created_at", false);
        assertColumn(importJobEntityType, "updatedAt", "updated_at", false);
        assertJsonbField(importJobEntityType, "payload");
        assertPlainPersistenceCarrier(importJobEntityType);

        assertScalarEntityFieldSet(
            importJobItemEntityType,
            Set.of(
                "id",
                "importJobId",
                "itemNo",
                "targetEntityType",
                "externalId",
                "employeeNumber",
                "status",
                "matchedEntityId",
                "payload",
                "errorCode",
                "errorMessage",
                "processedAt",
                "createdAt",
                "updatedAt"
            )
        );
        assertTable(importJobItemEntityType, "import_job_item");
        assertIdField(importJobItemEntityType, "id");
        assertColumn(importJobItemEntityType, "importJobId", "import_job_id", false);
        assertColumn(importJobItemEntityType, "itemNo", "item_no", false);
        assertColumn(importJobItemEntityType, "targetEntityType", "target_entity_type", false);
        assertColumn(importJobItemEntityType, "externalId", "external_id", true);
        assertColumn(importJobItemEntityType, "employeeNumber", "employee_number", true);
        assertColumn(importJobItemEntityType, "status", "status", false);
        assertColumn(importJobItemEntityType, "matchedEntityId", "matched_entity_id", true);
        assertColumn(importJobItemEntityType, "payload", "payload", true);
        assertColumn(importJobItemEntityType, "errorCode", "error_code", true);
        assertColumn(importJobItemEntityType, "errorMessage", "error_message", true);
        assertColumn(importJobItemEntityType, "processedAt", "processed_at", true);
        assertColumn(importJobItemEntityType, "createdAt", "created_at", false);
        assertColumn(importJobItemEntityType, "updatedAt", "updated_at", false);
        assertJsonbField(importJobItemEntityType, "payload");
        assertPlainPersistenceCarrier(importJobItemEntityType);
    }

    @Test
    void validImportJobPersistsAndJsonbPayloadRoundTrips() {
        AppUserEntity initiator = appUserRepository.saveAndFlush(appUserEntity("EMP-IMPORT-JOB"));

        ImportJobEntity entity = validImportJobEntity(initiator.getId());
        entity.setPayload("{\"source\":\"hr\",\"rows\":3,\"mode\":\"preview\"}");
        entity.setStartedAt(FIXED_INSTANT.plusSeconds(300));

        ImportJobEntity persisted = importJobRepository.saveAndFlush(entity);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getSourceType()).isEqualTo("HR_CSV");
        assertThat(persisted.getInitiatedByUserId()).isEqualTo(initiator.getId());
        assertThat(persisted.getPayload()).contains("\"source\":\"hr\"");

        ImportJobEntity reloaded = importJobRepository.findById(persisted.getId()).orElseThrow();
        assertThat(reloaded.getPayload()).contains("\"rows\": 3");
        assertThat(reloaded.getProcessedItemCount()).isZero();
    }

    @Test
    void importJobConstraintsAreEnforcedByDatabase() {
        AppUserEntity initiator = appUserRepository.saveAndFlush(appUserEntity("EMP-IMPORT-CONSTRAINTS"));

        assertThatThrownBy(() -> importJobRepository.saveAndFlush(importJobWith(
            initiator.getId(),
            "   ",
            "PENDING",
            1,
            0,
            0,
            0,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> importJobRepository.saveAndFlush(importJobWith(
            initiator.getId(),
            "HR_CSV",
            "   ",
            1,
            0,
            0,
            0,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> importJobRepository.saveAndFlush(importJobWith(
            initiator.getId(),
            "HR_CSV",
            "PENDING",
            -1,
            0,
            0,
            0,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> importJobRepository.saveAndFlush(importJobWith(
            initiator.getId(),
            "HR_CSV",
            "PENDING",
            3,
            4,
            1,
            1,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> importJobRepository.saveAndFlush(importJobWith(
            initiator.getId(),
            "HR_CSV",
            "COMPLETED",
            3,
            3,
            2,
            1,
            0,
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        ImportJobEntity validTerminal = importJobWith(
            initiator.getId(),
            "HR_CSV",
            "COMPLETED_WITH_ERRORS",
            3,
            3,
            1,
            1,
            1,
            FIXED_INSTANT.plusSeconds(900)
        );
        ImportJobEntity saved = importJobRepository.saveAndFlush(validTerminal);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void validImportJobItemPersistsAndItemConstraintsAreEnforced() {
        AppUserEntity initiator = appUserRepository.saveAndFlush(appUserEntity("EMP-IMPORT-ITEM"));
        ImportJobEntity job = importJobRepository.saveAndFlush(validImportJobEntity(initiator.getId()));

        ImportJobItemEntity validItem = validImportJobItemEntity(job.getId(), 0);
        validItem.setPayload("{\"employee\":\"EMP-100\",\"row\":1}");
        ImportJobItemEntity savedItem = importJobItemRepository.saveAndFlush(validItem);

        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getImportJobId()).isEqualTo(job.getId());
        assertThat(savedItem.getPayload()).contains("\"employee\":\"EMP-100\"");

        assertThatThrownBy(() -> importJobItemRepository.saveAndFlush(importJobItemWith(
            job.getId(),
            -1,
            "APP_USER",
            "PENDING",
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> importJobItemRepository.saveAndFlush(importJobItemWith(
            job.getId(),
            1,
            "   ",
            "PENDING",
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> importJobItemRepository.saveAndFlush(importJobItemWith(
            job.getId(),
            2,
            "APP_USER",
            "   ",
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> importJobItemRepository.saveAndFlush(importJobItemWith(
            job.getId(),
            3,
            "APP_USER",
            "FAILED",
            null
        ))).isInstanceOf(DataIntegrityViolationException.class);

        importJobItemRepository.saveAndFlush(validImportJobItemEntity(job.getId(), 7));
        assertThatThrownBy(() -> importJobItemRepository.saveAndFlush(validImportJobItemEntity(job.getId(), 7)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void importPersistenceBoundaryStaysScalarOnlyAndAllowsOnlyStage61ControllerOutsidePersistence() throws Exception {
        assertThat(Files.exists(IMPORT_CONTROLLER_PACKAGE)).isTrue();

        String persistenceSource = Files.walk(IMPORT_PERSISTENCE_PACKAGE)
            .filter(path -> path.toString().endsWith(".java"))
            .map(this::readString)
            .collect(Collectors.joining("\n"));

        assertThat(persistenceSource)
            .doesNotContain("AssignmentEntity")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ResultEntity")
            .doesNotContain("CourseEntity")
            .doesNotContain("UserEntity")
            .doesNotContain("AssignmentRepository")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("ResultRepository")
            .doesNotContain("CourseRepository")
            .doesNotContain("@RestController")
            .doesNotContain("@RequestMapping");

        try (var migrations = Files.list(Path.of("src/main/resources/db/migration"))) {
            List<Path> postV100Migrations = migrations
                .filter(path -> path.getFileName().toString().matches("V\\d+__.*\\.sql"))
                .filter(path -> !path.getFileName().toString().startsWith("V100__"))
                .toList();

            for (Path migration : postV100Migrations) {
                String source = Files.readString(migration).toLowerCase(Locale.ROOT);
                assertThat(source).doesNotContain("create table import_job");
                assertThat(source).doesNotContain("alter table import_job");
                assertThat(source).doesNotContain("create table import_job_item");
                assertThat(source).doesNotContain("alter table import_job_item");
            }
        }
    }

    private Class<?> requireClass(String simpleName) throws ClassNotFoundException {
        return Class.forName(PERSISTENCE_PACKAGE + simpleName);
    }

    private void assertTable(Class<?> entityType, String expectedTableName) {
        assertThat(entityType.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(entityType.isAnnotationPresent(Table.class)).isTrue();
        assertThat(entityType.getAnnotation(Table.class).name()).isEqualTo(expectedTableName);
    }

    private void assertIdField(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(Id.class)).isTrue();
        assertThat(field.isAnnotationPresent(GeneratedValue.class)).isTrue();
    }

    private void assertScalarEntityFieldSet(Class<?> entityType, Set<String> expectedFieldNames) {
        assertThat(fieldNames(entityType)).isEqualTo(expectedFieldNames);
    }

    private void assertColumn(Class<?> entityType, String fieldName, String expectedColumnName, boolean expectedNullable)
        throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(Column.class)).isTrue();
        Column column = field.getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(expectedColumnName);
        assertThat(column.nullable()).isEqualTo(expectedNullable);
    }

    private void assertJsonbField(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(JdbcTypeCode.class)).isTrue();
        assertThat(field.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.JSON);
    }

    private void assertPlainPersistenceCarrier(Class<?> entityType) {
        boolean persistenceAnnotationsOnly = Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .allMatch(field -> field.isAnnotationPresent(Column.class)
                || field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(GeneratedValue.class)
                || field.isAnnotationPresent(JdbcTypeCode.class));
        boolean scalarFieldTypesOnly = Stream.of(entityType.getDeclaredFields())
            .map(Field::getType)
            .allMatch(this::isPlainPersistenceFieldType);
        boolean noCollectionFields = Stream.of(entityType.getDeclaredFields())
            .map(Field::getType)
            .noneMatch(Collection.class::isAssignableFrom);

        assertThat(entityType.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(persistenceAnnotationsOnly).isTrue();
        assertThat(scalarFieldTypesOnly).isTrue();
        assertThat(noCollectionFields).isTrue();
    }

    private Set<String> fieldNames(Class<?> entityType) {
        return Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isPlainPersistenceFieldType(Class<?> fieldType) {
        return fieldType.equals(Long.class)
            || fieldType.equals(Integer.class)
            || fieldType.equals(int.class)
            || fieldType.equals(String.class)
            || fieldType.equals(Instant.class)
            || fieldType.isEnum();
    }

    private ImportJobEntity validImportJobEntity(Long initiatedByUserId) {
        return importJobWith(initiatedByUserId, "HR_CSV", "PENDING", 3, 0, 0, 0, 0, null);
    }

    private ImportJobEntity importJobWith(
        Long initiatedByUserId,
        String sourceType,
        String status,
        Integer totalItemCount,
        Integer processedItemCount,
        Integer appliedItemCount,
        Integer failedItemCount,
        Integer requiresReviewItemCount,
        Instant completedAt
    ) {
        ImportJobEntity entity = instantiate(ImportJobEntity.class);
        entity.setSourceType(sourceType);
        entity.setSourceRef("hr-feed-2026-05.csv");
        entity.setInitiatedByUserId(initiatedByUserId);
        entity.setStatus(status);
        entity.setPayload("{\"rows\":3}");
        entity.setStartedAt(FIXED_INSTANT.plusSeconds(120));
        entity.setCompletedAt(completedAt);
        entity.setTotalItemCount(totalItemCount);
        entity.setProcessedItemCount(processedItemCount);
        entity.setAppliedItemCount(appliedItemCount);
        entity.setFailedItemCount(failedItemCount);
        entity.setRequiresReviewItemCount(requiresReviewItemCount);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private ImportJobItemEntity validImportJobItemEntity(Long importJobId, int itemNo) {
        return importJobItemWith(importJobId, itemNo, "APP_USER", "PENDING", null);
    }

    private ImportJobItemEntity importJobItemWith(
        Long importJobId,
        int itemNo,
        String targetEntityType,
        String status,
        Instant processedAt
    ) {
        ImportJobItemEntity entity = instantiate(ImportJobItemEntity.class);
        entity.setImportJobId(importJobId);
        entity.setItemNo(itemNo);
        entity.setTargetEntityType(targetEntityType);
        entity.setExternalId("EXT-" + itemNo);
        entity.setEmployeeNumber("EMP-" + itemNo);
        entity.setStatus(status);
        entity.setMatchedEntityId(null);
        entity.setPayload("{\"row\":" + (itemNo + 1) + "}");
        entity.setErrorCode(null);
        entity.setErrorMessage(null);
        entity.setProcessedAt(processedAt);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AppUserEntity appUserEntity(String employeeNumber) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(null);
        entity.setLastName("Import");
        entity.setFirstName(employeeNumber);
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
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

    private String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read persistence source: " + path, exception);
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AppUserEntity.class,
        ImportJobEntity.class,
        ImportJobItemEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAppUserJpaRepository.class,
        SpringDataImportJobJpaRepository.class,
        SpringDataImportJobItemJpaRepository.class
    })
    static class ImportPersistenceInvariantTestApplication {
    }
}
