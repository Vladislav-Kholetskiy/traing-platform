package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AccessAdministrationPersonnelMutation}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AccessAdministrationPersonnelMutationContractTest {

    @Test
    void ownerSeamsExistForUserAccessAreaAssignAndClose() throws Exception {
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "assignUserAccessArea",
            Long.class,
            Long.class,
            AccessScopeType.class,
            Instant.class
        )).isNotNull();
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "closeUserAccessArea",
            Long.class,
            Instant.class
        )).isNotNull();
    }

    @Test
    void ownerSeamsExistForManagementRelationAssignAndClose() throws Exception {
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "assignManagementRelation",
            Long.class,
            Long.class,
            Long.class,
            Instant.class
        )).isNotNull();
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "closeManagementRelation",
            Long.class,
            Instant.class
        )).isNotNull();
    }

    @Test
    void ownerSeamsExistForTemporaryRoleAssignmentAssignAndClose() throws Exception {
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "assignTemporaryRoleAssignment",
            Long.class,
            Long.class,
            Instant.class
        )).isNotNull();
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "closeTemporaryRoleAssignment",
            Long.class,
            Instant.class
        )).isNotNull();
    }

    @Test
    void ownerSeamsExistForTemporaryAccessAreaAssignAndClose() throws Exception {
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "assignTemporaryAccessArea",
            Long.class,
            Long.class,
            AccessScopeType.class,
            Instant.class
        )).isNotNull();
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "closeTemporaryAccessArea",
            Long.class,
            Instant.class
        )).isNotNull();
    }

    @Test
    void ownerSeamsExistForTemporaryManagementDelegationAssignAndClose() throws Exception {
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "assignTemporaryManagementDelegation",
            Long.class,
            Long.class,
            Long.class,
            Instant.class
        )).isNotNull();
        assertThat(AccessAdministrationCommandService.class.getMethod(
            "closeTemporaryManagementDelegation",
            Long.class,
            Instant.class
        )).isNotNull();
    }

    @Test
    void temporaryManagerialAuthorityHasDedicatedDelegationSeam() throws Exception {
        String serviceInterface = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/AccessAdministrationCommandService.java"
        ));
        String serviceImpl = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/AccessAdministrationCommandServiceImpl.java"
        ));
        String validationSupport = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/AccessCommandValidationSupport.java"
        ));

        assertThat(serviceInterface)
            .contains("assignTemporaryManagementDelegation")
            .contains("closeTemporaryManagementDelegation");
        assertThat(serviceImpl)
            .contains("temporaryManagementDelegationCommandService")
            .contains("saveTemporaryManagementDelegation")
            .contains("endTemporaryManagementDelegation")
            .doesNotContain("replace temporary management with access area");
        assertThat(validationSupport)
            .contains("ensureTemporaryManagementAssignable")
            .contains("ensureTemporaryManagementClosable");
    }
}
