package com.livingabroad.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorResponse.FieldErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();

        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 형식 또는 파라미터가 올바르지 않습니다.", request, fieldErrors);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnalysisNotFound(AnalysisNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(AnalysisAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAnalysisAccessDenied(AnalysisAccessDeniedException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ANALYSIS_ACCESS_DENIED", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ChatSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatSessionNotFound(ChatSessionNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "CHAT_SESSION_NOT_FOUND", exception.getMessage(), request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(
        HttpStatus status,
        String code,
        String message,
        HttpServletRequest request,
        List<ErrorResponse.FieldErrorDetail> fieldErrors
    ) {
        ErrorResponse body = new ErrorResponse(
            OffsetDateTime.now(),
            status.value(),
            code,
            message,
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
