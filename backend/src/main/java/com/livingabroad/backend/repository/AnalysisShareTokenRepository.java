package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.AnalysisShareToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisShareTokenRepository extends JpaRepository<AnalysisShareToken, Long> {

    Optional<AnalysisShareToken> findByShareToken(String shareToken);

    Optional<AnalysisShareToken> findFirstByAnalysisIdAndRevokedAtIsNullOrderByCreatedAtDesc(Long analysisId);

    List<AnalysisShareToken> findAllByAnalysisIdAndRevokedAtIsNull(Long analysisId);
}
