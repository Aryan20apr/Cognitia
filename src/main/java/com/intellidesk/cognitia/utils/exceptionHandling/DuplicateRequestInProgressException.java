package com.intellidesk.cognitia.utils.exceptionHandling;

// DuplicateRequestInProgressException.java

public class DuplicateRequestInProgressException extends RuntimeException {
    public DuplicateRequestInProgressException(String requestId) {
        super("Request is already in progress: " + requestId);
    }
}



