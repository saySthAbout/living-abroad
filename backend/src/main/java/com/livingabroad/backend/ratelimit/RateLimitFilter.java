package com.livingabroad.backend.ratelimit;

import com.livingabroad.backend.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

// Spring Security의 필터 체인(및 그 안의 JWT 인증 처리)보다 먼저 실행되도록 최우선순위로 등록한다 —
// 남용성 요청을 인증/인가 처리 전에 최대한 일찍 차단하기 위함이다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_TIER_PREFIX = "/api/auth/";
    private static final String HEALTH_PATH = "/api/health";
    private static final int TOO_MANY_REQUESTS = 429;

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.auth.limit}")
    private int authLimit;

    @Value("${app.rate-limit.auth.window-ms}")
    private long authWindowMs;

    @Value("${app.rate-limit.general.limit}")
    private int generalLimit;

    @Value("${app.rate-limit.general.window-ms}")
    private long generalWindowMs;

    public RateLimitFilter(RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.equals(HEALTH_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isAuthTier = path.startsWith(AUTH_TIER_PREFIX);
        int limit = isAuthTier ? authLimit : generalLimit;
        long windowMs = isAuthTier ? authWindowMs : generalWindowMs;
        String tier = isAuthTier ? "auth" : "general";

        String key = tier + ":" + resolveClientIp(request);
        RateLimiter.Result result = rateLimiter.tryAcquire(key, limit, windowMs);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (!result.allowed()) {
            long retryAfterSeconds = Math.max(1, (result.resetAtMillis() - System.currentTimeMillis()) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setStatus(TOO_MANY_REQUESTS);
            response.setContentType("application/json;charset=UTF-8");

            ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED",
                "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
                path,
                List.of()
            );
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
