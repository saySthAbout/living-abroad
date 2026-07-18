package com.livingabroad.backend.dto.analysis;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AnalysisCreateRequest(
    @NotNull @Min(18) @Max(64)
    Integer age,

    @NotBlank
    String education,

    @Size(max = 100)
    String major,

    @NotBlank @Size(max = 150)
    String occupation,

    @NotNull @Min(0) @Max(40)
    BigDecimal experienceYears,

    String languageTest,

    BigDecimal languageScore,

    @NotBlank
    String fundsRange,

    @NotNull
    Boolean familyAccompanied,

    @NotBlank
    String preferredCountry,

    @NotBlank @Size(min = 100, max = 2000)
    String careerText
) {
}
