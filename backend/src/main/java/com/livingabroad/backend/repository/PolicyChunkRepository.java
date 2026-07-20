package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.PolicyChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyChunkRepository extends JpaRepository<PolicyChunk, Long> {

    List<PolicyChunk> findByChunkIdIn(List<Long> chunkIds);
}
