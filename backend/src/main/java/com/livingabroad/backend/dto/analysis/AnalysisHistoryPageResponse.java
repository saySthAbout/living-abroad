package com.livingabroad.backend.dto.analysis;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AnalysisHistoryPageResponse(
    int page,
    int size,
    long totalElements,
    List<HistoryItem> items
) {
    public record HistoryItem(
        Long analysisId,
        String topCountryCode,
        String topVisaName,
        BigDecimal topScore,
        OffsetDateTime analyzedAt
    ) {
    }
}
