package com.intellidesk.cognitia.utils.exceptionHandling;


public class DuplicateRequestAlreadyProcessedException extends RuntimeException {
    public DuplicateRequestAlreadyProcessedException(String requestId) {
        super("Request already processed: " + requestId);
    }
}
  