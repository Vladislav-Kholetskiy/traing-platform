package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Запрос {@code CloseTemporaryManagementDelegationRequest}.
 */
public record CloseTemporaryManagementDelegationRequest(Instant validTo) {
}
