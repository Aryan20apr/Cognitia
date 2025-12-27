package com.intellidesk.cognitia.utils.exceptionHandling.exceptions;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenInvalidException extends AuthenticationException {
    public JwtTokenInvalidException(String msg) {
        super(msg);
    }
}
