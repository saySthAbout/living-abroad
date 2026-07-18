package com.livingabroad.backend.exception;

public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException() {
        super("분석 결과를 찾을 수 없습니다.");
    }
}
