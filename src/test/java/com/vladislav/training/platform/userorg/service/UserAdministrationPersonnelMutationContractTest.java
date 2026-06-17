package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code UserAdministrationPersonnelMutation}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class UserAdministrationPersonnelMutationContractTest {

    @Test
    void replacePrimaryHomeUnitOwnerSeamExists() throws Exception {
        Method method = UserAdministrationCommandService.class.getMethod(
            "replacePrimaryHomeUnit",
            Long.class,
            Long.class,
            Instant.class
        );

        assertThat(method).isNotNull();
    }

    @Test
    void transferSemanticsStayInsideOwnerServiceInsteadOfIntegrationPatch() throws Exception {
        String integrationPersonnelSource = readTree(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel"
        ));

        assertThat(integrationPersonnelSource)
            .doesNotContain("UserOrganizationAssignmentEntity")
            .doesNotContain("userorg.infrastructure.persistence")
            .doesNotContain("saveOrganizationAssignment")
            .doesNotContain("endOrganizationAssignment");
    }

    @Test
    void deactivateUserOwnerSeamExistsAndCoordinatesClosureOrchestration() throws Exception {
        Method method = UserAdministrationCommandService.class.getMethod("deactivateUser", Long.class);
        String serviceImpl = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/userorg/service/UserAdministrationCommandServiceImpl.java"
        ));

        assertThat(method).isNotNull();
        assertThat(serviceImpl)
            .contains("closeActiveRoleAssignmentsByUserId")
            .contains("closeActiveOrganizationAssignmentsByUserId")
            .contains("closeActiveUserAccessAreasByUserId")
            .contains("closeActiveManagementRelationsByUserId")
            .contains("closeActiveTemporaryRoleAssignmentsByUserId")
            .contains("closeActiveTemporaryAccessAreasByUserId")
            .contains("closeActiveTemporaryManagementDelegationsByUserId")
            .contains("deactivateUserAfterAdmission")
            .doesNotContain("status = INACTIVE")
            .doesNotContain(".status(UserStatus.INACTIVE)");
    }

    @Test
    void ownerSeamKeepsPrimaryHomeInvariantAndRejectsSecondActivePrimary() throws Exception {
        String validationSupport = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/userorg/service/UserOrganizationAssignmentValidationSupport.java"
        ));
        Method method = UserOrganizationAssignmentService.class.getMethod(
            "assignOrganizationAssignment",
            com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment.class
        );

        assertThat(method).isNotNull();
        assertThat(validationSupport)
            .contains("User cannot have two simultaneously active PRIMARY assignments")
            .contains("Inactive user cannot receive new active organization assignment")
            .contains("Inactive user cannot replace PRIMARY home unit");
    }

    @Test
    void ownerSeamExposesPrimaryAssignmentTypeInsteadOfGenericPatch() throws Exception {
        String interfaceSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/userorg/service/UserAdministrationCommandService.java"
        ));

        assertThat(interfaceSource)
            .contains(OrganizationAssignmentType.class.getSimpleName())
            .doesNotContain("patch")
            .doesNotContain("reconcile");
    }

    private String readTree(Path root) throws Exception {
        return Files.walk(root)
            .filter(Files::isRegularFile)
            .map(path -> {
                try {
                    return Files.readString(path);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            })
            .reduce("", (left, right) -> left + "\n" + right);
    }
}
