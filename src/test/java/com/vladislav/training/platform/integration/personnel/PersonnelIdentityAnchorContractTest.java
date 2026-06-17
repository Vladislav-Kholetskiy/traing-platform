package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.service.PersonnelChangePlanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code PersonnelIdentityAnchor}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class PersonnelIdentityAnchorContractTest {

    private final PersonnelChangePlanner planner = new PersonnelChangePlanner();

    @Test
    void unresolvedActiveEmployeeBecomesExplicitCreatePlan() {
        PersonnelPlan plan = planner.plan(createIntent(null), PersonnelIdentityResolution.unresolved("1001"));

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED);
        assertThat(plan.decision()).isEqualTo(com.vladislav.training.platform.integration.personnel.model.PersonnelRowDecision.CREATE_USER);
        assertThat(plan.plannedMutations())
            .extracting(mutation -> mutation.mutationType())
            .contains(com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType.CREATE_USER);
    }

    @Test
    void unresolvedInactiveOrDismissedEmployeeRemainsFailClosed() {
        PersonnelPlan inactivePlan = planner.plan(
            new PersonnelBusinessIntent(
                2,
                "1001",
                null,
                "Ivanov",
                "Ivan",
                "Ivanovich",
                true,
                PersonnelEmploymentAction.DEACTIVATE,
                "INACTIVE",
                "HQ",
                new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
                null
            ),
            PersonnelIdentityResolution.unresolved("1001")
        );

        assertThat(inactivePlan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.FAIL_CLOSED);
        assertThat(inactivePlan.decision()).isEqualTo(com.vladislav.training.platform.integration.personnel.model.PersonnelRowDecision.FAIL_CLOSED);
    }

    @Test
    void externalIdMismatchUsesDedicatedIdentityMismatchOutcome() {
        PersonnelPlan plan = planner.plan(
            intent("expected-ext"),
            PersonnelIdentityResolution.identityMismatch("1001", "expected-ext", "actual-ext")
        );

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.IDENTITY_MISMATCH);
    }

    @Test
    void businessPlanContainsNoJpaEntities() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/model/PersonnelPlan.java"
        )) + "\n" + Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/model/PersonnelPlannedMutation.java"
        )) + "\n" + Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/model/PersonnelCurrentState.java"
        ));

        assertThat(source)
            .doesNotContain("@Entity")
            .doesNotContain("jakarta.persistence")
            .doesNotContain("Jpa")
            .doesNotContain("Repository");
    }

    private PersonnelBusinessIntent createIntent(String externalIdGuard) {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            externalIdGuard,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            false,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }

    private PersonnelBusinessIntent intent(String externalIdGuard) {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            externalIdGuard,
            true,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }
}
