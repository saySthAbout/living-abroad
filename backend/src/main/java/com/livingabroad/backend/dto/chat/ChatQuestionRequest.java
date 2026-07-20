package com.livingabroad.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatQuestionRequest(
    Long sessionId,

    @NotBlank
    @Size(min = 2, max = 1000)
    String question,

    @NotBlank
    String countryCode,

    // Optional: the frontend doesn't collect a visa selection today (MVP has
    // exactly one representative visa per country) — ChatService resolves it
    // from countryCode via VisaProgramRepository when this is blank.
    String visaCode,

    Long analysisId
) {
}
