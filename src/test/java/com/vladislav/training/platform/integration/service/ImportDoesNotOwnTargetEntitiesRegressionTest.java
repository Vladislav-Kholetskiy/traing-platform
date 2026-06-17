package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImportDoesNotOwnTargetEntities} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportDoesNotOwnTargetEntitiesRegressionTest {

    @Test
    void integrationControllersMustStayOwnerDecoupledAndTyped() throws Exception {
        String commandController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminCommandController.java"
        );
        String readController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminReadController.java"
        );
        String reviewController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportItemReviewController.java"
        );

        assertControllerBoundary(commandController);
        assertControllerBoundary(readController);
        assertControllerBoundary(reviewController);

        assertThat(commandController).contains("ImportCommandService");
        assertThat(readController).contains("ImportAdminReadService");
        assertThat(reviewController).contains("ImportItemReviewService");
    }

    @Test
    void importCommandServiceMustOnlyMaterializeTrackingRows() throws Exception {
        String source = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"
        );

        assertThat(source)
            .contains("ImportJobRepository")
            .contains("ImportJobItemRepository")
            .contains("saveImportJob(")
            .contains("saveImportJobItem(")
            .doesNotContain("UserCommandService")
            .doesNotContain("AppUserRepository")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("AssignmentRepository")
            .doesNotContain("ContentRepository")
            .doesNotContain("CourseRepository")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("ResultRepository")
            .doesNotContain("Analytics")
            .doesNotContain("EntityManager")
            .doesNotContain("JdbcTemplate")
            .doesNotContain("createNativeQuery(")
            .doesNotContain("executeUpdate(")
            .doesNotContain("updateUser(")
            .doesNotContain("appUser")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping");
    }

    @Test
    void processingAndReviewMustUseTypedOwnerSeamWithoutDirectOwnerPatch() throws Exception {
        String processing = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportProcessingServiceImpl.java"
        );
        String review = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportItemReviewServiceImpl.java"
        );

        assertTypedOwnerSeamOnly(processing);
        assertTypedOwnerSeamOnly(review);

        assertThat(processing)
            .contains("appUserRepository.findAllUsers()")
            .contains("userCommandService.updateUser(")
            .contains("matchedUser.employeeNumber()")
            .contains("matchedUser.externalId()")
            .contains("Payload employeeNumber does not match anchored app_user")
            .contains("Payload externalId does not match anchored app_user");

        assertThat(review)
            .contains("appUserRepository.findUserById(")
            .contains("userCommandService.updateUser(")
            .contains("matchedUser.employeeNumber()")
            .contains("matchedUser.externalId()")
            .contains("Payload employeeNumber does not match reviewed app_user")
            .contains("Payload externalId does not match reviewed app_user");
    }

    @Test
    void importApiAndDtosMustNotExposeGenericOwnerPatchSurface() throws Exception {
        String commandController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminCommandController.java"
        );
        String readController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminReadController.java"
        );
        String reviewController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportItemReviewController.java"
        );

        assertNoGenericPatchSurface(commandController);
        assertNoGenericPatchSurface(readController);
        assertNoGenericPatchSurface(reviewController);

        try (Stream<Path> stream = Files.walk(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/controller/dto"
        ))) {
            for (Path path : stream.filter(file -> file.toString().endsWith(".java")).toList()) {
                assertNoGenericPatchSurface(Files.readString(path));
            }
        }
    }

    @Test
    void importItemStatusVocabularyMustStayBaselineOnly() throws Exception {
        String statusEnum = read(
            "src/main/java/com/vladislav/training/platform/integration/domain/ImportItemStatus.java"
        );

        assertThat(statusEnum)
            .contains("PENDING")
            .contains("PROCESSING")
            .contains("APPLIED")
            .contains("NO_CHANGE")
            .contains("FAILED")
            .contains("REQUIRES_REVIEW")
            .doesNotContain("RESOLVED")
            .doesNotContain("REJECTED")
            .doesNotContain("OWNER_UPDATED")
            .doesNotContain("PATCHED")
            .doesNotContain("UPSERTED");
    }

    private void assertControllerBoundary(String source) {
        assertThat(source)
            .doesNotContain("UserCommandService")
            .doesNotContain("AppUserRepository")
            .doesNotContain("AssignmentRepository")
            .doesNotContain("ContentRepository")
            .doesNotContain("CourseRepository")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("ResultRepository")
            .doesNotContain("Analytics")
            .doesNotContain("AppUser ")
            .doesNotContain("AppUser<")
            .doesNotContain("EntityManager")
            .doesNotContain("JdbcTemplate")
            .doesNotContain("createNativeQuery(")
            .doesNotContain("executeUpdate(");
    }

    private void assertTypedOwnerSeamOnly(String source) {
        assertThat(source)
            .doesNotContain("EntityManager")
            .doesNotContain("JdbcTemplate")
            .doesNotContain("createNativeQuery(")
            .doesNotContain("executeUpdate(")
            .doesNotContain("AssignmentRepository")
            .doesNotContain("ContentRepository")
            .doesNotContain("CourseRepository")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("ResultRepository")
            .doesNotContain("Analytics")
            .doesNotContain("appUserRepository.save(")
            .doesNotContain("appUserRepository.saveUser(")
            .doesNotContain("appUserRepository.delete(")
            .doesNotContain("appUserRepository.update(")
            .doesNotContain("insert into app_user")
            .doesNotContain("update app_user")
            .doesNotContain("delete from app_user");
    }

    private void assertNoGenericPatchSurface(String source) {
        assertThat(source)
            .doesNotContain("/patch")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("targetTable")
            .doesNotContain("targetColumn")
            .doesNotContain("ownerTable")
            .doesNotContain("ownerField")
            .doesNotContain("sql")
            .doesNotContain("operation")
            .doesNotContain("patch");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath));
    }
}
