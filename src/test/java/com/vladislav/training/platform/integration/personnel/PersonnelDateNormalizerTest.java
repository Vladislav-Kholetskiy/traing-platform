package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.integration.personnel.support.PersonnelDateNormalizer;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelDateNormalizer}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelDateNormalizerTest {

    private final PersonnelDateNormalizer normalizer = new PersonnelDateNormalizer();

    @Test
    void isoDateIsNormalizedDeterministically() {
        assertThat(normalizer.normalize("2026-05-11", "temporaryValidFrom", 2))
            .isEqualTo(LocalDate.of(2026, 5, 11));
    }

    @Test
    void blankDateCellNormalizesToNull() {
        assertThat(normalizer.normalize("   ", "temporaryValidTo", 2)).isNull();
    }

    @Test
    void localizedFreeFormDateIsRejected() {
        assertThatThrownBy(() -> normalizer.normalize("11 мая 2026", "temporaryValidFrom", 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("temporaryValidFrom")
            .hasMessageContaining("row 2");
    }
}
