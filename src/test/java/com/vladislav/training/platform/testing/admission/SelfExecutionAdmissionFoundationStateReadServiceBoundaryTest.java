package com.vladislav.training.platform.testing.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code SelfExecutionAdmissionFoundationStateReadService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class SelfExecutionAdmissionFoundationStateReadServiceBoundaryTest {

    @Test
    void selfExecutionFoundationDependsOnCanonicalSelfVisiblePublicationCollaborators() {
        assertThat(fieldTypes(SelfExecutionAdmissionFoundationStateReadServiceImpl.class))
            .containsExactlyInAnyOrder(
                TestRepository.class,
                TopicRepository.class,
                CourseRepository.class
            )
            .doesNotContain(com.vladislav.training.platform.content.service.TestQueryService.class);
    }

    @Test
    void selfExecutionFoundationSourceDoesNotReintroduceLocalShortcutVisibilityLogic() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/admission/SelfExecutionAdmissionFoundationStateReadServiceImpl.java"
        ));

        assertThat(source)
            .contains("requirePublishedTopic")
            .contains("requirePublishedCourse")
            .contains("requireSelfExecutionEligible")
            .doesNotContain("requireSelfVisible(");
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
