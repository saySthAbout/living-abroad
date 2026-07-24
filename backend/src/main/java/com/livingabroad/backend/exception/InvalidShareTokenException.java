package com.livingabroad.backend.exception;

public class InvalidShareTokenException extends RuntimeException {

    public InvalidShareTokenException() {
        super("공유 링크를 찾을 수 없거나 더 이상 유효하지 않습니다.");
    }
}
