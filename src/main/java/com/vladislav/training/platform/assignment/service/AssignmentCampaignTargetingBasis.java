package com.vladislav.training.platform.assignment.service;

import java.util.Locale;
import java.util.Objects;

record AssignmentCampaignTargetingBasis(String value) {

    AssignmentCampaignTargetingBasis {
        Objects.requireNonNull(value, "value must not be null");
        value = normalize(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
