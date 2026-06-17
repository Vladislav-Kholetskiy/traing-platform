package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ValidationException;

/**
 * Запись данных {@code OrganizationalUnitTreePosition}.
 */
record OrganizationalUnitTreePosition(String path, int depth) {

    OrganizationalUnitTreePosition {
        if (path == null || path.isBlank()) {
            throw new ValidationException("organizationalUnit.path must not be blank");
        }
        if (depth < 0) {
            throw new ValidationException("organizationalUnit.depth must be non-negative");
        }
    }
}