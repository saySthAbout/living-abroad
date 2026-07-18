package com.livingabroad.backend.dto.analysis;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AnalysisDetailResponse(
    Long analysisId,
    String status,
    OffsetDateTime analyzedAt,
    List<CountryResultDto> results,
    String disclaimer
) {
    public record CountryResultDto(
        int rank,
        String countryCode,
        String countryName,
        String visaCode,
        String visaName,
        BigDecimal totalScore,
        BigDecimal ruleScore,
        BigDecimal environmentScore,
        BigDecimal careerSimilarity,
        BigDecimal preferenceScore,
        String ruleStatus,
        List<String> strengths,
        List<String> improvements
    ) {
    }
}
