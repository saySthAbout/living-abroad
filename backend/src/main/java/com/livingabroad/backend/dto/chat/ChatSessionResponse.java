package com.livingabroad.backend.dto.chat;

import java.time.OffsetDateTime;
import java.util.List;

public record ChatSessionResponse(
    Long sessionId,
    List<MessageDto> messages
) {
    public record MessageDto(
        Long messageId,
        String role,
        String content,
        Boolean answerable,
        List<ChatAnswerResponse.SourceDto> sources,
        OffsetDateTime createdAt
    ) {
    }
}
