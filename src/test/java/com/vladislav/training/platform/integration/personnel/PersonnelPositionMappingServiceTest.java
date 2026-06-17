package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.service.PersonnelPositionMappingService;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение сервиса {@code PersonnelPositionMapping}.
 * Сценарии сосредоточены на прикладной логике.
 */
class PersonnelPositionMappingServiceTest {

    private final PersonnelPositionMappingService mappingService = new PersonnelPositionMappingService();

    @Test
    void knownBasePositionMapsToRequiredRoleAccessAndManagementSemantics() {
        PersonnelPositionMapping mapping = mappingService.requireBasePosition("HEAD");

        assertThat(mapping.positionCode()).isEqualTo("HEAD");
        assertThat(mapping.roleCodes()).contains("ROLE_MANAGER", "ROLE_USER");
        assertThat(mapping.managementRelationRequired()).isTrue();
        assertThat(mapping.accessScopeRequired()).isEqualTo("UNIT");
        assertThat(mapping.temporaryManagementDelegationRequired()).isFalse();
        assertThat(mapping.additiveOnly()).isTrue();
    }

    @Test
    void knownTemporaryManagerialPositionMapsToTemporaryManagementDelegationRequirement() {
        PersonnelPositionMapping mapping = mappingService.requireTemporaryPosition("ACTING_HEAD");

        assertThat(mapping.positionCode()).isEqualTo("ACTING_HEAD");
        assertThat(mapping.roleCodes()).contains("ROLE_MANAGER");
        assertThat(mapping.temporaryManagementDelegationRequired()).isTrue();
        assertThat(mapping.managementRelationRequired()).isFalse();
        assertThat(mapping.additiveOnly()).isTrue();
    }

    @Test
    void unknownBasePositionFailsClosed() {
        assertThatThrownBy(() -> mappingService.requireBasePosition("UNKNOWN"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported basePositionCode");
    }

    @Test
    void unsupportedMultiAppointmentSemanticsFailClosed() {
        assertThatThrownBy(() -> mappingService.requireTemporaryPosition("DUAL_APPOINTMENT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("multi-appointment")
            .hasMessageContaining("DUAL_APPOINTMENT");
    }
}
