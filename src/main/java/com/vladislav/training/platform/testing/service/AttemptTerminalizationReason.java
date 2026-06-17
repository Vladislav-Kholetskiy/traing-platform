package com.vladislav.training.platform.testing.service;

/**
 * Перечисление {@code AttemptTerminalizationReason}.
 */
public enum AttemptTerminalizationReason {
    NORMAL_SUBMIT,
    ASSIGNED_EXPLICIT_EXPIRY,
    EXPIRED_BY_REFRESH,
    SELF_ABANDON
}
