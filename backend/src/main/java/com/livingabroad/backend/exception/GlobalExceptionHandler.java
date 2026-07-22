package com.livingabroad.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException exception, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", exception.getMessage(), request, List.of());
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

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", exception.getMessage(), request, List.of());
    }

    // 회원가입 시 existsByEmail 체크와 save() 사이의 경쟁 조건으로 email 유니크 제약을 위반한 경우.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("데이터 무결성 제약 위반: {}", request.getRequestURI(), exception);
        return build(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다.", request, List.of());
    }

    // FastAPI(ai-server)가 응답하지 않거나 오류를 반환한 경우 (분석/상담 요청 모두 해당).
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleAiServerUnavailable(RestClientException exception, HttpServletRequest request) {
        log.error("AI 서버 호출 실패: {}", request.getRequestURI(), exception);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVER_UNAVAILABLE", "AI 서버에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.", request, List.of());
    }

    // 예상하지 못한 모든 예외의 최종 안전망 — 서버 내부 정보는 로그로만 남기고 클라이언트에는 안전한 메시지만 반환한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("예상하지 못한 오류 발생 (traceId={}, path={})", traceId, request.getRequestURI(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "일시적인 오류가 발생했습니다. 문제가 계속되면 traceId " + traceId + "와 함께 문의해 주세요.", request, List.of());
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
