package com.intellidesk.cognitia.utils.exceptionHandling.exceptions;

import lombok.Getter;

/**
 * A generic runtime exception that carries a message and any type of data.
 * Useful for custom error responses or domain-specific errors.
 */
/**
 * Custom exception with message and optional arbitrary data.
 */
@Getter
public class ApiException extends RuntimeException {

    private final Object data;

    public ApiException(String message) {
        super(message);
        this.data = null;
    }

    public ApiException(String message, Object data) {
        super(message);
        this.data = data;
    }

    public ApiException(String message, Throwable cause, Object data) {
        super(message, cause);
        this.data = data;
    }
}