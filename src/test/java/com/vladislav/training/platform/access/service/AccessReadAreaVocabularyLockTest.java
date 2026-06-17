package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Фиксирует словарь и смысловые границы вокруг {@code AccessReadArea}.
 * Это помогает не расползтись именам и договорённостям.
 */
class AccessReadAreaVocabularyLockTest {

    private static final List<String> MANDATORY_WAVE_SIX_CONTOURS = List.of(
        "NOTIFICATION_ADMINISTRATION",
        "NOTIFICATION_RULE_ADMINISTRATION",
        "IMPORT_JOB_ADMINISTRATION",
        "AUDIT_EVENT_ADMINISTRATION"
    );

    private static final List<String> WAVE_FIVE_SUBSTITUTE_CONTOURS = List.of(
        "SELF_RESULT_HISTORY",
        "MANAGERIAL_CURRENT_SUPERVISION",
        "MANAGERIAL_HISTORICAL_ANALYTICS",
        "EXPERT_QUESTION_ANALYTICS"
    );

    private static final List<String> FORBIDDEN_GENERIC_ADMIN_SHORTCUTS = List.of(
        "ADMIN_ALL",
        "GENERIC_ADMIN",
        "OPERATIONAL_ADMINISTRATION",
        "TABLE_ADMINISTRATION"
    );

    @Test
    void mandatoryAdministrativeReadContoursMustExistAsDedicatedVocabulary() {
        MANDATORY_WAVE_SIX_CONTOURS.forEach(contourName ->
            assertThatCode(() -> AccessReadArea.valueOf(contourName))
                
                .doesNotThrowAnyException()
        );
    }

    @Test
    void administrativeReadContoursMustNotBeReplacedByAnalyticsContours() {
        List<String> contourNames = Arrays.stream(AccessReadArea.values())
            .map(Enum::name)
            .toList();

        assertThat(contourNames)
            
            .containsAll(MANDATORY_WAVE_SIX_CONTOURS);
        assertThat(contourNames)
            
            .containsAll(WAVE_FIVE_SUBSTITUTE_CONTOURS);
        assertThat(MANDATORY_WAVE_SIX_CONTOURS)
            
            .doesNotContainAnyElementsOf(WAVE_FIVE_SUBSTITUTE_CONTOURS);
    }

    @Test
    void administrativeReadContoursMustNotExposeGenericAdministrativeShortcut() {
        List<String> contourNames = Arrays.stream(AccessReadArea.values())
            .map(Enum::name)
            .toList();

        assertThat(contourNames)
            
            .doesNotContainAnyElementsOf(FORBIDDEN_GENERIC_ADMIN_SHORTCUTS);
    }
}
