package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.testing.controller.CurrentAttemptReadController;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Проверяет, что {@code SelfHistoricalResultApiExposure} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultApiExposureRegressionTest {

    @Test
    void selfHistoricalResultContourIsPublishedOnlyAsDedicatedSelfScopedControllerApi() {
        assertThat(SelfHistoricalResultController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(SelfHistoricalResultController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/self/results/history");
        assertThat(Arrays.stream(SelfHistoricalResultController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(Method::getName)
            .toList())
            .containsExactly("findSelfHistoricalResults");
    }

    @Test
    void currentAttemptApiSurfaceDoesNotMasqueradeAsImmutableResultHistoryEndpoint() {
        assertThat(CurrentAttemptReadController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(CurrentAttemptReadController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/current-attempts");
        assertThat(Arrays.stream(CurrentAttemptReadController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(Method::getName)
            .toList())
            .contains("findCurrentAssignedAttempt", "findCurrentSelfAttempt")
            .doesNotContain(
                "findSelfHistoricalResults",
                "findHistoricalResults",
                "findResultHistory",
                "findSelfResultHistory"
            );
    }

}
