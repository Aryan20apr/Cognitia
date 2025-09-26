package com.intellidesk.cognitia.utils.exceptionHandling;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ResourceUploadException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceUploadException.class)
    public ResponseEntity<ExceptionApiResponse<?>> handleFileUploadException(ResourceUploadException e){

       ExceptionApiResponse<Object> response = ExceptionApiResponse.<Object>builder()
                                .message(e.getMessage())
                                .data(e.getData())
                                .code(500)
                                .build();

        return ResponseEntity.status(500).body(response);

    }
}
