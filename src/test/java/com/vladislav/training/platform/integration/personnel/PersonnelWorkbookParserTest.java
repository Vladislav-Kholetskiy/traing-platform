package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.integration.personnel.model.PersonnelRow;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRowIssue;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRowOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelWorkbookParseResult;
import com.vladislav.training.platform.integration.personnel.service.ApachePoiPersonnelWorkbookParser;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookParser;
import com.vladislav.training.platform.integration.personnel.support.PersonnelWorkbookLimits;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelWorkbookParser}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelWorkbookParserTest {

    private static final List<String> HEADERS = List.of(
        "employeeNumber",
        "externalId",
        "lastName",
        "firstName",
        "middleName",
        "employmentStatus",
        "homeOrgUnitCode",
        "basePositionCode",
        "temporaryPositionCode",
        "temporaryOrgUnitCode",
        "temporaryValidFrom",
        "temporaryValidTo",
        "comment"
    );

    private final PersonnelWorkbookParser parser =
        new ApachePoiPersonnelWorkbookParser(new PersonnelWorkbookLimits(1000));

    @Test
    void validWorkbookParsesRows() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            writeRow(sheet, 1, "1001", "", "Ivanov", "Ivan", "Ivanovich", "ACTIVE", "HQ", "DEV", "", "", "", "", "ok");
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isFalse();
        assertThat(result.rows()).hasSize(1);
        PersonnelRow row = result.rows().getFirst();
        assertThat(row.rowNumber()).isEqualTo(2);
        assertThat(row.employeeNumber()).isEqualTo("1001");
        assertThat(row.externalId()).isBlank();
        assertThat(row.employmentStatus()).isEqualTo("ACTIVE");
        assertThat(row.temporaryValidFrom()).isNull();
        assertThat(row.temporaryValidTo()).isNull();
    }

    @Test
    void workbookMayOmitOptionalExternalIdHeader() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS.stream().filter(header -> !"externalId".equals(header)).toList());
            writeRowWithoutExternalId(sheet, 1, "1001", "Ivanov", "Ivan", "Ivanovich", "ACTIVE", "HQ", "DEV", "", "", "", "", "");
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isFalse();
        assertThat(result.rows()).singleElement().satisfies(row -> assertThat(row.externalId()).isBlank());
    }

    @Test
    void duplicateHeaderFails() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, List.of(
                "employeeNumber",
                "externalId",
                "lastName",
                "firstName",
                "middleName",
                "employmentStatus",
                "homeOrgUnitCode",
                "basePositionCode",
                "employeeNumber",
                "temporaryOrgUnitCode",
                "temporaryValidFrom",
                "temporaryValidTo",
                "comment"
            ));
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isTrue();
        assertThat(result.issues())
            .extracting(PersonnelRowIssue::outcomeCode)
            .contains(PersonnelRowOutcomeCode.DUPLICATE_HEADER);
    }

    @Test
    void duplicateEmployeeNumberFails() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            writeRow(sheet, 1, "1001", "", "Ivanov", "Ivan", "Ivanovich", "ACTIVE", "HQ", "DEV", "", "", "", "", "");
            writeRow(sheet, 2, "1001", "", "Petrov", "Petr", "Petrovich", "INACTIVE", "HQ", "QA", "", "", "", "", "");
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isTrue();
        assertThat(result.issues())
            .extracting(PersonnelRowIssue::outcomeCode)
            .contains(PersonnelRowOutcomeCode.DUPLICATE_EMPLOYEE_NUMBER);
    }

    @Test
    void formulaCellFails() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("1001");
            row.createCell(1).setCellValue("");
            row.createCell(2).setCellValue("Ivanov");
            row.createCell(3).setCellValue("Ivan");
            row.createCell(4).setCellValue("Ivanovich");
            row.createCell(5).setCellFormula("\"ACTIVE\"");
            row.createCell(6).setCellValue("HQ");
            row.createCell(7).setCellValue("DEV");
            row.createCell(8).setBlank();
            row.createCell(9).setBlank();
            row.createCell(10).setBlank();
            row.createCell(11).setBlank();
            row.createCell(12).setCellValue("");
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isTrue();
        assertThat(result.issues())
            .extracting(PersonnelRowIssue::outcomeCode)
            .contains(PersonnelRowOutcomeCode.FORMULA_CELL_NOT_ALLOWED);
    }

    @Test
    void localizedFreeFormDateFails() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            writeRow(sheet, 1, "1001", "", "Ivanov", "Ivan", "Ivanovich", "ACTIVE", "HQ", "DEV", "LEAD", "BR1", "11 мая 2026", "2026-05-20", "");
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isFalse();
        assertThat(result.issues())
            .extracting(PersonnelRowIssue::outcomeCode)
            .contains(PersonnelRowOutcomeCode.INVALID_DATE_VALUE);
    }

    @Test
    void emptyTrailingRowsIgnored() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            writeRow(sheet, 1, "1001", "", "Ivanov", "Ivan", "Ivanovich", "ACTIVE", "HQ", "DEV", "", "", "", "", "");
            sheet.createRow(2);
            sheet.createRow(3);
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isFalse();
        assertThat(result.rows()).hasSize(1);
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void partialTemporaryBlockFails() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            writeRow(sheet, 1, "1001", "", "Ivanov", "Ivan", "Ivanovich", "ACTIVE", "HQ", "DEV", "LEAD", "", "", "", "");
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isFalse();
        assertThat(result.issues())
            .extracting(PersonnelRowIssue::outcomeCode)
            .contains(PersonnelRowOutcomeCode.PARTIAL_TEMPORARY_BLOCK);
    }

    @Test
    void unsupportedEmploymentStatusFails() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            writeRow(sheet, 1, "1001", "", "Ivanov", "Ivan", "Ivanovich", "ON_LEAVE", "HQ", "DEV", "", "", "", "", "");
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isFalse();
        assertThat(result.issues())
            .extracting(PersonnelRowIssue::outcomeCode)
            .contains(PersonnelRowOutcomeCode.UNSUPPORTED_EMPLOYMENT_STATUS);
    }

    @Test
    void activeInactiveAndDismissedAreAcceptedAsInputVocabulary() throws Exception {
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            writeRow(sheet, 1, "1001", "", "Ivanov", "Ivan", "Ivanovich", "ACTIVE", "HQ", "DEV", "", "", "", "", "");
            writeRow(sheet, 2, "1002", "", "Petrov", "Petr", "Petrovich", "INACTIVE", "HQ", "QA", "", "", "", "", "");
            writeRow(sheet, 3, "1003", "", "Sidorov", "Sidr", "Sidorovich", "DISMISSED", "HQ", "OPS", "", "", "", "", "");
        });

        PersonnelWorkbookParseResult result = parser.parse(workbook);

        assertThat(result.hasFatalIssues()).isFalse();
        assertThat(result.issues()).isEmpty();
        assertThat(result.rows())
            .extracting(PersonnelRow::employmentStatus)
            .containsExactly("ACTIVE", "INACTIVE", "DISMISSED");
    }

    @Test
    void parserLayerHasNoForbiddenInfrastructureDependencies() throws Exception {
        List<Path> parserLayerPaths = List.of(
            Path.of("src/main/java/com/vladislav/training/platform/integration/personnel/model"),
            Path.of("src/main/java/com/vladislav/training/platform/integration/personnel/support"),
            Path.of("src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelWorkbookParser.java"),
            Path.of("src/main/java/com/vladislav/training/platform/integration/personnel/service/ApachePoiPersonnelWorkbookParser.java")
        );

        String source = parserLayerPaths.stream()
            .flatMap(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        return Files.walk(path).filter(Files::isRegularFile);
                    }
                    return java.util.stream.Stream.of(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .distinct()
            .map(path -> {
                try {
                    return Files.readString(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .reduce("", (left, right) -> left + "\n" + right);

        assertThat(source)
            .doesNotContain("Repository")
            .doesNotContain("Jpa")
            .doesNotContain("@Entity")
            .doesNotContain("jakarta.persistence")
            .doesNotContain("CapabilityAdmission")
            .doesNotContain("Policy")
            .doesNotContain("UserCommandService")
            .doesNotContain("UserAdministrationCommandService")
            .doesNotContain("AccessAdministrationCommandService")
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportTypedOwnerCommandExecutor")
            .doesNotContain("Audit")
            .doesNotContain("Notification")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance")
            .doesNotContain("userorg")
            .doesNotContain("access.service")
            .doesNotContain("import_job")
            .doesNotContain("import_job_item")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(");
    }

    @Test
    void rowCountLimitEnforced() throws Exception {
        PersonnelWorkbookParser limitedParser =
            new ApachePoiPersonnelWorkbookParser(new PersonnelWorkbookLimits(1));
        byte[] workbook = workbookBytes(sheet -> {
            writeHeaders(sheet, HEADERS);
            writeRow(sheet, 1, "1001", "", "Ivanov", "Ivan", "Ivanovich", "ACTIVE", "HQ", "DEV", "", "", "", "", "");
            writeRow(sheet, 2, "1002", "", "Petrov", "Petr", "Petrovich", "ACTIVE", "HQ", "QA", "", "", "", "", "");
        });

        PersonnelWorkbookParseResult result = limitedParser.parse(workbook);

        assertThat(result.hasFatalIssues()).isTrue();
        assertThat(result.issues())
            .extracting(PersonnelRowIssue::outcomeCode)
            .contains(PersonnelRowOutcomeCode.ROW_COUNT_LIMIT_EXCEEDED);
    }

    private byte[] workbookBytes(WorkbookSpec spec) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("personnel");
            spec.accept(sheet);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void writeHeaders(Sheet sheet, List<String> headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            header.createCell(i).setCellValue(headers.get(i));
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private void writeRowWithoutExternalId(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    @FunctionalInterface
    private interface WorkbookSpec {
        void accept(Sheet sheet) throws IOException;
    }
}
