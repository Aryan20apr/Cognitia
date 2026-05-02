package com.intellidesk.cognitia.utils.exceptionHandling.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class ResourceUploadException extends RuntimeException {

    Object data;

    public ResourceUploadException(String message) {
        super(message);
    }

    public ResourceUploadException(String message, Object data) {
        super(message);
        this.data = data;
    }
}
