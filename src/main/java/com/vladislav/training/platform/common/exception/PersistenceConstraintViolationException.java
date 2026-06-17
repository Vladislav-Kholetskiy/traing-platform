package com.vladislav.training.platform.common.exception;

/**
 * Класс {@code PersistenceConstraintViolationException}.
 */
public class PersistenceConstraintViolationException extends DomainException {

    public PersistenceConstraintViolationException(String message) {
        super(message);
    }

    public PersistenceConstraintViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
