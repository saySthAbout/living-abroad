package com.livingabroad.backend.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// 클라이언트별(키 단위) 고정 윈도우 카운터. 단일 인스턴스 배포(GCP VM 1대)를 전제로 메모리에만 보관한다 —
// 여러 인스턴스로 수평 확장하게 되면 Redis 등 공유 저장소로 옮겨야 한다.
@Component
public class RateLimiter {

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public Result tryAcquire(String key, int limit, long windowMs) {
        long now = clock.millis();
        Window window = windows.compute(key, (k, existing) ->
            (existing == null || now - existing.startMillis >= windowMs) ? new Window(now) : existing
        );

        long count = window.count.incrementAndGet();
        long resetAtMillis = window.startMillis + windowMs;
        long remaining = Math.max(limit - count, 0);
        return new Result(count <= limit, remaining, resetAtMillis);
    }

    // 윈도우가 한참 지난(스레드가 더 이상 참조하지 않을) 항목을 정리해 맵이 무한정 커지지 않게 한다.
    public void evictStaleEntries(long staleAfterMs) {
        long now = clock.millis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().startMillis >= staleAfterMs);
    }

    public record Result(boolean allowed, long remaining, long resetAtMillis) {
    }

    private static final class Window {
        private final long startMillis;
        private final AtomicLong count = new AtomicLong(0);

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }
}
