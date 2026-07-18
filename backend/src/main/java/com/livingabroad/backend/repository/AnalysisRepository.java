package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.Analysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Page<Analysis> findByUserIdOrderByRequestedAtDesc(Long userId, Pageable pageable);
}
