package com.vladislav.training.platform.access.service;

import java.util.Set;

/**
 * Запись данных {@code AccessReadScope}.
 */
public record AccessReadScope(
    boolean readAllowed,
    boolean fullOrganizationalUnitAccess,
    Set<Long> unitOnlyIds,
    Set<String> subtreePaths
) {

    public AccessReadScope {
        unitOnlyIds = unitOnlyIds == null ? Set.of() : Set.copyOf(unitOnlyIds);
        subtreePaths = subtreePaths == null ? Set.of() : Set.copyOf(subtreePaths);
        if (!readAllowed) {
            fullOrganizationalUnitAccess = false;
            unitOnlyIds = Set.of();
            subtreePaths = Set.of();
        }
    }

    public static AccessReadScope denyAll() {
        return new AccessReadScope(false, false, Set.of(), Set.of());
    }

    public static AccessReadScope fullAccess() {
        return new AccessReadScope(true, true, Set.of(), Set.of());
    }

    public static AccessReadScope scoped(Set<Long> unitOnlyIds, Set<String> subtreePaths) {
        return new AccessReadScope(true, false, unitOnlyIds, subtreePaths);
    }
}

