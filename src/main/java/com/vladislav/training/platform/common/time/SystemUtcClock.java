package com.vladislav.training.platform.common.time;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Класс {@code SystemUtcClock}.
 */
@Component
public class SystemUtcClock implements UtcClock {

    private final Clock clock;

    public SystemUtcClock() {
        this(Clock.systemUTC());
    }

    SystemUtcClock(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return Instant.now(clock);
    }
}
