package com.intellidesk.cognitia.utils.exceptionHandling.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResourceUploadException extends RuntimeException {

    String message;
    Object data;
    
    public ResourceUploadException(String message){
        super(message);
    }
}
