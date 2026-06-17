package com.vladislav.training.platform.common.time;

import java.time.Instant;

/**
 * Интерфейс {@code UtcClock}.
 */
public interface UtcClock {

    Instant now();
}
