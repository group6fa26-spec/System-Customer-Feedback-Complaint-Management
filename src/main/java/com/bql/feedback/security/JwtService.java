package com.bql.feedback.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-minutes:480}") long expirationMinutes
    ) {
        byte[] keyBytes = secret != null ? secret.getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret phải có độ dài tối thiểu 32 byte cho HS256");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("app.security.jwt.expiration-minutes phải lớn hơn 0");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMinutes * 60_000L;
    }

    public String generateToken(Long accountId, String username, String role,
                                Long customerId, Long staffId) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("accountId", accountId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs));
        if (customerId != null) {
            builder.claim("customerId", customerId);
        }
        if (staffId != null) {
            builder.claim("staffId", staffId);
        }
        return builder.signWith(key).compact();
    }

    /** Ném JwtException nếu token sai chữ ký, hết hạn hoặc thiếu claim bắt buộc. */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long accountId = getLongClaim(claims, "accountId");
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        Long customerId = getLongClaim(claims, "customerId");
        Long staffId = getLongClaim(claims, "staffId");

        if (accountId == null || username == null || username.isBlank() || role == null || role.isBlank()) {
            throw new MalformedJwtException("JWT thiếu claim bắt buộc (accountId, subject hoặc role)");
        }

        return new AuthenticatedUser(accountId, username, role, customerId, staffId);
    }

    private Long getLongClaim(Claims claims, String name) {
        Number value = claims.get(name, Number.class);
        return value != null ? value.longValue() : null;
    }
}
