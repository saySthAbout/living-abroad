package com.livingabroad.backend.service;

import com.livingabroad.backend.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

// refresh_tokens는 로그인/재발급마다 계속 쌓이기만 하고(로테이션 시 이전 행은 revoke만 되고 삭제되지 않음)
// 지워도 안전한 근거가 없어졌으므로(만료됐거나 revoke된 토큰은 findByTokenHash가 그대로 있어도 어차피
// isUsable()이 false라 재사용을 막아준다), 주기적으로 정리해 테이블이 무한정 커지는 것을 막는다.
@Component
public class RefreshTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);
    private static final long INTERVAL_MS = 24 * 60 * 60 * 1000;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedRate = INTERVAL_MS)
    @Transactional
    public void cleanup() {
        int deleted = refreshTokenRepository.deleteExpiredOrRevoked(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("만료/revoke된 refresh_tokens {}건 정리", deleted);
        }
    }
}
