package com.livingabroad.backend.dto.ai;

public record AiRagRequestDto(
    String question,
    String countryCode,
    String visaCode,
    Integer topK
) {
}
