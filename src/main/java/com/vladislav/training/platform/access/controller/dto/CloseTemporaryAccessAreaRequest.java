package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Запрос {@code CloseTemporaryAccessAreaRequest}.
 */
public record CloseTemporaryAccessAreaRequest(Instant validTo) {
}
