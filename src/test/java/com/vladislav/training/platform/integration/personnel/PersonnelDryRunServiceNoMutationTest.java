package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.service.PersonnelChangePlanner;
import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelDryRunService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code PersonnelDryRunServiceNoMutation}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelDryRunServiceNoMutationTest {

    @Mock
    private PersonnelCurrentStateReader currentStateReader;

    @Test
    void dryRunServiceDoesNotMutateOwnerState() {
        PersonnelDryRunService service = new PersonnelDryRunService(currentStateReader, new PersonnelChangePlanner());
        PersonnelBusinessIntent intent = activeIntent();
        when(currentStateReader.resolveIdentity(intent)).thenReturn(PersonnelIdentityResolution.resolved(currentState()));

        List<PersonnelPlan> plans = service.plan(List.of(intent));

        assertThat(plans).hasSize(1);
        verify(currentStateReader, only()).resolveIdentity(intent);
    }

    @Test
    void dryRunServiceDoesNotCreateImportJobOrItemAndDoesNotCallOwnerCommands() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelDryRunService.java"
        ));

        assertThat(source)
            .doesNotContain("ImportCommandService")
            .doesNotContain("import_job")
            .doesNotContain("import_job_item")
            .doesNotContain("UserAdministrationCommandService")
            .doesNotContain("AccessAdministrationCommandService")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(");
    }

    @Test
    void dryRunServiceDoesNotWriteAuditSuccessOrEmitNotificationOrUseMaintenance() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelDryRunService.java"
        ));

        assertThat(source)
            .doesNotContain("Audit")
            .doesNotContain("Notification")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance");
    }

    @Test
    void dryRunServiceReadsOnlyIdentitySeamAndNothingElse() {
        PersonnelDryRunService service = new PersonnelDryRunService(currentStateReader, new PersonnelChangePlanner());
        PersonnelBusinessIntent intent = activeIntent();
        when(currentStateReader.resolveIdentity(intent)).thenReturn(PersonnelIdentityResolution.unresolved("1001"));

        service.plan(List.of(intent));

        verify(currentStateReader).resolveIdentity(intent);
        verifyNoMoreInteractions(currentStateReader);
    }

    private PersonnelBusinessIntent activeIntent() {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            true,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }

    private PersonnelCurrentState currentState() {
        return new PersonnelCurrentState(
            "1001",
            "ext-1",
            "ACTIVE",
            "HQ",
            Set.of("ROLE_USER"),
            false,
            Set.of("SELF"),
            Set.of(),
            Set.of(),
            false
        );
    }
}
