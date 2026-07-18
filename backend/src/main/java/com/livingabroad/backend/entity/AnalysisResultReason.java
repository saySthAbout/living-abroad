package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_result_reasons")
public class AnalysisResultReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reason_id")
    private Long reasonId;

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(name = "reason_type", nullable = false)
    private String reasonType;

    @Column(name = "reason_content", nullable = false)
    private String reasonContent;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 1;

    protected AnalysisResultReason() {
    }

    public AnalysisResultReason(Long resultId, String reasonType, String reasonContent, short sortOrder) {
        this.resultId = resultId;
        this.reasonType = reasonType;
        this.reasonContent = reasonContent;
        this.sortOrder = sortOrder;
    }

    public Long getResultId() {
        return resultId;
    }

    public String getReasonType() {
        return reasonType;
    }

    public String getReasonContent() {
        return reasonContent;
    }

    public short getSortOrder() {
        return sortOrder;
    }
}
