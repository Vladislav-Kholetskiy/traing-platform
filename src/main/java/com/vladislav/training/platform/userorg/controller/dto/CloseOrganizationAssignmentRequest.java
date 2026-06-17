package com.vladislav.training.platform.userorg.controller.dto;

import java.time.Instant;

/**
 * Запрос {@code CloseOrganizationAssignmentRequest}.
 */
public record CloseOrganizationAssignmentRequest(Instant validTo) {
}
