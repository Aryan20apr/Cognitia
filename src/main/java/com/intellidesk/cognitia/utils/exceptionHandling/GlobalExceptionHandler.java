package com.intellidesk.cognitia.utils.exceptionHandling;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.PaymentRequiredException;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ResourceUploadException;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.TenantNotFoundException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleTenantNotFoundException(TenantNotFoundException ex) {
        log.warn("[GlobalExceptionHandler] : [handleTenantNotFoundException] : {}", ex.getMessage());
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(ex.getMessage())
                .data(null)
                .code(404)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("[GlobalExceptionHandler] : [handleIllegalArgumentException] : {}", ex.getMessage());
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(ex.getMessage())
                .data(null)
                .code(400)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

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

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleInvalidTokenException(InvalidTokenException invalidTokenException) {
        invalidTokenException.printStackTrace();
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(invalidTokenException.getMessage())
                .data(null)
                .code(401)
                .build();
    
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
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

    @ExceptionHandler(PaymentRequiredException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handlePaymentRequired(PaymentRequiredException ex) {
        log.warn("[GlobalExceptionHandler] : [handlePaymentRequired] : {}", ex.getMessage());
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(ex.getMessage())
                .data(ex.getData())
                .code(402)
                .build();
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
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

    @ExceptionHandler(ThreadBusyException.class)
    public ResponseEntity<?> handleThreadBusy(ThreadBusyException ex) {
        log.warn("[GlobalExceptionHandler] Thread busy: {} - queue position: {}", ex.getThreadId(), ex.getQueuePosition());
        Map<String, Object> body = Map.of(
                "error", "thread_busy",
                "code", "THREAD_BUSY",
                "message", ex.getMessage(),
                "threadId", ex.getThreadId(),
                "queuePosition", ex.getQueuePosition(),
                "retryable", true,
                "timestamp", Instant.now()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(Error.class)
public ResponseEntity<ExceptionApiResponse<?>> handleError(Error error) {
    log.error("[GlobalExceptionHandler] : [handleError] : " + error.getMessage(), error);
    error.printStackTrace();
    ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
            .message("Internal server error: " + error.getMessage())
            .data(null)
            .code(500)
            .build();
    return ResponseEntity.status(500).body(response);
}

@ExceptionHandler(Throwable.class)
public ResponseEntity<ExceptionApiResponse<?>> handleThrowable(Throwable throwable) {
    log.error("[GlobalExceptionHandler] : [handleThrowable] : " + throwable.getMessage(), throwable);
    throwable.printStackTrace();
    ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
            .message("Internal server error: " + throwable.getMessage())
            .data(null)
            .code(500)
            .build();
    return ResponseEntity.status(500).body(response);
}

@ExceptionHandler(MissingServletRequestPartException.class)
public ResponseEntity<ExceptionApiResponse<?>> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
    log.error("[GlobalExceptionHandler] : [handleMissingServletRequestPartException] : " + ex.getMessage(), ex);
    ex.printStackTrace();
    ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
            .message(ex.getMessage())
            .data(null)
            .code(400)
            .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ExceptionApiResponse<Map<String, String>>> handleValidationErrors(
        MethodArgumentNotValidException ex) {

    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult()
      .getFieldErrors()
      .forEach(error ->
          errors.put(error.getField(), error.getDefaultMessage())
      );

    ExceptionApiResponse<Map<String, String>> response =
            ExceptionApiResponse.<Map<String, String>>builder()
            .message("Validation failed")
            .data(errors)
            .code(400)
            .build();

    return ResponseEntity.badRequest().body(response);
}
}
