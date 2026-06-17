package com.vladislav.training.platform.common.exception;

/**
 * Класс {@code PolicyViolationException}.
 */
public class PolicyViolationException extends DomainException {

    public PolicyViolationException(String message) {
        super(message);
    }

    public PolicyViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
