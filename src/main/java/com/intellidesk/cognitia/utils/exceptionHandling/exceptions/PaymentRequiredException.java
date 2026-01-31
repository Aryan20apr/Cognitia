package com.intellidesk.cognitia.utils.exceptionHandling.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a payment is required to perform an operation.
 * Maps to HTTP 402 Payment Required status code.
 * 
 * Used for:
 * - Plan upgrades that require payment verification
 * - Feature purchases that require payment
 * - Any operation gated by payment
 */
@Getter
public class PaymentRequiredException extends RuntimeException {

    private final Object data;

    public PaymentRequiredException(String message) {
        super(message);
        this.data = null;
    }

    public PaymentRequiredException(String message, Object data) {
        super(message);
        this.data = data;
    }

    public PaymentRequiredException(String message, Throwable cause) {
        super(message, cause);
        this.data = null;
    }

    public PaymentRequiredException(String message, Throwable cause, Object data) {
        super(message, cause);
        this.data = data;
    }
}
