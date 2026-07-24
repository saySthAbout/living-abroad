package com.livingabroad.backend.event;

import com.livingabroad.backend.dto.analysis.AnalysisCreateRequest;

public record AnalysisRequestedEvent(Long analysisId, AnalysisCreateRequest request) {
}
