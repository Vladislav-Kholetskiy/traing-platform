package com.vladislav.training.platform.result.domain;

import java.util.Objects;
/**
 * Запись данных {@code ResultOrgContextSnapshot}.
 */
public record ResultOrgContextSnapshot(
    Long organizationalUnitIdSnapshot,
    String organizationalPathSnapshot
) {

    public ResultOrgContextSnapshot {
        Objects.requireNonNull(organizationalUnitIdSnapshot, "organizationalUnitIdSnapshot must not be null");
        Objects.requireNonNull(organizationalPathSnapshot, "organizationalPathSnapshot must not be null");
    }
}
