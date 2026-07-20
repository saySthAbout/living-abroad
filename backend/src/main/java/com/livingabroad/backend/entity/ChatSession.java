package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(name = "visa_program_id")
    private Long visaProgramId;

    @Column(name = "session_title")
    private String sessionTitle;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    protected ChatSession() {
    }

    public ChatSession(Long userId, Long analysisId, Long visaProgramId, String sessionTitle) {
        this.userId = userId;
        this.analysisId = analysisId;
        this.visaProgramId = visaProgramId;
        this.sessionTitle = sessionTitle;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
