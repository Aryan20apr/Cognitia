package com.intellidesk.cognitia.utils.exceptionHandling;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that catches exceptions thrown by other filters and delegates them
 * to the HandlerExceptionResolver, allowing @RestControllerAdvice handlers
 * to process them uniformly.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class FilterExceptionHandler extends OncePerRequestFilter {

    private final HandlerExceptionResolver exceptionResolver;

    public FilterExceptionHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("[FilterExceptionHandler] Caught exception in filter chain: {}", e.getMessage());
            
            Throwable cause = e;
            if (e instanceof ServletException && e.getCause() != null) {
                cause = e.getCause();
            }
            
            // Spring Security handle authentication/authorization exceptions
            if (cause instanceof AuthenticationException || 
                cause instanceof AccessDeniedException) {
                log.debug("[FilterExceptionHandler] Rethrowing security exception");
                throw e;
            }
            
            // Delegate to HandlerExceptionResolver (same as @RestControllerAdvice)
            Exception exceptionToHandle = cause instanceof Exception 
                ? (Exception) cause 
                : e;
            
            log.info("[FilterExceptionHandler] Delegating to HandlerExceptionResolver: {}", 
                exceptionToHandle.getClass().getSimpleName());
            
            exceptionResolver.resolveException(request, response, null, exceptionToHandle);
        }
    }
}

