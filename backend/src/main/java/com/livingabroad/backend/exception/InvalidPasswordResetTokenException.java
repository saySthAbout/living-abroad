package com.livingabroad.backend.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("비밀번호 재설정 링크가 유효하지 않거나 만료되었습니다.");
    }
}
