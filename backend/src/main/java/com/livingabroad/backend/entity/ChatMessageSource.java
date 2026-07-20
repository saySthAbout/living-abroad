package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "chat_message_sources")
public class ChatMessageSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(name = "relevance_score")
    private BigDecimal relevanceScore;

    @Column(name = "quoted_text")
    private String quotedText;

    protected ChatMessageSource() {
    }

    public ChatMessageSource(Long messageId, Long chunkId, BigDecimal relevanceScore) {
        this.messageId = messageId;
        this.chunkId = chunkId;
        this.relevanceScore = relevanceScore;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public BigDecimal getRelevanceScore() {
        return relevanceScore;
    }
}
