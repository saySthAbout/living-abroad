package com.livingabroad.backend.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String code,
    String message,
    String path,
    List<FieldErrorDetail> fieldErrors
) {
    public record FieldErrorDetail(String field, String reason) {
    }
}
