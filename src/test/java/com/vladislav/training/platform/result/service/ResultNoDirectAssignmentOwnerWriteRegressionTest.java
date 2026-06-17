package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.repository.AssignmentCampaignCourseRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ResultNoDirectAssignmentOwnerWrite} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ResultNoDirectAssignmentOwnerWriteRegressionTest {

    @Test
    void resultRecordingImplementationDependsOnNarrowAssignmentHandoffInsteadOfDirectAssignmentRepositories() {
        assertThat(fieldTypes(ResultRecordingServiceImpl.class))
            .contains(AssignmentCountedResultHandoffService.class)
            .doesNotContain(
                AssignmentRepository.class,
                AssignmentTestRepository.class,
                AssignmentStatusRecalculationService.class,
                AssignmentCampaignRepository.class,
                AssignmentCampaignCourseRepository.class,
                AssignmentCampaignRecipientSnapshotRepository.class
            );
    }

    @Test
    void snapshotFactsProviderReadsAssignmentFactsWithoutAssignmentOwnerWriteDrift() throws IOException {
        assertThat(fieldTypes(ResultRecordingSnapshotFactsProvider.class))
            .contains(
                AssignmentRepository.class,
                AssignmentTestRepository.class,
                AssignmentCampaignRecipientSnapshotRepository.class,
                TestRepository.class,
                TestQuestionRepository.class,
                QuestionRepository.class,
                AnswerOptionRepository.class,
                UserAnswerRepository.class,
                UserAnswerItemRepository.class
            )
            .doesNotContain(
                AssignmentCampaignRepository.class,
                AssignmentCampaignCourseRepository.class,
                AssignmentStatusRecalculationService.class
            );

        String source = read("src/main/java/com/vladislav/training/platform/result/service/ResultRecordingSnapshotFactsProvider.java");
        assertThat(source)
            .contains("findAssignmentTestById")
            .contains("findAssignmentById")
            .contains("findAssignmentCampaignRecipientSnapshotsByCampaignId")
            .contains("findUserAnswersByTestAttemptId")
            .contains("findUserAnswerItemsByUserAnswerId")
            .doesNotContain(
                "saveAssignment(",
                "saveAssignmentTest(",
                "saveAssignmentCampaign",
                "saveAssignmentCampaignRecipientSnapshot",
                "refreshAssignmentStatusCache",
                "AssignmentCampaignRepository",
                "AssignmentCampaignCourseRepository"
            );
    }

    @Test
    void resultPackageDoesNotDependOnAssignmentCampaignRepositories() throws IOException {
        String packageSource = readAllJavaSources("src/main/java/com/vladislav/training/platform/result/service");

        assertThat(packageSource)
            .doesNotContain(
                "AssignmentCampaignRepository",
                "AssignmentCampaignCourseRepository",
                "saveAssignment(",
                "saveAssignmentTest(",
                "saveAssignmentCampaign",
                "saveAssignmentCampaignRecipientSnapshot"
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private String readAllJavaSources(String directory) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(Path.of(directory))) {
            return paths
                .filter(path -> path.toString().endsWith(".java"))
                .sorted()
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Cannot read file: " + path, exception);
                    }
                })
                .reduce("", (left, right) -> left + "\n" + right);
        }
    }
}
