package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "message_role", nullable = false)
    private String messageRole;

    @Column(name = "message_content", nullable = false)
    private String messageContent;

    @Column(name = "answerable")
    private Boolean answerable;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(Long sessionId, String messageRole, String messageContent, Boolean answerable) {
        this.sessionId = sessionId;
        this.messageRole = messageRole;
        this.messageContent = messageContent;
        this.answerable = answerable;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getMessageRole() {
        return messageRole;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public Boolean getAnswerable() {
        return answerable;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
