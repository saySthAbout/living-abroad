package com.livingabroad.backend.dto.chat;

import java.util.List;

public record ChatAnswerResponse(
    Long sessionId,
    Long messageId,
    String answer,
    boolean answerable,
    List<SourceDto> sources,
    String disclaimer
) {
    public record SourceDto(String title, String url, String verifiedAt) {
    }
}
