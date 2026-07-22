package com.livingabroad.backend.dto.analysis;

import java.math.BigDecimal;

public record AnalysisInputResponse(
    Integer age,
    String education,
    String major,
    String occupation,
    BigDecimal experienceYears,
    String languageTest,
    BigDecimal languageScore,
    String fundsRange,
    Boolean familyAccompanied,
    String preferredCountry,
    String careerText
) {
}
