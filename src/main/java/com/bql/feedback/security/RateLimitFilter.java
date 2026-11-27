package com.bql.feedback.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IA-CF05 Abuse Protection: giới hạn tần suất (Rate Limiting) theo IP bằng Token Bucket (Bucket4j).
 *  - Nhóm "auth"     : POST /api/auth/login, /api/auth/register  (chống brute-force / spam đăng ký)
 *  - Nhóm "feedback" : POST /api/feedbacks (+ /attachments)      (chống spam phiếu / upload)
 * Vượt giới hạn -> 429 + header Retry-After. Log WARN "RATE_LIMIT_BLOCKED" để CF07 (Security Monitoring) thống kê.
 *
 * Lưu ý: LoginAttemptService (khóa theo USERNAME) vẫn giữ nguyên; filter này chặn theo IP => hai lớp bổ trợ nhau.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final int authPerMinute;
    private final int feedbackPerMinute;
    private final boolean trustForwardedFor;
    private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();

    private record Entry(Bucket bucket, Instant createdAt) {}

    public RateLimitFilter(int authPerMinute, int feedbackPerMinute, boolean trustForwardedFor) {
        this.authPerMinute = authPerMinute;
        this.feedbackPerMinute = feedbackPerMinute;
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String group = classify(req);
        if (group == null) {
            chain.doFilter(req, res);
            return;
        }
        int limit = group.equals("auth") ? authPerMinute : feedbackPerMinute;
        String ip = clientIp(req);

        if (buckets.size() > MAX_TRACKED_KEYS) {   // tránh tràn RAM khi bị tấn công nhiều IP
            buckets.clear();
        }
        Bucket bucket = buckets.computeIfAbsent(group + ":" + ip, k -> new Entry(newBucket(limit), Instant.now())).bucket();
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            chain.doFilter(req, res);
            return;
        }
        long retryAfter = Math.max(1L, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
        log.warn("RATE_LIMIT_BLOCKED ip={} group={} method={} path={} retryAfter={}s",
                ip, group, req.getMethod(), req.getRequestURI(), retryAfter);
        res.setStatus(429);
        res.setHeader("Retry-After", String.valueOf(retryAfter));
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":429,"
                + "\"error\":\"Too Many Requests\",\"message\":\"Bạn thao tác quá nhanh. Vui lòng thử lại sau.\"}");
    }

    private Bucket newBucket(int perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute)
                        .refillIntervally(perMinute, Duration.ofMinutes(1)).build())
                .build();
    }

    /** Chỉ giới hạn request ghi (POST) vào các endpoint nhạy cảm. */
    private String classify(HttpServletRequest req) {
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            return null;
        }
        String path = req.getRequestURI();
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            return "auth";
        }
        if (path.equals("/api/feedbacks") || (path.startsWith("/api/feedbacks/") && path.endsWith("/attachments"))) {
            return "feedback";
        }
        return null;
    }

    /** Chỉ tin X-Forwarded-For khi chạy sau Nginx của mình (nếu không, client có thể giả header để né limit). */
    private String clientIp(HttpServletRequest req) {
        if (trustForwardedFor) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return req.getRemoteAddr();
    }
}