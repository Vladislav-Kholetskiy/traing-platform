package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AnalyticsResultRebuildServiceTechnicalOutcomeReporting}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AnalyticsResultRebuildServiceTechnicalOutcomeReportingTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 result analytics rebuild must report a bounded technical outcome for source rows, supported topic rows, "
            + "unsupported topic-key rows and aggregate rows without using audit as job outcome store, mutating owner facts, "
            + "opening scheduler/public API, or adding live fallback.";

    private static final String REPORTER_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsResultRebuildOutcomeReporter";
    private static final String OUTCOME_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsResultRebuildOutcome";
    private static final String UNSUPPORTED_ROW_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsUnsupportedTopicKeyReportRow";

    private static final Path REPORTER_SOURCE_PATH = Path.of(
        "src",
        "main",
        "java",
        "com",
        "vladislav",
        "training",
        "platform",
        "analytics",
        "service",
        "AnalyticsResultRebuildOutcomeReporter.java"
    );
    private static final Path OUTCOME_SOURCE_PATH = Path.of(
        "src",
        "main",
        "java",
        "com",
        "vladislav",
        "training",
        "platform",
        "analytics",
        "service",
        "AnalyticsResultRebuildOutcome.java"
    );
    private static final Path UNSUPPORTED_ROW_SOURCE_PATH = Path.of(
        "src",
        "main",
        "java",
        "com",
        "vladislav",
        "training",
        "platform",
        "analytics",
        "service",
        "AnalyticsUnsupportedTopicKeyReportRow.java"
    );
    private static final Path REBUILD_SERVICE_SOURCE_PATH = Path.of(
        "src",
        "main",
        "java",
        "com",
        "vladislav",
        "training",
        "platform",
        "analytics",
        "service",
        "AnalyticsResultRebuildServiceImpl.java"
    );

    private static final List<String> FORBIDDEN_MARKERS = List.of(
        "@Service",
        "@Component",
        "@Scheduled",
        "@EnableScheduling",
        "@RestController",
        "@Controller",
        "@RequestMapping",
        "@PostMapping",
        "@GetMapping",
        "Audit",
        "audit",
        "Job",
        "job",
        "ResultEntity",
        "ResultQuestionSnapshotEntity",
        "TestAttemptEntity",
        "AssignmentEntity",
        "QuestionEntity",
        "TopicEntity",
        "CourseEntity",
        "OrganizationalUnitEntity",
        "UserOrganizationAssignmentEntity",
        "AssignmentStatus",
        "TestAttemptStatus",
        "questionRepository",
        "topicRepository",
        "contentRepository",
        "organizationalUnitRepository",
        "assignmentRepository",
        "testAttemptRepository",
        "question.topic_id",
        "getTopicId",
        "test_attempt.status",
        "assignment.status",
        "executionStatus"
    );

    @Test
    void rebuildResultAnalyticsMustReportBoundedTechnicalOutcome() throws Exception {
        List<String> preconditionViolations = new ArrayList<>();

        verifySourceFileExists(REPORTER_SOURCE_PATH, preconditionViolations);
        verifySourceFileExists(OUTCOME_SOURCE_PATH, preconditionViolations);
        verifySourceFileExists(UNSUPPORTED_ROW_SOURCE_PATH, preconditionViolations);
        verifySourceFileExists(REBUILD_SERVICE_SOURCE_PATH, preconditionViolations);
        verifyForbiddenMarkers(REBUILD_SERVICE_SOURCE_PATH, preconditionViolations);
        verifyForbiddenMarkers(REPORTER_SOURCE_PATH, preconditionViolations);
        verifyForbiddenMarkers(OUTCOME_SOURCE_PATH, preconditionViolations);
        verifyForbiddenMarkers(UNSUPPORTED_ROW_SOURCE_PATH, preconditionViolations);

        Class<?> reporterClass = loadClass(REPORTER_CLASS_NAME, preconditionViolations);
        Class<?> outcomeClass = loadClass(OUTCOME_CLASS_NAME, preconditionViolations);
        Class<?> unsupportedRowClass = loadClass(UNSUPPORTED_ROW_CLASS_NAME, preconditionViolations);

        if (reporterClass != null) {
            verifyReporterShape(reporterClass, preconditionViolations);
        }
        if (outcomeClass != null) {
            verifyOutcomeShape(outcomeClass, unsupportedRowClass, preconditionViolations);
        }
        if (unsupportedRowClass != null) {
            verifyUnsupportedRowShape(unsupportedRowClass, preconditionViolations);
        }
        if (reporterClass != null) {
            verifySixArgConstructorExists(reporterClass, preconditionViolations);
        }

        if (!preconditionViolations.isEmpty()) {
            fail(CONTRACT_MESSAGE + System.lineSeparator() + String.join(System.lineSeparator(), preconditionViolations));
        }

        Instant periodStartInclusive = Instant.parse("2026-05-01T00:00:00Z");
        Instant periodEndExclusive = Instant.parse("2026-05-02T00:00:00Z");

        List<AnalyticsQuestionAggregateResultSourceRow> sourceRows = List.of(
            new AnalyticsQuestionAggregateResultSourceRow(
                101L,
                7001L,
                3001L,
                "/company/unit-a",
                "ASSIGNED",
                new BigDecimal("100.0000"),
                true,
                false,
                501L,
                true,
                BigDecimal.ONE,
                BigDecimal.ONE,
                Instant.parse("2026-05-01T10:00:00Z")
            ),
            new AnalyticsQuestionAggregateResultSourceRow(
                102L,
                7002L,
                3002L,
                "/company/unit-b",
                "ASSIGNED",
                BigDecimal.ZERO.setScale(4),
                false,
                false,
                502L,
                false,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                Instant.parse("2026-05-01T10:05:00Z")
            ),
            new AnalyticsQuestionAggregateResultSourceRow(
                103L,
                7003L,
                3003L,
                "/company/unit-c",
                "ASSIGNED",
                new BigDecimal("100.0000"),
                true,
                true,
                503L,
                true,
                BigDecimal.ONE,
                BigDecimal.ONE,
                Instant.parse("2026-05-01T10:10:00Z")
            )
        );

        AnalyticsQuestionAggregateResultSourceReader reader = new FakeResultSourceReader(sourceRows);
        AnalyticsTopicKeyStrategy topicKeyStrategy = new FakeTopicKeyStrategy();
        CapturingUserTopicAggregateWriter userTopicWriter = new CapturingUserTopicAggregateWriter();
        CapturingDepartmentTopicAggregateWriter departmentTopicWriter = new CapturingDepartmentTopicAggregateWriter();
        CapturingQuestionAggregateWriter questionWriter = new CapturingQuestionAggregateWriter();
        CapturingOutcomeReporter reporter = new CapturingOutcomeReporter();

        AnalyticsResultRebuildServiceImpl service = instantiateServiceWithOutcomeReporter(
            reporterClass,
            reader,
            topicKeyStrategy,
            userTopicWriter,
            departmentTopicWriter,
            questionWriter,
            reporter
        );

        service.rebuildResultAnalytics(periodStartInclusive, periodEndExclusive);

        assertThat(reporter.invocationCount)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(1);
        assertThat(reporter.capturedOutcome)
            .withFailMessage(CONTRACT_MESSAGE)
            .isNotNull();

        assertThat(invokeOutcomeAccessor(reporter.capturedOutcome, "periodStartInclusive"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodStartInclusive);
        assertThat(invokeOutcomeAccessor(reporter.capturedOutcome, "periodEndExclusive"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodEndExclusive);
        assertThat(invokeOutcomeAccessor(reporter.capturedOutcome, "sourceRowCount"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(3L);
        assertThat(invokeOutcomeAccessor(reporter.capturedOutcome, "supportedTopicRowCount"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(2L);
        assertThat(invokeOutcomeAccessor(reporter.capturedOutcome, "unsupportedTopicRowCount"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(1L);
        assertThat(invokeOutcomeAccessor(reporter.capturedOutcome, "userTopicAggregateRowCount"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo((long) userTopicWriter.capturedRows.size());
        assertThat(invokeOutcomeAccessor(reporter.capturedOutcome, "departmentTopicAggregateRowCount"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo((long) departmentTopicWriter.capturedRows.size());
        assertThat(invokeOutcomeAccessor(reporter.capturedOutcome, "questionAggregateRowCount"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo((long) questionWriter.capturedRows.size());

        @SuppressWarnings("unchecked")
        List<Object> unsupportedTopicRows = (List<Object>) invokeOutcomeAccessor(
            reporter.capturedOutcome,
            "unsupportedTopicRows"
        );
        assertThat(unsupportedTopicRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .hasSize(1);
        Object unsupportedRow = unsupportedTopicRows.getFirst();
        assertThat(invokeRecordAccessor(unsupportedRow, "resultId"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(103L);
        assertThat(invokeRecordAccessor(unsupportedRow, "questionOriginalId"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(503L);
        assertThat(invokeRecordAccessor(unsupportedRow, "reason"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("unsupported immutable topic anchor");

        assertThat(userTopicWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .allMatch(row -> !Objects.equals(row.topicId(), null));
        assertThat(userTopicWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .noneMatch(row -> Objects.equals(row.userId(), 7003L));
        assertThat(departmentTopicWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .noneMatch(row -> Objects.equals(row.organizationalUnitIdSnapshot(), 3003L));
        assertThat(questionWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .extracting(AnalyticsQuestionAggregateRow::questionId)
            .containsExactlyInAnyOrder(501L, 502L, 503L);
    }

    private static void verifySourceFileExists(Path sourcePath, List<String> violations) {
        if (!Files.exists(sourcePath)) {
            violations.add("Missing production source file: " + sourcePath);
        }
    }

    private static void verifyForbiddenMarkers(Path sourcePath, List<String> violations) throws Exception {
        if (!Files.exists(sourcePath)) {
            return;
        }
        String source = Files.readString(sourcePath);
        for (String marker : FORBIDDEN_MARKERS) {
            if (source.contains(marker)) {
                violations.add(sourcePath + " contains forbidden marker: " + marker);
            }
        }
    }

    private static Class<?> loadClass(String className, List<String> violations) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            violations.add("Missing production type: " + className);
            return null;
        }
    }

    private static void verifyReporterShape(Class<?> reporterClass, List<String> violations) {
        if (!REPORTER_CLASS_NAME.equals(reporterClass.getName())) {
            violations.add("Unexpected reporter class name: " + reporterClass.getName());
        }
        if (!"com.vladislav.training.platform.analytics.service".equals(reporterClass.getPackageName())) {
            violations.add("Reporter must live in analytics.service package");
        }
        if (!Modifier.isPublic(reporterClass.getModifiers())) {
            violations.add("Reporter must be public");
        }
        if (!reporterClass.isInterface()) {
            violations.add("Reporter must be a public interface");
        }
        Method[] methods = reporterClass.getDeclaredMethods();
        if (methods.length != 1) {
            violations.add("Reporter must declare exactly one method");
            return;
        }
        Method method = methods[0];
        if (!"reportResultRebuildOutcome".equals(method.getName())) {
            violations.add("Reporter method name mismatch: " + method.getName());
        }
        if (method.getParameterCount() != 1) {
            violations.add("Reporter method must accept exactly one outcome parameter");
        }
        if (method.getReturnType() != void.class) {
            violations.add("Reporter method must return void");
        }
        if (method.getParameterCount() == 1 && !OUTCOME_CLASS_NAME.equals(method.getParameterTypes()[0].getName())) {
            violations.add("Reporter method must accept AnalyticsResultRebuildOutcome");
        }
    }

    private static void verifyOutcomeShape(
        Class<?> outcomeClass,
        Class<?> unsupportedRowClass,
        List<String> violations
    ) {
        if (!OUTCOME_CLASS_NAME.equals(outcomeClass.getName())) {
            violations.add("Unexpected outcome class name: " + outcomeClass.getName());
        }
        if (!"com.vladislav.training.platform.analytics.service".equals(outcomeClass.getPackageName())) {
            violations.add("Outcome must live in analytics.service package");
        }
        if (!Modifier.isPublic(outcomeClass.getModifiers())) {
            violations.add("Outcome must be public");
        }
        if (!outcomeClass.isRecord()) {
            violations.add("Outcome must be a public record");
            return;
        }

        RecordComponent[] components = outcomeClass.getRecordComponents();
        List<String> expectedNames = List.of(
            "periodStartInclusive",
            "periodEndExclusive",
            "sourceRowCount",
            "supportedTopicRowCount",
            "unsupportedTopicRowCount",
            "userTopicAggregateRowCount",
            "departmentTopicAggregateRowCount",
            "questionAggregateRowCount",
            "unsupportedTopicRows"
        );
        if (components.length != expectedNames.size()) {
            violations.add("Outcome record component count mismatch: " + components.length);
            return;
        }

        for (int index = 0; index < components.length; index++) {
            RecordComponent component = components[index];
            if (!expectedNames.get(index).equals(component.getName())) {
                violations.add("Outcome component name mismatch at index " + index + ": " + component.getName());
            }
        }

        assertType(components[0].getType(), Instant.class, "Outcome periodStartInclusive type mismatch", violations);
        assertType(components[1].getType(), Instant.class, "Outcome periodEndExclusive type mismatch", violations);
        assertType(components[2].getType(), long.class, "Outcome sourceRowCount type mismatch", violations);
        assertType(components[3].getType(), long.class, "Outcome supportedTopicRowCount type mismatch", violations);
        assertType(components[4].getType(), long.class, "Outcome unsupportedTopicRowCount type mismatch", violations);
        assertType(components[5].getType(), long.class, "Outcome userTopicAggregateRowCount type mismatch", violations);
        assertType(components[6].getType(), long.class, "Outcome departmentTopicAggregateRowCount type mismatch", violations);
        assertType(components[7].getType(), long.class, "Outcome questionAggregateRowCount type mismatch", violations);
        assertType(components[8].getType(), List.class, "Outcome unsupportedTopicRows type mismatch", violations);
        if (unsupportedRowClass != null) {
            String genericType = components[8].getGenericSignature();
            if (genericType == null || !genericType.contains(unsupportedRowClass.getSimpleName())) {
                violations.add("Outcome unsupportedTopicRows must be typed to AnalyticsUnsupportedTopicKeyReportRow");
            }
        }
    }

    private static void verifyUnsupportedRowShape(Class<?> unsupportedRowClass, List<String> violations) {
        if (!UNSUPPORTED_ROW_CLASS_NAME.equals(unsupportedRowClass.getName())) {
            violations.add("Unexpected unsupported-row class name: " + unsupportedRowClass.getName());
        }
        if (!"com.vladislav.training.platform.analytics.service".equals(unsupportedRowClass.getPackageName())) {
            violations.add("Unsupported-row type must live in analytics.service package");
        }
        if (!Modifier.isPublic(unsupportedRowClass.getModifiers())) {
            violations.add("Unsupported-row type must be public");
        }
        if (!unsupportedRowClass.isRecord()) {
            violations.add("Unsupported-row type must be a public record");
            return;
        }

        RecordComponent[] components = unsupportedRowClass.getRecordComponents();
        if (components.length != 3) {
            violations.add("Unsupported-row record component count mismatch: " + components.length);
            return;
        }
        assertRecordComponent(components[0], "resultId", Long.class, violations);
        assertRecordComponent(components[1], "questionOriginalId", Long.class, violations);
        assertRecordComponent(components[2], "reason", String.class, violations);
    }

    private static void verifySixArgConstructorExists(Class<?> reporterClass, List<String> violations) {
        try {
            AnalyticsResultRebuildServiceImpl.class.getDeclaredConstructor(
                AnalyticsQuestionAggregateResultSourceReader.class,
                AnalyticsTopicKeyStrategy.class,
                AnalyticsUserTopicAggregateWriter.class,
                AnalyticsDepartmentTopicAggregateWriter.class,
                AnalyticsQuestionAggregateWriter.class,
                reporterClass
            );
        } catch (NoSuchMethodException exception) {
            violations.add(
                "Missing 6-arg AnalyticsResultRebuildServiceImpl constructor with AnalyticsResultRebuildOutcomeReporter"
            );
        }
    }

    private static AnalyticsResultRebuildServiceImpl instantiateServiceWithOutcomeReporter(
        Class<?> reporterClass,
        AnalyticsQuestionAggregateResultSourceReader reader,
        AnalyticsTopicKeyStrategy topicKeyStrategy,
        AnalyticsUserTopicAggregateWriter userTopicWriter,
        AnalyticsDepartmentTopicAggregateWriter departmentTopicWriter,
        AnalyticsQuestionAggregateWriter questionWriter,
        CapturingOutcomeReporter capturingReporter
    ) {
        try {
            Constructor<AnalyticsResultRebuildServiceImpl> constructor = AnalyticsResultRebuildServiceImpl.class.getDeclaredConstructor(
                AnalyticsQuestionAggregateResultSourceReader.class,
                AnalyticsTopicKeyStrategy.class,
                AnalyticsUserTopicAggregateWriter.class,
                AnalyticsDepartmentTopicAggregateWriter.class,
                AnalyticsQuestionAggregateWriter.class,
                reporterClass
            );
            constructor.setAccessible(true);

            Object reporterProxy = Proxy.newProxyInstance(
                reporterClass.getClassLoader(),
                new Class<?>[] { reporterClass },
                capturingReporter
            );

            return constructor.newInstance(
                reader,
                topicKeyStrategy,
                userTopicWriter,
                departmentTopicWriter,
                questionWriter,
                reporterProxy
            );
        } catch (NoSuchMethodException exception) {
            fail(CONTRACT_MESSAGE, exception);
            throw new IllegalStateException("Unreachable");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }
    }

    private static Object invokeOutcomeAccessor(Object outcome, String accessorName) {
        return invokeRecordAccessor(outcome, accessorName);
    }

    private static Object invokeRecordAccessor(Object target, String accessorName) {
        try {
            return target.getClass().getMethod(accessorName).invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }
    }

    private static void assertType(
        Class<?> actualType,
        Class<?> expectedType,
        String message,
        List<String> violations
    ) {
        if (!expectedType.equals(actualType)) {
            violations.add(message + ": " + actualType.getName());
        }
    }

    private static void assertRecordComponent(
        RecordComponent component,
        String expectedName,
        Class<?> expectedType,
        List<String> violations
    ) {
        if (!expectedName.equals(component.getName())) {
            violations.add("Unsupported-row component name mismatch: " + component.getName());
        }
        if (!expectedType.equals(component.getType())) {
            violations.add("Unsupported-row component type mismatch for " + expectedName + ": " + component.getType().getName());
        }
    }

    private static final class FakeResultSourceReader implements AnalyticsQuestionAggregateResultSourceReader {

        private final List<AnalyticsQuestionAggregateResultSourceRow> rowsToReturn;

        private FakeResultSourceReader(List<AnalyticsQuestionAggregateResultSourceRow> rowsToReturn) {
            this.rowsToReturn = rowsToReturn;
        }

        @Override
        public List<AnalyticsQuestionAggregateResultSourceRow> readQuestionAggregateRows(
            Instant periodStartInclusive,
            Instant periodEndExclusive
        ) {
            return rowsToReturn;
        }
    }

    private static final class FakeTopicKeyStrategy implements AnalyticsTopicKeyStrategy {

        @Override
        public AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
            if (Objects.equals(sourceRow.resultId(), 101L)) {
                return new AnalyticsTopicKeyResolution(9001L, true, "supported test topic");
            }
            if (Objects.equals(sourceRow.resultId(), 102L)) {
                return new AnalyticsTopicKeyResolution(9001L, true, "supported test topic");
            }
            return new AnalyticsTopicKeyResolution(null, false, "unsupported immutable topic anchor");
        }
    }

    private static final class CapturingUserTopicAggregateWriter implements AnalyticsUserTopicAggregateWriter {

        private List<AnalyticsUserTopicAggregateRow> capturedRows = List.of();

        @Override
        public void replaceUserTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsUserTopicAggregateRow> aggregateRows
        ) {
            capturedRows = List.copyOf(aggregateRows);
        }
    }

    private static final class CapturingDepartmentTopicAggregateWriter implements AnalyticsDepartmentTopicAggregateWriter {

        private List<AnalyticsDepartmentTopicAggregateRow> capturedRows = List.of();

        @Override
        public void replaceDepartmentTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
        ) {
            capturedRows = List.copyOf(aggregateRows);
        }
    }

    private static final class CapturingQuestionAggregateWriter implements AnalyticsQuestionAggregateWriter {

        private List<AnalyticsQuestionAggregateRow> capturedRows = List.of();

        @Override
        public void replaceQuestionAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsQuestionAggregateRow> aggregateRows
        ) {
            capturedRows = List.copyOf(aggregateRows);
        }
    }

    private static final class CapturingOutcomeReporter implements InvocationHandler {

        private int invocationCount;
        private Object capturedOutcome;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("reportResultRebuildOutcome".equals(method.getName())) {
                invocationCount++;
                capturedOutcome = args == null || args.length == 0 ? null : args[0];
                return null;
            }
            if ("toString".equals(method.getName())) {
                return "CapturingOutcomeReporter";
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName())) {
                return proxy == (args == null || args.length == 0 ? null : args[0]);
            }
            return null;
        }
    }
}
