package com.intellidesk.cognitia.utils.exceptionHandling;

public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
