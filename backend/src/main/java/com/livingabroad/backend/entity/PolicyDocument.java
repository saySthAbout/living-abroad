package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "policy_documents")
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "document_title", nullable = false)
    private String documentTitle;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "verified_at", nullable = false)
    private OffsetDateTime verifiedAt;

    protected PolicyDocument() {
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }
}
