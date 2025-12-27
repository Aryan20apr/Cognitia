package com.intellidesk.cognitia.userandauth.security;

import java.io.IOException;


import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.JwtTokenExpiredException;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.JwtTokenInvalidException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
    
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    
        String error = "Unauthorized";
        String message = authException.getMessage();
        log.error("[JwtAuthenticationEntryPoint] [commence] Authentication exception", authException);
        // Provide more specific error messages
        if (authException instanceof JwtTokenExpiredException) {
            error = "Token Expired";
            message = "Your access token has expired. Please refresh your token or login again.";
        } else if (authException instanceof JwtTokenInvalidException) {
            error = "Invalid Token";
            message = "The provided token is invalid or malformed.";
        } else if (message == null || message.isEmpty()) {
            message = "Full authentication is required to access this resource";
        }
    
        response.getWriter().write(
            String.format(
                "{\"error\": \"%s\", \"message\": \"%s\"}",
                error,
                message
            ));
        }
      
}
