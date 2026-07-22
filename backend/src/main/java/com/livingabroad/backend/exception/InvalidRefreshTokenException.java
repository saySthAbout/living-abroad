package com.livingabroad.backend.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("로그인이 만료되었습니다. 다시 로그인해 주세요.");
    }
}
