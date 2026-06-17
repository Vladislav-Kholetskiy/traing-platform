package com.vladislav.training.platform.common.web;

import com.vladislav.training.platform.common.context.CurrentRequestContext;
import com.vladislav.training.platform.common.context.RequestContextHolder;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.DomainException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Класс {@code GlobalExceptionHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final UtcClock utcClock;

    public GlobalExceptionHandler(UtcClock utcClock) {
        this.utcClock = utcClock;
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        return build(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException exception) {
        return build(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return build(HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, "Request body is invalid");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        return build(HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ApiError> handlePolicyViolation(PolicyViolationException exception) {
        return build(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(PersistenceConstraintViolationException.class)
    public ResponseEntity<ApiError> handlePersistenceConstraint(PersistenceConstraintViolationException exception) {
        return build(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(DomainException exception) {
        return build(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, Exception exception) {
        return build(status, exception.getMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        ApiError body = new ApiError(
            utcClock.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            currentCorrelationId()
        );

        return ResponseEntity.status(status).body(body);
    }

    private String currentCorrelationId() {
        return RequestContextHolder.getCurrent()
            .map(CurrentRequestContext::correlationId)
            .orElse(null);
    }
}
