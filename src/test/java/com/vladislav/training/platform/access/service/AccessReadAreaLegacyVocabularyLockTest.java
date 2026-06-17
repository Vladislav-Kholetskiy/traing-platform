package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Фиксирует словарь и смысловые границы вокруг {@code AccessReadAreaLegacy}.
 * Это помогает не расползтись именам и договорённостям.
 */
class AccessReadAreaLegacyVocabularyLockTest {

    private static final List<String> EXPECTED_WAVE_FIVE_CONTOURS = List.of(
        "SELF_RESULT_HISTORY",
        "MANAGERIAL_CURRENT_SUPERVISION",
        "MANAGERIAL_HISTORICAL_ANALYTICS",
        "EXPERT_QUESTION_ANALYTICS"
    );

    @Test
    void analyticsReadContoursAlreadyExistWithExactVocabulary() {
        List<String> contourNames = Arrays.stream(AccessReadArea.values())
            .map(Enum::name)
            .toList();

        assertThat(contourNames)
            .contains(
                "SELF_RESULT_HISTORY",
                "MANAGERIAL_CURRENT_SUPERVISION",
                "MANAGERIAL_HISTORICAL_ANALYTICS",
                "EXPERT_QUESTION_ANALYTICS"
            );
        assertThat(contourNames)
            .filteredOn(EXPECTED_WAVE_FIVE_CONTOURS::contains)
            .containsExactlyInAnyOrderElementsOf(EXPECTED_WAVE_FIVE_CONTOURS);
    }
}
