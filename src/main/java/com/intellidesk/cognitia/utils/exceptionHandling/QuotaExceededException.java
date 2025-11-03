package com.intellidesk.cognitia.utils.exceptionHandling;

public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) { super(message); }
}
