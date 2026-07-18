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
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "age", nullable = false)
    private Short age;

    @Column(name = "education_level", nullable = false)
    private String educationLevel;

    @Column(name = "major")
    private String major;

    @Column(name = "current_occupation", nullable = false)
    private String currentOccupation;

    @Column(name = "experience_years", nullable = false)
    private BigDecimal experienceYears;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    protected UserProfile() {
    }

    public UserProfile(Long userId, Short age, String educationLevel, String major, String currentOccupation, BigDecimal experienceYears) {
        this.userId = userId;
        this.age = age;
        this.educationLevel = educationLevel;
        this.major = major;
        this.currentOccupation = currentOccupation;
        this.experienceYears = experienceYears;
    }

    public void update(Short age, String educationLevel, String major, String currentOccupation, BigDecimal experienceYears) {
        this.age = age;
        this.educationLevel = educationLevel;
        this.major = major;
        this.currentOccupation = currentOccupation;
        this.experienceYears = experienceYears;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getUserId() {
        return userId;
    }

    public Short getAge() {
        return age;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public String getMajor() {
        return major;
    }

    public String getCurrentOccupation() {
        return currentOccupation;
    }

    public BigDecimal getExperienceYears() {
        return experienceYears;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
