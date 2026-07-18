package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.AnalysisResultReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisResultReasonRepository extends JpaRepository<AnalysisResultReason, Long> {

    List<AnalysisResultReason> findByResultIdInOrderBySortOrderAsc(List<Long> resultIds);
}
