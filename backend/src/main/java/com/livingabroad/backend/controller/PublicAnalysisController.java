package com.livingabroad.backend.controller;

import com.livingabroad.backend.dto.analysis.AnalysisDetailResponse;
import com.livingabroad.backend.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig에서 "/api/public/**"를 permitAll로 열어둔, 로그인 없이 접근 가능한 API 전용 컨트롤러.
@RestController
@RequestMapping("/api/public/analyses")
public class PublicAnalysisController {

    private final AnalysisService analysisService;

    public PublicAnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/shared/{shareToken}")
    public ResponseEntity<AnalysisDetailResponse> getShared(@PathVariable String shareToken) {
        return ResponseEntity.ok(analysisService.getSharedAnalysisDetail(shareToken));
    }
}
