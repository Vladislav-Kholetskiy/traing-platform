package com.vladislav.training.platform.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Фиксирует словарь и смысловые границы вокруг {@code CapabilityAdmission}.
 * Это помогает не расползтись именам и договорённостям.
 */
class CapabilityAdmissionVocabularyLockTest {

    private static final List<String> MANDATORY_WAVE_SIX_OPERATION_CODES = List.of(
        "NOTIFICATION_RULE_CREATE",
        "NOTIFICATION_RULE_UPDATE",
        "NOTIFICATION_RULE_ENABLE",
        "NOTIFICATION_RULE_DISABLE",
        "IMPORT_JOB_LAUNCH"
    );

    private static final List<String> MANDATORY_WAVE_SIX_TARGET_ENTITY_TYPES = List.of(
        "NOTIFICATION_RULE",
        "IMPORT_JOB"
    );

    private static final List<String> FORBIDDEN_OPERATION_SHORTCUTS = List.of(
        "AUDIT_EVENT_MUTATE",
        "AUDIT_EVENT_CREATE",
        "AUDIT_EVENT_UPDATE",
        "GENERIC_ADMIN_COMMAND",
        "OPERATIONAL_ADMIN_COMMAND",
        "TABLE_MUTATE",
        "OWNER_TABLE_PATCH",
        "IMPORT_OWNER_TABLE_PATCH"
    );

    private static final List<String> FORBIDDEN_TARGET_ENTITY_TYPES = List.of(
        "AUDIT_EVENT",
        "GENERIC_ADMIN",
        "OPERATIONAL_TABLE",
        "DATABASE_TABLE",
        "OWNER_TABLE"
    );

    @Test
    void mandatoryAdministrativeOperationCodesMustExistAsDedicatedEnumValues() {
        MANDATORY_WAVE_SIX_OPERATION_CODES.forEach(operationName ->
            assertThatCode(() -> CapabilityOperationCode.valueOf(operationName))
                
                .doesNotThrowAnyException()
        );
    }

    @Test
    void capabilityOperationCodesMustExposeExactStringMirrorsForAdministrativeOperations() {
        MANDATORY_WAVE_SIX_OPERATION_CODES.forEach(operationName -> {
            assertThatCode(() -> CapabilityOperationCodes.class.getDeclaredField(operationName))
                
                .doesNotThrowAnyException();

            try {
                Field field = CapabilityOperationCodes.class.getDeclaredField(operationName);
                Object fieldValue = field.get(null);

                assertThat(fieldValue)
                    
                    .isEqualTo(operationName);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Failed to inspect CapabilityOperationCodes." + operationName, e);
            }
        });
    }

    @Test
    void mandatoryAdministrativeTargetEntityTypesMustExistAsDedicatedEnumValues() {
        MANDATORY_WAVE_SIX_TARGET_ENTITY_TYPES.forEach(targetTypeName ->
            assertThatCode(() -> CapabilityTargetEntityType.valueOf(targetTypeName))
                
                .doesNotThrowAnyException()
        );
    }

    @Test
    void commandVocabularyMustNotDriftIntoGenericOrForbiddenOperationShortcuts() {
        List<String> operationNames = Arrays.stream(CapabilityOperationCode.values())
            .map(Enum::name)
            .toList();

        assertThat(operationNames)
            
            .doesNotContainAnyElementsOf(FORBIDDEN_OPERATION_SHORTCUTS);
    }

    @Test
    void commandVocabularyMustNotDriftIntoForbiddenTargetEntityTypes() {
        List<String> targetTypeNames = Arrays.stream(CapabilityTargetEntityType.values())
            .map(Enum::name)
            .toList();

        assertThat(targetTypeNames)
            
            .doesNotContainAnyElementsOf(FORBIDDEN_TARGET_ENTITY_TYPES);
    }
}
