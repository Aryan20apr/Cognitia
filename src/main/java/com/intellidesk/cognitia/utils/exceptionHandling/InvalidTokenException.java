package com.intellidesk.cognitia.utils.exceptionHandling;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message){
        super(message);
    }
}