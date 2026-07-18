package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.AnalysisCountryResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisCountryResultRepository extends JpaRepository<AnalysisCountryResult, Long> {

    List<AnalysisCountryResult> findByAnalysisIdOrderByRankPositionAsc(Long analysisId);

    Optional<AnalysisCountryResult> findFirstByAnalysisIdAndRankPosition(Long analysisId, short rankPosition);
}
