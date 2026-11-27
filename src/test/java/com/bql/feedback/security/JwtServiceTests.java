package com.bql.feedback.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTests {

    private static final String TEST_SECRET = "bql-feedback-test-jwt-secret-key-minimum-32-bytes-2026";

    @Test
    void generateAndParseCustomerToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 480);

        String token = jwtService.generateToken(1L, "an@gmail.com", "CUSTOMER", 10L, null);
        AuthenticatedUser user = jwtService.parse(token);

        assertEquals(1L, user.accountId());
        assertEquals("an@gmail.com", user.username());
        assertEquals("CUSTOMER", user.role());
        assertEquals(10L, user.customerId());
        assertNull(user.staffId());
    }

    @Test
    void generateAndParseStaffToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 480);

        String token = jwtService.generateToken(2L, "manager@bql.vn", "CS_MANAGER", null, 5L);
        AuthenticatedUser user = jwtService.parse(token);

        assertEquals(2L, user.accountId());
        assertEquals("manager@bql.vn", user.username());
        assertEquals("CS_MANAGER", user.role());
        assertNull(user.customerId());
        assertEquals(5L, user.staffId());
    }

    @Test
    void parseRejectsTamperedToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 480);
        String token = jwtService.generateToken(1L, "an@gmail.com", "CUSTOMER", 10L, null);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("aa") ? "bb" : "aa");

        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }

    @Test
    void parseRejectsExpiredToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 480);
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("an@gmail.com")
                .claim("accountId", 1L)
                .claim("role", "CUSTOMER")
                .claim("customerId", 10L)
                .issuedAt(new Date(System.currentTimeMillis() - 120_000L))
                .expiration(new Date(System.currentTimeMillis() - 60_000L))
                .signWith(key)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.parse(expiredToken));
    }

    @Test
    void parseRejectsTokenMissingRequiredClaims() {
        JwtService jwtService = new JwtService(TEST_SECRET, 480);
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String missingRoleToken = Jwts.builder()
                .subject("an@gmail.com")
                .claim("accountId", 1L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key)
                .compact();

        assertThrows(JwtException.class, () -> jwtService.parse(missingRoleToken));
    }

    @Test
    void constructorRejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short-secret", 480));
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 480));
        assertThrows(IllegalStateException.class, () -> new JwtService(TEST_SECRET, 0));
    }
}
