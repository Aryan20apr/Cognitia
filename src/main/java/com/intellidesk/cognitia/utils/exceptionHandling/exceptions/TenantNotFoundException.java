package com.intellidesk.cognitia.utils.exceptionHandling.exceptions;

/**
 * Exception thrown when a tenant is not found in the system.
 */
public class TenantNotFoundException extends RuntimeException {
    
    public TenantNotFoundException(String message) {
        super(message);
    }
    
    public TenantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

