package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code IntegrationDoesNotImportOwnerJpaAdapters} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class IntegrationDoesNotImportOwnerJpaAdaptersRegressionTest {

    private static final Path INTEGRATION_SRC = Path.of(
        "src/main/java/com/vladislav/training/platform/integration"
    );
    private static final Path IMPORT_PROCESSING_SERVICE = INTEGRATION_SRC.resolve(
        "service/ImportProcessingServiceImpl.java"
    );
    private static final Path IMPORT_COMMAND_SERVICE = INTEGRATION_SRC.resolve(
        "service/ImportCommandServiceImpl.java"
    );
    private static final Path IMPORT_REVIEW_SERVICE = INTEGRATION_SRC.resolve(
        "service/ImportItemReviewServiceImpl.java"
    );

    @Test
    void integrationSourcesMustNotImportOwnerJpaEntitiesRepositoriesOrAdapters() throws Exception {
        try (Stream<Path> stream = Files.walk(INTEGRATION_SRC)) {
            for (Path path : stream.filter(file -> file.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertThat(source)
                    
                    .doesNotContain(".userorg.infrastructure.persistence.")
                    .doesNotContain(".content.infrastructure.persistence.")
                    .doesNotContain(".assignment.infrastructure.persistence.")
                    .doesNotContain(".testing.infrastructure.persistence.")
                    .doesNotContain(".result.infrastructure.persistence.")
                    .doesNotContain("AppUserEntity")
                    .doesNotContain("OrganizationalUnitEntity")
                    .doesNotContain("UserOrganizationAssignmentEntity")
                    .doesNotContain("UserRoleAssignmentEntity")
                    .doesNotContain("CourseEntity")
                    .doesNotContain("MaterialEntity")
                    .doesNotContain("QuestionEntity")
                    .doesNotContain("TestEntity")
                    .doesNotContain("TopicEntity")
                    .doesNotContain("AssignmentEntity")
                    .doesNotContain("AssignmentCampaignEntity")
                    .doesNotContain("AssignmentTestEntity")
                    .doesNotContain("TestAttemptEntity")
                    .doesNotContain("UserAnswerEntity")
                    .doesNotContain("ResultEntity")
                    .doesNotContain("SpringDataAppUserJpaRepository")
                    .doesNotContain("SpringDataOrganizationalUnitJpaRepository")
                    .doesNotContain("SpringDataCourseJpaRepository")
                    .doesNotContain("SpringDataAssignmentJpaRepository")
                    .doesNotContain("SpringDataTestAttemptJpaRepository")
                    .doesNotContain("SpringDataResultJpaRepository")
                    .doesNotContain("JpaAppUserRepositoryAdapter")
                    .doesNotContain("JpaOrganizationalUnitRepositoryAdapter")
                    .doesNotContain("JpaCourseRepositoryAdapter")
                    .doesNotContain("JpaAssignmentRepositoryAdapter")
                    .doesNotContain("JpaTestAttemptRepositoryAdapter")
                    .doesNotContain("JpaResultRepositoryAdapter");
            }
        }
    }

    @Test
    void integrationSourcesMustNotUseNativeSqlOrJpaEscapeHatchesToPatchOwnerTables() throws Exception {
        try (Stream<Path> stream = Files.walk(INTEGRATION_SRC)) {
            for (Path path : stream.filter(file -> file.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertThat(source)
                    
                    .doesNotContain("EntityManager")
                    .doesNotContain("JdbcTemplate")
                    .doesNotContain("createNativeQuery(")
                    .doesNotContain("nativeQuery = true")
                    .doesNotContain("nativeQuery=true")
                    .doesNotContain("executeUpdate(")
                    .doesNotContain("insert into app_user")
                    .doesNotContain("update app_user")
                    .doesNotContain("delete from app_user")
                    .doesNotContain("insert into assignment")
                    .doesNotContain("update assignment")
                    .doesNotContain("delete from assignment")
                    .doesNotContain("insert into result")
                    .doesNotContain("update result")
                    .doesNotContain("delete from result")
                    .doesNotContain("insert into test_attempt")
                    .doesNotContain("update test_attempt")
                    .doesNotContain("delete from test_attempt")
                    .doesNotContain("insert into course")
                    .doesNotContain("update course")
                    .doesNotContain("delete from course");
            }
        }
    }

    @Test
    void typedOwnerEffectsMayUseAcceptedSeamsButNotOwnerPersistenceAdapters() throws Exception {
        String command = Files.readString(IMPORT_COMMAND_SERVICE);
        String processing = Files.readString(IMPORT_PROCESSING_SERVICE);
        String review = Files.readString(IMPORT_REVIEW_SERVICE);

        assertThat(command)
            .doesNotContain("AppUserRepository")
            .doesNotContain("UserCommandService")
            .doesNotContain(".userorg.infrastructure.persistence.");

        assertThat(processing)
            .contains("AppUserRepository")
            .contains("UserCommandService")
            .contains("appUserRepository.findAllUsers()")
            .contains("userCommandService.updateUser(")
            .doesNotContain(".userorg.infrastructure.persistence.")
            .doesNotContain("SpringDataAppUserJpaRepository")
            .doesNotContain("JpaAppUserRepositoryAdapter")
            .doesNotContain("AppUserEntity")
            .doesNotContain("appUserRepository.save(")
            .doesNotContain("appUserRepository.delete(")
            .doesNotContain("appUserRepository.update(");

        assertThat(review)
            .contains("AppUserRepository")
            .contains("UserCommandService")
            .contains("appUserRepository.findUserById(")
            .contains("userCommandService.updateUser(")
            .doesNotContain(".userorg.infrastructure.persistence.")
            .doesNotContain("SpringDataAppUserJpaRepository")
            .doesNotContain("JpaAppUserRepositoryAdapter")
            .doesNotContain("AppUserEntity")
            .doesNotContain("appUserRepository.save(")
            .doesNotContain("appUserRepository.delete(")
            .doesNotContain("appUserRepository.update(");
    }

    @Test
    void importProcessingMustNotMarkAppliedBeforeTypedOwnerCommandSucceeds() throws Exception {
        String source = Files.readString(IMPORT_PROCESSING_SERVICE);

        assertThat(source)
            .contains("importTypedOwnerCommandExecutor.updateAppUser(")
            .contains("userCommandService.updateUser(")
            .contains("ImportItemStatus.APPLIED");

        int ownerCommandIndex = source.indexOf("importTypedOwnerCommandExecutor.updateAppUser(");
        int appliedIndex = source.lastIndexOf("ImportItemStatus.APPLIED");

        assertThat(ownerCommandIndex).isGreaterThanOrEqualTo(0);
        assertThat(appliedIndex).isGreaterThan(ownerCommandIndex);
        assertThat(source)
            .contains("OWNER_COMMAND_FAILED")
            .contains("ImportItemStatus.FAILED")
            .contains("ImportItemStatus.REQUIRES_REVIEW");
    }
}
