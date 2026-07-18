package com.livingabroad.backend.exception;

public class AnalysisAccessDeniedException extends RuntimeException {

    public AnalysisAccessDeniedException() {
        super("해당 분석 결과를 조회할 수 없습니다.");
    }
}
