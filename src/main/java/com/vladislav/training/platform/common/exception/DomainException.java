package com.vladislav.training.platform.common.exception;

/**
 * Класс {@code DomainException}.
 */
public class DomainException extends RuntimeException {

    private final String errorCode;

    public DomainException(String message) {
        this(null, message, null);
    }

    public DomainException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public DomainException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
