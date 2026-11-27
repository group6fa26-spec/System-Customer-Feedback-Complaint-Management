package com.bql.feedback.security;

import com.bql.feedback.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final long lockMinutes;
    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${app.security.login.max-failed-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lock-minutes:15}") long lockMinutes
    ) {
        this(maxAttempts, lockMinutes, Clock.systemUTC());
    }

    LoginAttemptService(int maxAttempts, long lockMinutes, Clock clock) {
        if (maxAttempts <= 0) {
            throw new IllegalStateException("app.security.login.max-failed-attempts phải lớn hơn 0");
        }
        if (lockMinutes <= 0) {
            throw new IllegalStateException("app.security.login.lock-minutes phải lớn hơn 0");
        }
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
        this.clock = clock;
    }

    private record AttemptState(int failedCount, Instant lockedUntil) {
    }

    public void checkAllowed(String username) {
        String key = normalizeKey(username);
        if (key == null) {
            return;
        }
        AttemptState state = attempts.get(key);
        if (state == null || state.lockedUntil() == null) {
            return;
        }
        Instant now = clock.instant();
        if (now.isBefore(state.lockedUntil())) {
            long remainingMillis = Duration.between(now, state.lockedUntil()).toMillis();
            long retryAfterSeconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
            throw new TooManyRequestsException(
                    "Tài khoản tạm bị khóa do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau.",
                    retryAfterSeconds
            );
        }
        attempts.remove(key, state);
    }

    public void recordFailure(String username) {
        String key = normalizeKey(username);
        if (key == null) {
            return;
        }
        Instant now = clock.instant();
        attempts.compute(key, (k, existing) -> {
            int count;
            if (existing == null || (existing.lockedUntil() != null && !now.isBefore(existing.lockedUntil()))) {
                count = 1;
            } else {
                count = existing.failedCount() + 1;
            }
            Instant lockedUntil = count >= maxAttempts
                    ? now.plusSeconds(lockMinutes * 60L)
                    : null;
            return new AttemptState(count, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        String key = normalizeKey(username);
        if (key != null) {
            attempts.remove(key);
        }
    }

    private String normalizeKey(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
