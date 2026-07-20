package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

    List<PolicyDocument> findByDocumentIdIn(List<Long> documentIds);
}
