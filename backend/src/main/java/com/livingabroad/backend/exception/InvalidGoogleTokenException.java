package com.livingabroad.backend.exception;

public class InvalidGoogleTokenException extends RuntimeException {

    public InvalidGoogleTokenException() {
        super("Google 로그인 토큰이 유효하지 않습니다.");
    }
}
