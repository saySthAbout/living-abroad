package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "analysis_share_tokens")
public class AnalysisShareToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_token_id")
    private Long shareTokenId;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "share_token", nullable = false, unique = true)
    private String shareToken;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AnalysisShareToken() {
    }

    public AnalysisShareToken(Long analysisId, String shareToken) {
        this.analysisId = analysisId;
        this.shareToken = shareToken;
    }

    public boolean isUsable() {
        return revokedAt == null;
    }

    public void revoke() {
        this.revokedAt = OffsetDateTime.now();
    }

    public Long getShareTokenId() {
        return shareTokenId;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public String getShareToken() {
        return shareToken;
    }
}
