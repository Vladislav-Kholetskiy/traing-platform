package com.vladislav.training.platform.integration.personnel.support;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Перечисление {@code PersonnelExcelColumn}.
 */
public enum PersonnelExcelColumn {
    EMPLOYEE_NUMBER("employeeNumber"),
    EXTERNAL_ID("externalId"),
    LAST_NAME("lastName"),
    FIRST_NAME("firstName"),
    MIDDLE_NAME("middleName"),
    EMPLOYMENT_STATUS("employmentStatus"),
    HOME_ORG_UNIT_CODE("homeOrgUnitCode"),
    BASE_POSITION_CODE("basePositionCode"),
    TEMPORARY_POSITION_CODE("temporaryPositionCode"),
    TEMPORARY_ORG_UNIT_CODE("temporaryOrgUnitCode"),
    TEMPORARY_VALID_FROM("temporaryValidFrom"),
    TEMPORARY_VALID_TO("temporaryValidTo"),
    COMMENT("comment");

    private final String headerName;

    PersonnelExcelColumn(String headerName) {
        this.headerName = headerName;
    }

    public String headerName() {
        return headerName;
    }

    public static List<String> requiredHeaders() {
        return List.of(
            EMPLOYEE_NUMBER.headerName(),
            LAST_NAME.headerName(),
            FIRST_NAME.headerName(),
            MIDDLE_NAME.headerName(),
            EMPLOYMENT_STATUS.headerName(),
            HOME_ORG_UNIT_CODE.headerName(),
            BASE_POSITION_CODE.headerName(),
            TEMPORARY_POSITION_CODE.headerName(),
            TEMPORARY_ORG_UNIT_CODE.headerName(),
            TEMPORARY_VALID_FROM.headerName(),
            TEMPORARY_VALID_TO.headerName(),
            COMMENT.headerName()
        );
    }

    public static Optional<PersonnelExcelColumn> fromHeader(String headerName) {
        return Arrays.stream(values()).filter(column -> column.headerName.equals(headerName)).findFirst();
    }
}
