package com.livingabroad.backend.dto.ai;

import java.math.BigDecimal;
import java.util.List;

public record AiRecommendResponseDto(
    String modelVersion,
    String dataVersion,
    List<CountryResult> results
) {
    public record CountryResult(
        int rank,
        String countryCode,
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
