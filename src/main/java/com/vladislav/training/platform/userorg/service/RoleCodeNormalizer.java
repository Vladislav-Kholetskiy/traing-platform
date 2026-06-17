package com.vladislav.training.platform.userorg.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Класс {@code RoleCodeNormalizer}.
 */
public final class RoleCodeNormalizer {

    private RoleCodeNormalizer() {
    }

    public static String normalize(String roleCode) {
        if (roleCode == null) {
            return null;
        }

        return switch (roleCode.toUpperCase(Locale.ROOT)) {
            case "ROLE_OPERATIONS", "ROLE_OPERATOR" -> "OPERATOR";
            case "ROLE_MANAGER" -> "MANAGER";
            default -> roleCode;
        };
    }

    public static Set<String> normalizeAll(Collection<String> roleCodes) {
        LinkedHashSet<String> normalizedRoleCodes = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            String normalizedRoleCode = normalize(roleCode);
            if (normalizedRoleCode != null) {
                normalizedRoleCodes.add(normalizedRoleCode);
            }
        }
        return Set.copyOf(normalizedRoleCodes);
    }
}

