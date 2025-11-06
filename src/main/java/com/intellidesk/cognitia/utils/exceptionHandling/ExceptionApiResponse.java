package com.intellidesk.cognitia.utils.exceptionHandling;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Exception response payload")
public record ExceptionApiResponse<T>(
        String message,
        boolean success,
        Integer code,
        T data) {
}
