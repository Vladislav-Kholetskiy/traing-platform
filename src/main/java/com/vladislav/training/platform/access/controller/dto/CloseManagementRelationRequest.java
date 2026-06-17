package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Запрос {@code CloseManagementRelationRequest}.
 */
public record CloseManagementRelationRequest(Instant validTo) {
}
