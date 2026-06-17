package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import java.util.Map;
import java.util.Set;

/**
 * Контракт сервиса {@code PersonnelPositionMappingService}.
 */
public class PersonnelPositionMappingService {

    private static final Map<String, PersonnelPositionMapping> BASE_MAPPINGS = Map.of(
        "DEV", new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
        "HEAD", new PersonnelPositionMapping("HEAD", Set.of("ROLE_USER", "ROLE_MANAGER"), true, "UNIT", false, true),
        "OPS", new PersonnelPositionMapping("OPS", Set.of("ROLE_USER", "ROLE_OPERATIONS"), false, "UNIT", false, true)
    );

    private static final Map<String, PersonnelPositionMapping> TEMPORARY_MAPPINGS = Map.of(
        "ACTING_HEAD", new PersonnelPositionMapping("ACTING_HEAD", Set.of("ROLE_MANAGER"), false, "UNIT", true, true),
        "ACTING_OPS", new PersonnelPositionMapping("ACTING_OPS", Set.of("ROLE_OPERATIONS"), false, "UNIT", false, true)
    );

    public PersonnelPositionMapping requireBasePosition(String basePositionCode) {
        return requireKnownMapping(basePositionCode, BASE_MAPPINGS, "basePositionCode");
    }

    public PersonnelPositionMapping requireTemporaryPosition(String temporaryPositionCode) {
        if ("DUAL_APPOINTMENT".equals(temporaryPositionCode)) {
            throw new IllegalArgumentException(
                "Unsupported multi-appointment semantics for temporaryPositionCode: " + temporaryPositionCode
            );
        }
        return requireKnownMapping(temporaryPositionCode, TEMPORARY_MAPPINGS, "temporaryPositionCode");
    }

    private PersonnelPositionMapping requireKnownMapping(
        String positionCode,
        Map<String, PersonnelPositionMapping> mappings,
        String fieldName
    ) {
        if (positionCode == null || positionCode.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        PersonnelPositionMapping mapping = mappings.get(positionCode);
        if (mapping == null) {
            throw new IllegalArgumentException("Unsupported " + fieldName + ": " + positionCode);
        }
        return mapping;
    }
}
