package com.livingabroad.backend.exception;

public class ChatSessionNotFoundException extends RuntimeException {

    public ChatSessionNotFoundException() {
        super("상담 이력을 찾을 수 없습니다.");
    }
}
