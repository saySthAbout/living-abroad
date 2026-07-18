package com.livingabroad.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "visa_programs")
public class VisaProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visa_program_id")
    private Long visaProgramId;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "program_code", nullable = false)
    private String programCode;

    @Column(name = "program_name_ko", nullable = false)
    private String programNameKo;

    @Column(name = "program_name_en", nullable = false)
    private String programNameEn;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected VisaProgram() {
    }

    public Long getVisaProgramId() {
        return visaProgramId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getProgramCode() {
        return programCode;
    }

    public String getProgramNameKo() {
        return programNameKo;
    }

    public String getProgramNameEn() {
        return programNameEn;
    }
}
