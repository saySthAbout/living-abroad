package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "analysis_country_results")
public class AnalysisCountryResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "visa_program_id", nullable = false)
    private Long visaProgramId;

    @Column(name = "rank_position", nullable = false)
    private Short rankPosition;

    @Column(name = "total_score", nullable = false)
    private BigDecimal totalScore;

    @Column(name = "rule_score", nullable = false)
    private BigDecimal ruleScore;

    @Column(name = "environment_score", nullable = false)
    private BigDecimal environmentScore;

    @Column(name = "career_similarity_score", nullable = false)
    private BigDecimal careerSimilarityScore;

    @Column(name = "preference_score", nullable = false)
    private BigDecimal preferenceScore;

    @Column(name = "eligibility_status", nullable = false)
    private String eligibilityStatus;

    @Column(name = "result_summary")
    private String resultSummary;

    protected AnalysisCountryResult() {
    }

    public AnalysisCountryResult(
        Long analysisId,
        Long visaProgramId,
        Short rankPosition,
        BigDecimal totalScore,
        BigDecimal ruleScore,
        BigDecimal environmentScore,
        BigDecimal careerSimilarityScore,
        BigDecimal preferenceScore,
        String eligibilityStatus
    ) {
        this.analysisId = analysisId;
        this.visaProgramId = visaProgramId;
        this.rankPosition = rankPosition;
        this.totalScore = totalScore;
        this.ruleScore = ruleScore;
        this.environmentScore = environmentScore;
        this.careerSimilarityScore = careerSimilarityScore;
        this.preferenceScore = preferenceScore;
        this.eligibilityStatus = eligibilityStatus;
    }

    public Long getResultId() {
        return resultId;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public Long getVisaProgramId() {
        return visaProgramId;
    }

    public Short getRankPosition() {
        return rankPosition;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public BigDecimal getRuleScore() {
        return ruleScore;
    }

    public BigDecimal getEnvironmentScore() {
        return environmentScore;
    }

    public BigDecimal getCareerSimilarityScore() {
        return careerSimilarityScore;
    }

    public BigDecimal getPreferenceScore() {
        return preferenceScore;
    }

    public String getEligibilityStatus() {
        return eligibilityStatus;
    }
}
