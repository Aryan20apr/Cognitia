package com.intellidesk.cognitia.utils.exceptionHandling;

import lombok.Builder;

@Builder
public record ExceptionApiResponse<T>(
        String message,
        boolean success,
        Integer code,
        T data) {
}
