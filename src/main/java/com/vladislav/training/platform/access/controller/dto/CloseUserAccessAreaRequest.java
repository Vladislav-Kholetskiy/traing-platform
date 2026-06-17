package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Запрос {@code CloseUserAccessAreaRequest}.
 */
public record CloseUserAccessAreaRequest(Instant validTo) {
}
