package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now OR t.revokedAt IS NOT NULL")
    int deleteExpiredOrRevoked(@Param("now") OffsetDateTime now);

    // 비밀번호 재설정 시, 재설정 전 발급된 refresh token으로 이미 로그인된 다른 세션(기기)이
    // 계속 유효하지 않도록 전부 무효화한다 (계정 탈취 복구 시나리오의 표준 보안 관행).
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
