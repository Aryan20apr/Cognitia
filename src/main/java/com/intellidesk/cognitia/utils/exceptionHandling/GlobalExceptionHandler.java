package com.intellidesk.cognitia.utils.exceptionHandling;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;
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

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleApiException(ApiException ex) {
        log.warn("[GlobalExceptionHandler] : [handleApiException] : {}", ex.getMessage());
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(ex.getMessage())
                .data(ex.getData())
                .code(400)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMsg = ex.getMostSpecificCause().getMessage();
        log.warn("[GlobalExceptionHandler] : [handleDataIntegrityViolation] : {}", rootMsg);

        String message;
        HttpStatus status;

        if (rootMsg != null && rootMsg.contains("violates foreign key constraint")) {
            message = extractFkMessage(rootMsg);
            status = HttpStatus.CONFLICT;
        } else if (rootMsg != null && rootMsg.contains("violates unique constraint")) {
            message = extractUniqueMessage(rootMsg);
            status = HttpStatus.CONFLICT;
        } else if (rootMsg != null && rootMsg.contains("violates not-null constraint")) {
            message = "A required field is missing";
            status = HttpStatus.BAD_REQUEST;
        } else {
            message = "Data integrity violation";
            status = HttpStatus.CONFLICT;
        }

        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(message)
                .data(null)
                .code(status.value())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private String extractFkMessage(String rootMsg) {
        if (rootMsg.contains("is still referenced from table")) {
            int idx = rootMsg.indexOf("is still referenced from table");
            String tail = rootMsg.substring(idx);
            String referencedTable = tail.replaceAll("is still referenced from table \"?(\\w+)\"?.*", "$1");
            return "Cannot delete because it is still referenced by " + referencedTable;
        }
        return "Cannot perform operation due to existing references";
    }

    private String extractUniqueMessage(String rootMsg) {
        if (rootMsg.contains("email")) {
            return "A user with this email already exists";
        } else if (rootMsg.contains("phone")) {
            return "A user with this phone number already exists";
        }
        String detail = "";
        int detailIdx = rootMsg.indexOf("Detail:");
        if (detailIdx >= 0) {
            detail = rootMsg.substring(detailIdx + 7).trim();
        }
        return detail.isEmpty()
                ? "A record with the provided details already exists"
                : "Duplicate value: " + detail;
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ExceptionApiResponse<?>> handleAccessDenied(RuntimeException ex) {
        log.warn("[GlobalExceptionHandler] : [handleAccessDenied] : {}", ex.getMessage());
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message("Access denied: insufficient permissions")
                .data(null)
                .code(403)
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
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

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleLlmUnavailable(LlmUnavailableException ex) {
        log.error("[GlobalExceptionHandler] LLM unavailable: {}", ex.getMessage(), ex);
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(ex.getMessage())
                .data(null)
                .code(503)
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(LlmResponseParseException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleLlmResponseParse(LlmResponseParseException ex) {
        log.error("[GlobalExceptionHandler] LLM response parse error: {}", ex.getMessage(), ex);
        ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                .message(ex.getMessage())
                .data(null)
                .code(502)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
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
