package com.livingabroad.backend.exception;

public class AnalysisNotCompletedException extends RuntimeException {

    public AnalysisNotCompletedException() {
        super("완료된 분석만 공유할 수 있습니다.");
    }
}
