package com.livingabroad.backend.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

    private final Clock testClock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };

    private final RateLimiter rateLimiter = new RateLimiter(testClock);

    @Test
    void allowsUpToTheLimitWithinTheWindow() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire("key", 3, 60_000).allowed()).isTrue();
        }
    }

    @Test
    void rejectsRequestsBeyondTheLimit() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryAcquire("key", 3, 60_000);
        }

        RateLimiter.Result result = rateLimiter.tryAcquire("key", 3, 60_000);

        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isZero();
    }

    @Test
    void resetsAfterTheWindowElapses() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryAcquire("key", 3, 60_000);
        }
        assertThat(rateLimiter.tryAcquire("key", 3, 60_000).allowed()).isFalse();

        now.set(now.get().plusMillis(60_001));

        assertThat(rateLimiter.tryAcquire("key", 3, 60_000).allowed()).isTrue();
    }

    @Test
    void tracksSeparateKeysIndependently() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryAcquire("a", 3, 60_000);
        }

        assertThat(rateLimiter.tryAcquire("a", 3, 60_000).allowed()).isFalse();
        assertThat(rateLimiter.tryAcquire("b", 3, 60_000).allowed()).isTrue();
    }

    @Test
    void remainingCountsDownCorrectly() {
        assertThat(rateLimiter.tryAcquire("key", 5, 60_000).remaining()).isEqualTo(4);
        assertThat(rateLimiter.tryAcquire("key", 5, 60_000).remaining()).isEqualTo(3);
    }

    @Test
    void evictStaleEntriesDoesNotRemoveStillActiveWindows() {
        rateLimiter.tryAcquire("active", 3, 60_000);

        rateLimiter.evictStaleEntries(600_000);

        assertThat(rateLimiter.tryAcquire("active", 3, 60_000).remaining()).isEqualTo(1);
    }
}
