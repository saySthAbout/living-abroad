package com.livingabroad.backend.controller;

import com.livingabroad.backend.dto.analysis.AnalysisCreateRequest;
import com.livingabroad.backend.dto.analysis.AnalysisCreateResponse;
import com.livingabroad.backend.dto.analysis.AnalysisDetailResponse;
import com.livingabroad.backend.dto.analysis.AnalysisHistoryPageResponse;
import com.livingabroad.backend.dto.analysis.AnalysisInputResponse;
import com.livingabroad.backend.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<AnalysisCreateResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody AnalysisCreateRequest request
    ) {
        AnalysisCreateResponse response = analysisService.createAnalysis(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/latest-input")
    public ResponseEntity<AnalysisInputResponse> getLatestInput(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(analysisService.getLatestInput(Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisDetailResponse> getDetail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long analysisId) {
        return ResponseEntity.ok(analysisService.getAnalysisDetail(Long.valueOf(jwt.getSubject()), analysisId));
    }

    @GetMapping
    public ResponseEntity<AnalysisHistoryPageResponse> list(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(analysisService.listAnalyses(Long.valueOf(jwt.getSubject()), page, size));
    }
}
