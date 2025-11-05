package com.intellidesk.cognitia.utils.exceptionHandling;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ResourceUploadException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleAllRuntimeExcption(RuntimeException runtimeException) {
        log.error("[GlobalExceptionHandler] : [handleAllRuntimeExcption] : " + runtimeException.getMessage());
        runtimeException.printStackTrace();
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(runtimeException.getMessage())
                .data(null)
                .code(500)
                .build();

        return ResponseEntity.status(500).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleBadCredentialsException(
            BadCredentialsException badCredentialsException) {
        badCredentialsException.printStackTrace();
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(badCredentialsException.getMessage())
                .data(null)
                .code(401)
                .build();

        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(ResourceUploadException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleFileUploadException(ResourceUploadException e) {

        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(e.getMessage())
                .data(e.getData())
                .code(500)
                .build();

        return ResponseEntity.status(500).body(response);

    }

    
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<?> handleQuotaExceeded(QuotaExceededException ex) {
        Map<String,Object> body = Map.of(
                "error", "quota_exceeded",
                "message", ex.getMessage(),
                "timestamp", Instant.now()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

     @ExceptionHandler(DuplicateRequestInProgressException.class)
    public ResponseEntity<?> handleInProgress(DuplicateRequestInProgressException ex) {
        Map<String, Object> body = Map.of(
                "error", "request_in_progress",
                "message", ex.getMessage(),
                "timestamp", Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body); // 409
    }

    @ExceptionHandler(DuplicateRequestAlreadyProcessedException.class)
    public ResponseEntity<?> handleAlreadyProcessed(DuplicateRequestAlreadyProcessedException ex) {
        Map<String,Object> body = Map.of(
                "error","request_already_processed",
                "message",ex.getMessage(),
                "timestamp", Instant.now()
        );
        // 200 OK or 409; choose 409 conflict to indicate duplicate — or return cached response if available
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }
}
