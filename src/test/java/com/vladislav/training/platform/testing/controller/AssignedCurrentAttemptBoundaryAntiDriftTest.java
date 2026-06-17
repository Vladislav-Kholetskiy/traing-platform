package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Не даёт {@code AssignedCurrentAttemptBoundary} незаметно уйти от текущего замысла.
 * Тест страхует важные ограничения и исходную идею решения.
 */
class AssignedCurrentAttemptBoundaryAntiDriftTest {

    @Test
    void assignedCurrentAttemptControllerMustStayThinAndMustNotOwnOwnerLocalLookup() throws Exception {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/controller/CurrentAttemptReadController.java"))
            
            .doesNotContain("activeAttemptOwnerLocalReadService.findActiveAssignedAttempt(assignmentTestId)")
            .doesNotContain("activeAttemptOwnerLocalReadService.findActiveAssignedAttemptForActor(")
            .contains("assignedCurrentAttemptReadService.findCurrentAssignedAttemptForActor(");
    }

    @Test
    void assignedCurrentAttemptSupportingReadMustUseTestingSpecificPolicyContourBeforeOwnerLocalMaterialization() throws Exception {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/AssignedCurrentAttemptReadService.java"))
            
            .contains("AccessReadArea.ASSIGNED_CURRENT_ATTEMPT")
            .contains("contextResolver.resolveActorSelfScope(")
            .contains("accessSpecificationPolicy.canRead(")
            .contains("\"assigned_current_attempt\"")
            .contains("activeAttemptOwnerLocalReadService.findActiveAssignedAttemptForActor(");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
