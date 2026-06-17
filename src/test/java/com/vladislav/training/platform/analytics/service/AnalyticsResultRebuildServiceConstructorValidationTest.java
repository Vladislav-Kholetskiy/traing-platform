package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AnalyticsResultRebuildServiceConstructorValidation}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AnalyticsResultRebuildServiceConstructorValidationTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 constructor-based orchestration must fail fast for required dependencies; "
            + "question aggregate writer is required on the 5-arg path, while department writer remains optional for compatibility.";

    private final AnalyticsQuestionAggregateResultSourceReader sourceReader = new StubResultSourceReader();
    private final AnalyticsTopicKeyStrategy topicKeyStrategy = new StubTopicKeyStrategy();
    private final AnalyticsUserTopicAggregateWriter userTopicAggregateWriter = new NoOpUserTopicAggregateWriter();
    private final AnalyticsDepartmentTopicAggregateWriter departmentTopicAggregateWriter =
        new NoOpDepartmentTopicAggregateWriter();
    private final AnalyticsQuestionAggregateWriter questionAggregateWriter = new NoOpQuestionAggregateWriter();

    @Test
    void fiveArgConstructorValidatesRequiredDependenciesAndAllowsOptionalDepartmentWriter() {
        assertThatThrownBy(() -> new AnalyticsResultRebuildServiceImpl(
            null,
            topicKeyStrategy,
            userTopicAggregateWriter,
            departmentTopicAggregateWriter,
            questionAggregateWriter
        ))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AnalyticsResultRebuildServiceImpl(
            sourceReader,
            null,
            userTopicAggregateWriter,
            departmentTopicAggregateWriter,
            questionAggregateWriter
        ))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AnalyticsResultRebuildServiceImpl(
            sourceReader,
            topicKeyStrategy,
            null,
            departmentTopicAggregateWriter,
            questionAggregateWriter
        ))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AnalyticsResultRebuildServiceImpl(
            sourceReader,
            topicKeyStrategy,
            userTopicAggregateWriter,
            departmentTopicAggregateWriter,
            null
        ))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);

        assertThatCode(() -> new AnalyticsResultRebuildServiceImpl(
            sourceReader,
            topicKeyStrategy,
            userTopicAggregateWriter,
            null,
            questionAggregateWriter
        ))
            .withFailMessage(CONTRACT_MESSAGE)
            .doesNotThrowAnyException();
    }

    private static final class StubResultSourceReader implements AnalyticsQuestionAggregateResultSourceReader {

        @Override
        public List<AnalyticsQuestionAggregateResultSourceRow> readQuestionAggregateRows(
            Instant periodStartInclusive,
            Instant periodEndExclusive
        ) {
            return List.of();
        }
    }

    private static final class StubTopicKeyStrategy implements AnalyticsTopicKeyStrategy {

        @Override
        public AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
            return new AnalyticsTopicKeyResolution(null, false, "unsupported immutable topic anchor");
        }
    }

    private static final class NoOpUserTopicAggregateWriter implements AnalyticsUserTopicAggregateWriter {

        @Override
        public void replaceUserTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsUserTopicAggregateRow> aggregateRows
        ) {
        }
    }

    private static final class NoOpDepartmentTopicAggregateWriter implements AnalyticsDepartmentTopicAggregateWriter {

        @Override
        public void replaceDepartmentTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
        ) {
        }
    }

    private static final class NoOpQuestionAggregateWriter implements AnalyticsQuestionAggregateWriter {

        @Override
        public void replaceQuestionAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsQuestionAggregateRow> aggregateRows
        ) {
        }
    }
}
