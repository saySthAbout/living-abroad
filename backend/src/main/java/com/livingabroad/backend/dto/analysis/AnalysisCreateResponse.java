package com.livingabroad.backend.dto.analysis;

import java.time.OffsetDateTime;

public record AnalysisCreateResponse(
    Long analysisId,
    String status,
    OffsetDateTime requestedAt
) {
}
