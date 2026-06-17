package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRow;
import com.vladislav.training.platform.integration.personnel.model.PersonnelTemporaryAppointmentIntent;
import com.vladislav.training.platform.integration.personnel.service.PersonnelPositionMappingService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelRowInterpreter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelRowInterpreter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelRowInterpreterTest {

    private final PersonnelRowInterpreter interpreter =
        new PersonnelRowInterpreter(new PersonnelPositionMappingService());

    @Test
    void employeeNumberIsMandatory() {
        assertThatThrownBy(() -> interpreter.interpret(row("", "ACTIVE", "DEV", null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("employeeNumber");
    }

    @Test
    void externalIdIsOnlyOptionalConsistencyGuardAndDoesNotReplaceEmployeeNumber() {
        PersonnelBusinessIntent intent = interpreter.interpret(row("1001", "ACTIVE", "DEV", null, null, null, null));

        assertThat(intent.externalIdConsistencyGuard()).isNull();
        assertThat(intent.employeeNumber()).isEqualTo("1001");
        assertThatThrownBy(() -> interpreter.interpret(row(" ", "ACTIVE", "DEV", null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("employeeNumber");
    }

    @Test
    void createCapableActiveRowDoesNotRequireExistingEmployeeButStillDoesNotAddCreateEmploymentAction() {
        PersonnelBusinessIntent intent = interpreter.interpret(row("1001", "ACTIVE", "DEV", null, null, null, null));

        assertThat(intent.requiresExistingEmployee()).isFalse();
        assertThat(intent.employmentAction()).isEqualTo(PersonnelEmploymentAction.ENSURE_ACTIVE);
        assertThat(PersonnelEmploymentAction.values())
            .extracting(Enum::name)
            .doesNotContain("CREATE", "CREATE_USER", "UPSERT");
    }

    @Test
    void activeBecomesBaseActivePersonnelIntent() {
        PersonnelBusinessIntent intent = interpreter.interpret(row("1001", "ACTIVE", "DEV", null, null, null, null));

        assertThat(intent.employmentAction()).isEqualTo(PersonnelEmploymentAction.ENSURE_ACTIVE);
        assertThat(intent.futureTargetUserStatus()).isEqualTo("ACTIVE");
        assertThat(intent.basePositionMapping().positionCode()).isEqualTo("DEV");
    }

    @Test
    void inactiveBecomesDeactivationStyleIntent() {
        PersonnelBusinessIntent intent = interpreter.interpret(row("1001", "INACTIVE", "DEV", null, null, null, null));

        assertThat(intent.employmentAction()).isEqualTo(PersonnelEmploymentAction.DEACTIVATE);
        assertThat(intent.futureTargetUserStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void dismissedIsExcelInputOnlyAndMapsToDismissalDeactivationIntentEndingInInactive() {
        PersonnelBusinessIntent intent = interpreter.interpret(row("1001", "DISMISSED", "DEV", null, null, null, null));

        assertThat(intent.employmentAction()).isEqualTo(PersonnelEmploymentAction.DISMISS_TO_INACTIVE);
        assertThat(intent.futureTargetUserStatus()).isEqualTo("INACTIVE");
        assertThat(intent.futureTargetUserStatus()).isNotEqualTo("DISMISSED");
    }

    @Test
    void unknownEmploymentStatusFailsClosed() {
        assertThatThrownBy(() -> interpreter.interpret(row("1001", "UNKNOWN", "DEV", null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("employmentStatus");
    }

    @Test
    void unknownBasePositionCodeFailsClosed() {
        assertThatThrownBy(() -> interpreter.interpret(row("1001", "ACTIVE", "UNKNOWN", null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("basePositionCode");
    }

    @Test
    void temporaryBlockAbsentMeansNoTemporaryAppointmentIntent() {
        PersonnelBusinessIntent intent = interpreter.interpret(row("1001", "ACTIVE", "DEV", null, null, null, null));

        assertThat(intent.temporaryAppointmentIntent()).isNull();
    }

    @Test
    void partialTemporaryBlockFailsClosed() {
        assertThatThrownBy(() -> interpreter.interpret(
            row("1001", "ACTIVE", "DEV", "ACTING_HEAD", null, null, null)
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Temporary appointment block");
    }

    @Test
    void knownTemporaryManagerialPositionMapsToTemporaryManagementDelegationRequirement() {
        PersonnelBusinessIntent intent = interpreter.interpret(
            row("1001", "ACTIVE", "DEV", "ACTING_HEAD", "BR1", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))
        );

        PersonnelTemporaryAppointmentIntent temporaryIntent = intent.temporaryAppointmentIntent();
        assertThat(temporaryIntent).isNotNull();
        assertThat(temporaryIntent.positionMapping().temporaryManagementDelegationRequired()).isTrue();
        assertThat(temporaryIntent.orgUnitCode()).isEqualTo("BR1");
    }

    @Test
    void temporaryAppointmentUsesOnlyAdditiveV1Semantics() {
        PersonnelBusinessIntent intent = interpreter.interpret(
            row("1001", "ACTIVE", "DEV", "ACTING_HEAD", "BR1", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))
        );

        assertThat(intent.temporaryAppointmentIntent().additiveOnly()).isTrue();
    }

    @Test
    void unsupportedMultiAppointmentSemanticsFailClosed() {
        assertThatThrownBy(() -> interpreter.interpret(
            row("1001", "ACTIVE", "DEV", "DUAL_APPOINTMENT", "BR1", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("multi-appointment");
    }

    @Test
    void interpreterLayerHasNoForbiddenInfrastructureDependencies() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelRowInterpreter.java"
        )) + "\n" + Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelPositionMappingService.java"
        ));

        assertThat(source)
            .doesNotContain("Repository")
            .doesNotContain("Jpa")
            .doesNotContain("Entity")
            .doesNotContain("Policy")
            .doesNotContain("Audit")
            .doesNotContain("Notification")
            .doesNotContain("ImportJob")
            .doesNotContain("Maintenance")
            .doesNotContain("UserAdministrationCommandService")
            .doesNotContain("AccessAdministrationCommandService")
            .doesNotContain("ImportTypedOwnerCommandExecutor");
    }

    @Test
    void businessIntentContainsNoJpaEntities() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/model/PersonnelBusinessIntent.java"
        )) + "\n" + Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/model/PersonnelTemporaryAppointmentIntent.java"
        )) + "\n" + Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/model/PersonnelPositionMapping.java"
        ));

        assertThat(source)
            .doesNotContain("@Entity")
            .doesNotContain("jakarta.persistence")
            .doesNotContain("Jpa")
            .doesNotContain("Repository");
    }

    private PersonnelRow row(
        String employeeNumber,
        String employmentStatus,
        String basePositionCode,
        String temporaryPositionCode,
        String temporaryOrgUnitCode,
        LocalDate temporaryValidFrom,
        LocalDate temporaryValidTo
    ) {
        return new PersonnelRow(
            2,
            employeeNumber,
            "",
            "Ivanov",
            "Ivan",
            "Ivanovich",
            employmentStatus,
            "HQ",
            basePositionCode,
            temporaryPositionCode == null ? "" : temporaryPositionCode,
            temporaryOrgUnitCode == null ? "" : temporaryOrgUnitCode,
            temporaryValidFrom,
            temporaryValidTo,
            "comment"
        );
    }
}
