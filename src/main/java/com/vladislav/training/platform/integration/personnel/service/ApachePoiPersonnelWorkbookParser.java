package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.integration.personnel.model.PersonnelRow;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRowIssue;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRowOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelWorkbookParseResult;
import com.vladislav.training.platform.integration.personnel.support.PersonnelDateNormalizer;
import com.vladislav.training.platform.integration.personnel.support.PersonnelExcelColumn;
import com.vladislav.training.platform.integration.personnel.support.PersonnelWorkbookLimits;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Класс {@code ApachePoiPersonnelWorkbookParser}.
 */
public class ApachePoiPersonnelWorkbookParser implements PersonnelWorkbookParser {

    private static final Set<String> SUPPORTED_EMPLOYMENT_STATUSES = Set.of("ACTIVE", "INACTIVE", "DISMISSED");

    private final PersonnelWorkbookLimits workbookLimits;
    private final PersonnelDateNormalizer dateNormalizer;
    private final DataFormatter dataFormatter = new DataFormatter();

    public ApachePoiPersonnelWorkbookParser(PersonnelWorkbookLimits workbookLimits) {
        this(workbookLimits, new PersonnelDateNormalizer());
    }

    public ApachePoiPersonnelWorkbookParser(
        PersonnelWorkbookLimits workbookLimits,
        PersonnelDateNormalizer dateNormalizer
    ) {
        this.workbookLimits = Objects.requireNonNull(workbookLimits, "workbookLimits must not be null");
        this.dateNormalizer = Objects.requireNonNull(dateNormalizer, "dateNormalizer must not be null");
    }

    @Override
    public PersonnelWorkbookParseResult parse(byte[] workbookBytes) {
        if (workbookBytes == null || workbookBytes.length == 0) {
            throw new IllegalArgumentException("workbookBytes must not be empty");
        }

        List<PersonnelRow> rows = new ArrayList<>();
        List<PersonnelRowIssue> issues = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            List<Sheet> businessSheets = visibleSheets(workbook);
            if (businessSheets.size() != 1) {
                issues.add(new PersonnelRowIssue(
                    null,
                    null,
                    PersonnelRowOutcomeCode.BUSINESS_SHEET_COUNT_INVALID,
                    "Workbook must contain exactly one visible business sheet"
                ));
                return new PersonnelWorkbookParseResult(rows, issues);
            }

            Sheet sheet = businessSheets.getFirst();
            HeaderLayout headerLayout = parseHeader(sheet, issues);
            if (headerLayout == null) {
                return new PersonnelWorkbookParseResult(rows, issues);
            }

            Set<String> employeeNumbers = new HashSet<>();
            int nonEmptyRowCount = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row, headerLayout.columnsByIndex())) {
                    continue;
                }
                nonEmptyRowCount++;
                if (nonEmptyRowCount > workbookLimits.maxRows()) {
                    issues.add(new PersonnelRowIssue(
                        rowIndex + 1,
                        null,
                        PersonnelRowOutcomeCode.ROW_COUNT_LIMIT_EXCEEDED,
                        "Workbook row count exceeds configured limit of " + workbookLimits.maxRows()
                    ));
                    return new PersonnelWorkbookParseResult(rows, issues);
                }

                PersonnelRow parsedRow = parseDataRow(row, headerLayout, issues);
                if (parsedRow == null) {
                    continue;
                }
                if (!employeeNumbers.add(parsedRow.employeeNumber())) {
                    issues.add(new PersonnelRowIssue(
                        parsedRow.rowNumber(),
                        PersonnelExcelColumn.EMPLOYEE_NUMBER.headerName(),
                        PersonnelRowOutcomeCode.DUPLICATE_EMPLOYEE_NUMBER,
                        "Duplicate employeeNumber: " + parsedRow.employeeNumber()
                    ));
                    return new PersonnelWorkbookParseResult(rows, issues);
                }
                rows.add(parsedRow);
            }

            return new PersonnelWorkbookParseResult(rows, issues);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse personnel workbook", e);
        }
    }

    private List<Sheet> visibleSheets(Workbook workbook) {
        List<Sheet> sheets = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (!workbook.isSheetHidden(i) && !workbook.isSheetVeryHidden(i)) {
                sheets.add(workbook.getSheetAt(i));
            }
        }
        return sheets;
    }

    private HeaderLayout parseHeader(Sheet sheet, List<PersonnelRowIssue> issues) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            issues.add(new PersonnelRowIssue(
                1,
                null,
                PersonnelRowOutcomeCode.MISSING_REQUIRED_HEADER,
                "Header row is missing"
            ));
            return null;
        }

        Map<Integer, PersonnelExcelColumn> columnsByIndex = new HashMap<>();
        Set<String> seenHeaders = new HashSet<>();
        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex);
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                continue;
            }
            if (cell.getCellType() == CellType.FORMULA) {
                issues.add(new PersonnelRowIssue(
                    1,
                    null,
                    PersonnelRowOutcomeCode.FORMULA_CELL_NOT_ALLOWED,
                    "Formula header cells are not allowed"
                ));
                return null;
            }

            String headerName = dataFormatter.formatCellValue(cell).trim();
            if (!seenHeaders.add(headerName)) {
                issues.add(new PersonnelRowIssue(
                    1,
                    headerName,
                    PersonnelRowOutcomeCode.DUPLICATE_HEADER,
                    "Duplicate header: " + headerName
                ));
                return null;
            }

            PersonnelExcelColumn column = PersonnelExcelColumn.fromHeader(headerName).orElse(null);
            if (column == null) {
                issues.add(new PersonnelRowIssue(
                    1,
                    headerName,
                    PersonnelRowOutcomeCode.UNEXPECTED_HEADER,
                    "Unexpected header: " + headerName
                ));
                return null;
            }
            columnsByIndex.put(cellIndex, column);
        }

        for (String requiredHeader : PersonnelExcelColumn.requiredHeaders()) {
            if (!seenHeaders.contains(requiredHeader)) {
                issues.add(new PersonnelRowIssue(
                    1,
                    requiredHeader,
                    PersonnelRowOutcomeCode.MISSING_REQUIRED_HEADER,
                    "Missing required header: " + requiredHeader
                ));
            }
        }
        if (!issues.isEmpty()) {
            return null;
        }
        return new HeaderLayout(columnsByIndex);
    }

    private PersonnelRow parseDataRow(Row row, HeaderLayout headerLayout, List<PersonnelRowIssue> issues) {
        Map<PersonnelExcelColumn, String> values = new EnumMap<>(PersonnelExcelColumn.class);
        for (Map.Entry<Integer, PersonnelExcelColumn> entry : headerLayout.columnsByIndex().entrySet()) {
            Cell cell = row.getCell(entry.getKey());
            if (cell != null && cell.getCellType() == CellType.FORMULA) {
                issues.add(new PersonnelRowIssue(
                    row.getRowNum() + 1,
                    entry.getValue().headerName(),
                    PersonnelRowOutcomeCode.FORMULA_CELL_NOT_ALLOWED,
                    "Formula cells are not allowed"
                ));
                return null;
            }
            values.put(entry.getValue(), readCellAsString(cell));
        }

        int issueCountBefore = issues.size();
        String employmentStatus = values.get(PersonnelExcelColumn.EMPLOYMENT_STATUS);
        if (!SUPPORTED_EMPLOYMENT_STATUSES.contains(employmentStatus)) {
            issues.add(new PersonnelRowIssue(
                row.getRowNum() + 1,
                PersonnelExcelColumn.EMPLOYMENT_STATUS.headerName(),
                PersonnelRowOutcomeCode.UNSUPPORTED_EMPLOYMENT_STATUS,
                "Unsupported employmentStatus: " + employmentStatus
            ));
        }

        LocalDate temporaryValidFrom = normalizeDate(row, issues, PersonnelExcelColumn.TEMPORARY_VALID_FROM, headerLayout);
        LocalDate temporaryValidTo = normalizeDate(row, issues, PersonnelExcelColumn.TEMPORARY_VALID_TO, headerLayout);

        boolean hasTemporaryPosition = !values.get(PersonnelExcelColumn.TEMPORARY_POSITION_CODE).isBlank();
        boolean hasTemporaryOrg = !values.get(PersonnelExcelColumn.TEMPORARY_ORG_UNIT_CODE).isBlank();
        boolean hasTemporaryFrom = temporaryValidFrom != null;
        boolean hasTemporaryTo = temporaryValidTo != null;
        int temporaryFieldCount = (hasTemporaryPosition ? 1 : 0)
            + (hasTemporaryOrg ? 1 : 0)
            + (hasTemporaryFrom ? 1 : 0)
            + (hasTemporaryTo ? 1 : 0);
        if (temporaryFieldCount > 0 && temporaryFieldCount < 4) {
            issues.add(new PersonnelRowIssue(
                row.getRowNum() + 1,
                null,
                PersonnelRowOutcomeCode.PARTIAL_TEMPORARY_BLOCK,
                "Temporary appointment block must be either fully filled or fully empty"
            ));
        }

        if (issues.size() > issueCountBefore) {
            return null;
        }

        return new PersonnelRow(
            row.getRowNum() + 1,
            values.get(PersonnelExcelColumn.EMPLOYEE_NUMBER),
            values.getOrDefault(PersonnelExcelColumn.EXTERNAL_ID, ""),
            values.get(PersonnelExcelColumn.LAST_NAME),
            values.get(PersonnelExcelColumn.FIRST_NAME),
            values.get(PersonnelExcelColumn.MIDDLE_NAME),
            employmentStatus,
            values.get(PersonnelExcelColumn.HOME_ORG_UNIT_CODE),
            values.get(PersonnelExcelColumn.BASE_POSITION_CODE),
            values.get(PersonnelExcelColumn.TEMPORARY_POSITION_CODE),
            values.get(PersonnelExcelColumn.TEMPORARY_ORG_UNIT_CODE),
            temporaryValidFrom,
            temporaryValidTo,
            values.get(PersonnelExcelColumn.COMMENT)
        );
    }

    private LocalDate normalizeDate(
        Row row,
        List<PersonnelRowIssue> issues,
        PersonnelExcelColumn column,
        HeaderLayout headerLayout
    ) {
        Integer columnIndex = headerLayout.indexByColumn().get(column);
        Cell cell = columnIndex == null ? null : row.getCell(columnIndex);
        if (cell != null && cell.getCellType() == CellType.FORMULA) {
            issues.add(new PersonnelRowIssue(
                row.getRowNum() + 1,
                column.headerName(),
                PersonnelRowOutcomeCode.FORMULA_CELL_NOT_ALLOWED,
                "Formula cells are not allowed"
            ));
            return null;
        }
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String rawValue = readCellAsString(cell);
        try {
            return dateNormalizer.normalize(rawValue, column.headerName(), row.getRowNum() + 1);
        } catch (IllegalArgumentException e) {
            issues.add(new PersonnelRowIssue(
                row.getRowNum() + 1,
                column.headerName(),
                PersonnelRowOutcomeCode.INVALID_DATE_VALUE,
                e.getMessage()
            ));
            return null;
        }
    }

    private boolean isEmptyRow(Row row, Map<Integer, PersonnelExcelColumn> columnsByIndex) {
        if (row == null) {
            return true;
        }
        for (Integer cellIndex : columnsByIndex.keySet()) {
            String value = readCellAsString(row.getCell(cellIndex));
            if (!value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readCellAsString(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private record HeaderLayout(
        Map<Integer, PersonnelExcelColumn> columnsByIndex
    ) {
        private HeaderLayout {
            columnsByIndex = Map.copyOf(columnsByIndex);
        }

        private Map<PersonnelExcelColumn, Integer> indexByColumn() {
            Map<PersonnelExcelColumn, Integer> index = new EnumMap<>(PersonnelExcelColumn.class);
            for (Map.Entry<Integer, PersonnelExcelColumn> entry : columnsByIndex.entrySet()) {
                index.put(entry.getValue(), entry.getKey());
            }
            return index;
        }
    }
}
