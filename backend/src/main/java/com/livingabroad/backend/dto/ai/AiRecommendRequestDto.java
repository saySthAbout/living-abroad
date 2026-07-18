package com.livingabroad.backend.dto.ai;

import java.util.List;
import java.util.Map;

public record AiRecommendRequestDto(
    Long analysisId,
    Map<String, Object> userProfile,
    List<String> supportedCountries
) {
}
