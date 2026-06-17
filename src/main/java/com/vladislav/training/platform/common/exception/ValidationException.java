package com.vladislav.training.platform.common.exception;

/**
 * Класс {@code ValidationException}.
 */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
