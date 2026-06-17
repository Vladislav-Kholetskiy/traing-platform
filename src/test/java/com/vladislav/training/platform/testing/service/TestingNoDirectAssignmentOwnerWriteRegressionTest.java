package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.repository.AssignmentCampaignCourseRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code TestingNoDirectAssignmentOwnerWrite} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class TestingNoDirectAssignmentOwnerWriteRegressionTest {

    @Test
    void testingServicesDoNotDependOnAssignmentCampaignRepositories() throws IOException {
        String source = readAllJavaSources("src/main/java/com/vladislav/training/platform/testing/service");

        assertThat(source)
            .doesNotContain(
                AssignmentCampaignRepository.class.getSimpleName(),
                AssignmentCampaignCourseRepository.class.getSimpleName(),
                AssignmentCampaignRecipientSnapshotRepository.class.getSimpleName()
            );
    }

    @Test
    void testingServicesDoNotExposeAssignmentOwnerWriteVocabulary() throws IOException {
        String source = readAllJavaSources("src/main/java/com/vladislav/training/platform/testing/service");

        assertThat(source)
            .doesNotContain(
                "saveAssignment(",
                "saveAssignmentTest(",
                "saveAssignmentCampaign(",
                "patchAssignment",
                "overrideAssignmentResult",
                "launchAssignmentCampaign",
                "replaceWithNewAssignment"
            );
    }

    @Test
    void attemptLifecycleServicesDoNotBecomeAssignmentMutationOwners() throws IOException {
        String assignedSubmit = read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitTerminalService.java");
        String assignedExpiry = read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptExpiryTerminalService.java");
        String selfSubmit = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitTerminalService.java");
        String selfAbandon = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonTerminalService.java");
        String activeMutation = read("src/main/java/com/vladislav/training/platform/testing/service/ActiveAttemptAnswerMutationService.java");

        assertThat(assignedSubmit + assignedExpiry + selfSubmit + selfAbandon + activeMutation)
            .doesNotContain(
                "saveAssignment(",
                "saveAssignmentTest(",
                "AssignmentCommandService",
                "AssignmentAdministrativeActionService",
                "AssignmentCampaignCommandService"
            );
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
