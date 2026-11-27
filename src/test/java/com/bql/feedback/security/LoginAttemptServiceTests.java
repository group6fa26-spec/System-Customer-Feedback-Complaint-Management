package com.bql.feedback.security;

import com.bql.feedback.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTests {

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant initial) {
            this.current = initial;
        }

        void plusSeconds(long seconds) {
            this.current = this.current.plusSeconds(seconds);
        }

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
            return current;
        }
    }

    @Test
    void locksAfterMaxFailedAttemptsAndUnlocksAfterLockDuration() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-27T08:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(3, 15, clock);

        service.recordFailure("UserA");
        service.recordFailure("usera");
        assertDoesNotThrow(() -> service.checkAllowed("usera"));

        service.recordFailure("USERA");
        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> service.checkAllowed("usera"));
        assertTrue(ex.getRetryAfterSeconds() >= 899 && ex.getRetryAfterSeconds() <= 900);

        clock.plusSeconds(900);
        assertDoesNotThrow(() -> service.checkAllowed("usera"));
    }

    @Test
    void recordSuccessClearsPreviousFailures() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-27T08:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(2, 15, clock);

        service.recordFailure("an@gmail.com");
        service.recordSuccess("AN@GMAIL.COM");
        service.recordFailure("an@gmail.com");

        assertDoesNotThrow(() -> service.checkAllowed("an@gmail.com"));
    }
}
