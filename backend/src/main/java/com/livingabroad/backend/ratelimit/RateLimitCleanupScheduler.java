package com.livingabroad.backend.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimitCleanupScheduler {

    // 두 티어(auth/general) 윈도우 중 더 큰 쪽보다 넉넉히 크게 잡아, 아직 유효한 항목을 지우지 않는다.
    private static final long STALE_AFTER_MS = 10 * 60 * 1000;

    private final RateLimiter rateLimiter;

    public RateLimitCleanupScheduler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cleanup() {
        rateLimiter.evictStaleEntries(STALE_AFTER_MS);
    }
}
