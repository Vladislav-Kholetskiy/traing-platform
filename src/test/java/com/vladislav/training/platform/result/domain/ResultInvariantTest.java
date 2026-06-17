package com.vladislav.training.platform.result.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code ResultInvariant}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ResultInvariantTest {

    private static final Instant COMPLETED_AT = Instant.parse("2026-04-13T08:40:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-04-13T08:45:00Z");

    @Test
    void rejectsNullUserIdSnapshot() {
        assertThatThrownBy(() -> new Result(
            101L,
            201L,
            null,
            AttemptMode.ASSIGNED,
            301L,
            401L,
            501L,
            "Assigned Test",
            scoringSnapshot(),
            true,
            true,
            COMPLETED_AT,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            CREATED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("userIdSnapshot must not be null");
    }

    @Test
    void acceptsValidUserIdSnapshot() {
        Result result = new Result(
            101L,
            201L,
            601L,
            AttemptMode.ASSIGNED,
            301L,
            401L,
            501L,
            "Assigned Test",
            scoringSnapshot(),
            true,
            true,
            COMPLETED_AT,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            CREATED_AT
        );

        assertThat(result.userIdSnapshot()).isEqualTo(601L);
    }

    @Test
    void rejectsBlankTestNameSnapshot() {
        assertThatThrownBy(() -> new Result(
            101L,
            201L,
            601L,
            AttemptMode.ASSIGNED,
            301L,
            401L,
            501L,
            "   ",
            scoringSnapshot(),
            true,
            true,
            COMPLETED_AT,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            CREATED_AT
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("testNameSnapshot must not be blank");
    }

    @Test
    void noLegacyConstructorWithoutUserAnchorRemainsAvailable() {
        assertThat(Arrays.stream(Result.class.getDeclaredConstructors()).map(Constructor::getParameterCount))
            .doesNotContain(12)
            .doesNotContain(13)
            .doesNotContain(14);
    }

    private ResultScoringSnapshot scoringSnapshot() {
        return new ResultScoringSnapshot(
            new BigDecimal("70.0000"),
            new BigDecimal("8.5000"),
            new BigDecimal("10.0000"),
            new BigDecimal("85.0000"),
            true,
            "DEFAULT_POLICY",
            "{\"policy\":\"v1\"}"
        );
    }
}
