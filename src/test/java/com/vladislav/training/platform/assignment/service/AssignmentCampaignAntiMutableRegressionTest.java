package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AssignmentCampaignAntiMutable} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AssignmentCampaignAntiMutableRegressionTest {

    @Test
    void commandSurfaceStaysLaunchOnlyWithoutStandaloneMutableCampaignEntrypoints() {
        assertThat(methodNames(AssignmentCampaignCommandService.class))
            .containsExactly("launchAssignmentCampaign")
            .doesNotContain(
                "createAssignmentCampaign",
                "recordRecipientSnapshot",
                "updateAssignmentCampaign",
                "addCourseToCampaign",
                "removeCourseFromCampaign",
                "rewriteCampaignComposition"
            );
    }

    @Test
    void implementationDoesNotExposePublicBypassAroundLaunchOnlySemantics() {
        assertThat(publicMethodNames(AssignmentCampaignCommandServiceImpl.class))
            .containsExactly("launchAssignmentCampaign")
            .doesNotContain(
                "persistLaunchCampaignRoot",
                "persistLaunchRecipientSnapshot",
                "createAssignmentCampaign",
                "recordRecipientSnapshot",
                "updateAssignmentCampaign"
            );
    }

    @Test
    void launchDocumentationAndServiceImplementationStayHistoricalRatherThanCrudLike() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandService.java"))
            .contains("launchAssignmentCampaign")
            .doesNotContain("createAssignmentCampaign(")
            .doesNotContain("recordRecipientSnapshot(");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandServiceImpl.java"))
            .contains("persistLaunchCampaignRoot")
            .contains("persistLaunchRecipientSnapshot")
            .doesNotContain("public AssignmentCampaign createAssignmentCampaign")
            .doesNotContain("public AssignmentCampaignRecipientSnapshot recordRecipientSnapshot");
    }

    @Test
    void existingLaunchAndReadSmokeCoverageRemainsCompatibleWithAntiDriftGuard() {
        assertThat(methodNames(AssignmentCampaignCommandServiceImplHappyPathTest.class))
            .contains("launchCreatesCampaignCompositionSnapshotsAssignmentsTestsRefreshAndAudit");
        assertThat(methodNames(AssignmentCampaignCommandServiceImplRejectPathTest.class))
            .contains("emptyRecipientPoolFailsClosed", "duplicateActiveAssignmentFailsClosed");
        assertThat(methodNames(AssignmentCampaignQueryServiceImplTest.class))
            .contains(
                "recipientSnapshotDetailReadUsesPostLaunchCampaignContourAndReturnsHistoricalSnapshot",
                "recipientSnapshotCampaignReadUsesPersistedSnapshotContourWithoutPreviewRecompute"
            );
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> publicMethodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
