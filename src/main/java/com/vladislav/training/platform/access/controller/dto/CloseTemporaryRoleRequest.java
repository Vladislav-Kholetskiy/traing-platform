package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Запрос {@code CloseTemporaryRoleRequest}.
 */
public record CloseTemporaryRoleRequest(Instant validTo) {
}
