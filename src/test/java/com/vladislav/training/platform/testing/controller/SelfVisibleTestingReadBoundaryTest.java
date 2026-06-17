package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.assignment.service.AssignmentSelfScopedQueryService;
import com.vladislav.training.platform.audit.service.AuditService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.content.service.CourseQueryService;
import com.vladislav.training.platform.content.service.QuestionQueryService;
import com.vladislav.training.platform.content.service.TestQueryService;
import com.vladislav.training.platform.content.service.TopicQueryService;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import com.vladislav.training.platform.testing.service.AssignedAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitTerminalService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonTerminalService;
import com.vladislav.training.platform.testing.service.SelfAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitTerminalService;
import com.vladislav.training.platform.testing.service.SelfVisibleTestVisibilityFilter;
import com.vladislav.training.platform.testing.service.SelfVisibleTestingProjectionReader;
import com.vladislav.training.platform.testing.service.SelfVisibleTestingReadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 * Проверяет граничные случаи для {@code SelfVisibleTestingRead}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class SelfVisibleTestingReadBoundaryTest {

    @Test
    void selfVisibleTestingControllerUsesDedicatedSelfTestingRootRatherThanAssignmentOrExpertContentRoots() {
        assertThat(SelfVisibleTestingReadController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/self-testing/tests");
    }

    @Test
    void selfVisibleTestingControllerExposesOnlyReadHandlerVocabulary() {
        assertThat(Arrays.stream(SelfVisibleTestingReadController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(Method::getName)
            .toList())
            .containsExactlyInAnyOrder("findSelfVisibleTests", "findSelfVisibleTestById", "findSelfVisibleTopicById")
            .doesNotContain("startSelfAttempt", "resumeAttempt", "submitAttempt", "recordResult");
    }

    @Test
    void selfVisibleTestingReadSliceDoesNotDependOnAssignmentOrCommandSideCollaborators() {
        assertThat(fieldTypes(SelfVisibleTestingReadController.class))
            .containsExactly(SelfVisibleTestingReadService.class)
            .doesNotContain(
                CourseQueryService.class,
                TopicQueryService.class,
                TestQueryService.class,
                QuestionQueryService.class,
                AssignmentSelfScopedQueryService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                ActiveAttemptAnswerMutationService.class,
                AssignedAttemptAnswerMutationEntryService.class,
                SelfAttemptAnswerMutationEntryService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonSequencingService.class,
                SelfAttemptAbandonTerminalService.class,
                AssignmentCountedResultHandoffService.class,
                ResultRecordingService.class
            );

        assertThat(fieldTypes(SelfVisibleTestingReadService.class))
            .contains(
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class,
                SelfVisibleTestingProjectionReader.class
            )
            .extracting(Class::getName)
            .doesNotContain(
                CourseQueryService.class.getName(),
                TopicQueryService.class.getName(),
                TestQueryService.class.getName(),
                QuestionQueryService.class.getName(),
                SelfVisibleTestVisibilityFilter.class.getName(),
                AssignmentSelfScopedQueryService.class.getName(),
                AssignedAttemptEntryService.class.getName(),
                SelfAttemptEntryService.class.getName(),
                ActiveAttemptAnswerMutationService.class.getName(),
                AssignedAttemptAnswerMutationEntryService.class.getName(),
                SelfAttemptAnswerMutationEntryService.class.getName(),
                AssignedAttemptSubmitSequencingService.class.getName(),
                AssignedAttemptSubmitTerminalService.class.getName(),
                SelfAttemptSubmitSequencingService.class.getName(),
                SelfAttemptSubmitTerminalService.class.getName(),
                SelfAttemptAbandonSequencingService.class.getName(),
                SelfAttemptAbandonTerminalService.class.getName(),
                AssignmentCountedResultHandoffService.class.getName(),
                ResultRecordingService.class.getName(),
                CriticalCommandAuditSupport.class.getName(),
                AuditService.class.getName()
            );
    }

    @Test
    void selfVisibleTestingReadWillUseDedicatedQueryContourInsteadOfExternalContentAuthoringContour() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfVisibleTestingReadService.java"
        ));

        assertThat(source)
            .contains("AccessReadArea.SELF_VISIBLE_TESTING")
            .doesNotContain("AccessReadArea.CONTENT_AUTHORING");
    }

    @Test
    void selfVisibleReadModelsAreNotReusedByCommandOrMutationControllers() throws Exception {
        try (Stream<Path> controllerSources = Files.walk(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/controller"
        ))) {
            List<Path> nonSelfVisibleControllerSources = controllerSources
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.getFileName().toString().equals("SelfVisibleTestingReadController.java"))
                .toList();

            for (Path sourcePath : nonSelfVisibleControllerSources) {
                assertThat(Files.readString(sourcePath))
                    
                    .doesNotContain("SelfVisibleTestReadModel")
                    .doesNotContain("SelfVisibleTestCatalogEntryReadModel")
                    .doesNotContain("SelfVisibleTopicReadModel");
            }
        }
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
