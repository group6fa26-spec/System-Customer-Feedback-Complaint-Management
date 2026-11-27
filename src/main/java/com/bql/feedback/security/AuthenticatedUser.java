package com.bql.feedback.security;

public record AuthenticatedUser(
        Long accountId,
        String username,
        String role,
        Long customerId,
        Long staffId
) {
}
