package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Класс {@code OrganizationalUnitTreePathBuilder}.
 */
@Component
final class OrganizationalUnitTreePathBuilder {

    OrganizationalUnitTreePosition resolvePlacement(OrganizationalUnit parentUnit, String unitName) {
        String pathSegment = canonicalPathSegment(unitName);
        String canonicalPath = parentUnit == null ? "/" + pathSegment : parentUnit.path() + "/" + pathSegment;

        if (canonicalPath.isBlank() || !canonicalPath.startsWith("/") || canonicalPath.endsWith("/") || canonicalPath.contains("//")) {
            throw new ValidationException("Generated organizationalUnit.path is not canonical: " + canonicalPath);
        }

        int depth = parentUnit == null ? 0 : parentUnit.depth() + 1;
        return new OrganizationalUnitTreePosition(canonicalPath, depth);
    }

    private String canonicalPathSegment(String unitName) {
        String normalized = Normalizer.normalize(unitName, Normalizer.Form.NFKC)
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{Nd}]+", "-")
            .replaceAll("(^-+|-+$)", "");

        if (normalized.isBlank()) {
            throw new ValidationException("organizationalUnit.name cannot be transformed into a canonical path segment");
        }

        return normalized;
    }
}