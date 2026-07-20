package com.livingabroad.backend.dto.ai;

import java.math.BigDecimal;
import java.util.List;

public record AiRagResponseDto(
    String answer,
    boolean answerable,
    List<Source> sources,
    String promptVersion
) {
    public record Source(
        Long chunkId,
        String title,
        String url,
        String verifiedAt,
        BigDecimal score
    ) {
    }
}
