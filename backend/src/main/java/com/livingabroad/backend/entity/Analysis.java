package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "analysis_status", nullable = false)
    private String analysisStatus = "PENDING";

    @Column(name = "input_age", nullable = false)
    private Short inputAge;

    @Column(name = "input_education_level", nullable = false)
    private String inputEducationLevel;

    @Column(name = "input_major")
    private String inputMajor;

    @Column(name = "input_current_occupation", nullable = false)
    private String inputCurrentOccupation;

    @Column(name = "input_experience_years", nullable = false)
    private BigDecimal inputExperienceYears;

    @Column(name = "input_language_test_type")
    private String inputLanguageTestType;

    @Column(name = "input_language_score")
    private BigDecimal inputLanguageScore;

    @Column(name = "input_available_funds_krw", nullable = false)
    private Long inputAvailableFundsKrw;

    @Column(name = "input_family_accompanied", nullable = false)
    private boolean inputFamilyAccompanied;

    @Column(name = "input_preferred_country_code")
    private String inputPreferredCountryCode;

    @Column(name = "input_career_description", nullable = false)
    private String inputCareerDescription;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "data_version")
    private String dataVersion;

    @Column(name = "requested_at", insertable = false, updatable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    protected Analysis() {
    }

    public Analysis(
        Long userId,
        Short inputAge,
        String inputEducationLevel,
        String inputMajor,
        String inputCurrentOccupation,
        BigDecimal inputExperienceYears,
        String inputLanguageTestType,
        BigDecimal inputLanguageScore,
        Long inputAvailableFundsKrw,
        boolean inputFamilyAccompanied,
        String inputPreferredCountryCode,
        String inputCareerDescription
    ) {
        this.userId = userId;
        this.inputAge = inputAge;
        this.inputEducationLevel = inputEducationLevel;
        this.inputMajor = inputMajor;
        this.inputCurrentOccupation = inputCurrentOccupation;
        this.inputExperienceYears = inputExperienceYears;
        this.inputLanguageTestType = inputLanguageTestType;
        this.inputLanguageScore = inputLanguageScore;
        this.inputAvailableFundsKrw = inputAvailableFundsKrw;
        this.inputFamilyAccompanied = inputFamilyAccompanied;
        this.inputPreferredCountryCode = inputPreferredCountryCode;
        this.inputCareerDescription = inputCareerDescription;
    }

    public void markProcessing() {
        this.analysisStatus = "PROCESSING";
    }

    public void markCompleted(String modelVersion, String dataVersion) {
        this.analysisStatus = "COMPLETED";
        this.modelVersion = modelVersion;
        this.dataVersion = dataVersion;
        this.completedAt = OffsetDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.analysisStatus = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = OffsetDateTime.now();
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public String getInputCareerDescription() {
        return inputCareerDescription;
    }

    public Short getInputAge() {
        return inputAge;
    }

    public String getInputEducationLevel() {
        return inputEducationLevel;
    }

    public String getInputMajor() {
        return inputMajor;
    }

    public String getInputCurrentOccupation() {
        return inputCurrentOccupation;
    }

    public BigDecimal getInputExperienceYears() {
        return inputExperienceYears;
    }

    public String getInputLanguageTestType() {
        return inputLanguageTestType;
    }

    public BigDecimal getInputLanguageScore() {
        return inputLanguageScore;
    }

    public Long getInputAvailableFundsKrw() {
        return inputAvailableFundsKrw;
    }

    public boolean isInputFamilyAccompanied() {
        return inputFamilyAccompanied;
    }

    public String getInputPreferredCountryCode() {
        return inputPreferredCountryCode;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}
