package com.vladislav.training.platform.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code CapabilityAdmissionPersonnelVocabularyStage1Lock}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class CapabilityAdmissionPersonnelVocabularyStage1LockTest {

    @Test
    void personnelOperationCodesMustExistAsDedicatedEnumValues() {
        assertThatCode(() -> CapabilityOperationCode.valueOf("PERSONNEL_EXCEL_DRY_RUN")).doesNotThrowAnyException();
        assertThatCode(() -> CapabilityOperationCode.valueOf("PERSONNEL_EXCEL_APPLY")).doesNotThrowAnyException();
    }

    @Test
    void personnelOperationConstantsMustMirrorCanonicalEnumNames() throws Exception {
        Field dryRunField = CapabilityOperationCodes.class.getDeclaredField("PERSONNEL_EXCEL_DRY_RUN");
        Field applyField = CapabilityOperationCodes.class.getDeclaredField("PERSONNEL_EXCEL_APPLY");

        assertThat(dryRunField.get(null)).isEqualTo("PERSONNEL_EXCEL_DRY_RUN");
        assertThat(applyField.get(null)).isEqualTo("PERSONNEL_EXCEL_APPLY");
    }

    @Test
    void personnelTargetTypeMustExistAsDedicatedEnumValue() {
        assertThatCode(() -> CapabilityTargetEntityType.valueOf("PERSONNEL_EXCEL_IMPORT")).doesNotThrowAnyException();
    }
}
