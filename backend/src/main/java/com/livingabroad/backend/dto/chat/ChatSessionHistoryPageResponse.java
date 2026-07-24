package com.livingabroad.backend.dto.chat;

import java.time.OffsetDateTime;
import java.util.List;

public record ChatSessionHistoryPageResponse(
    int page,
    int size,
    long totalElements,
    List<SessionSummary> items
) {
    public record SessionSummary(
        Long sessionId,
        String sessionTitle,
        String countryCode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
    }
}
