package com.intellidesk.cognitia.utils.exceptionHandling;

public class LlmResponseParseException extends RuntimeException {

    public LlmResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
