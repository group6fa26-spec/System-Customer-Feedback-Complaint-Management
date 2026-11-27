package com.bql.feedback.dto;

import java.time.LocalDateTime;

public record AccountManagementResponseDto(
        Long accountId,
        String username,
        String role,
        String accountStatus,
        boolean active,
        boolean mustChangePassword,
        boolean deleted,
        LocalDateTime deletedAt,
        Long staffId,
        Long customerId,
        String fullName,
        String department,
        Boolean onDuty,
        Boolean staffActive,
        String phone,
        String email,
        String apartmentCode,
        String residentType,
        Boolean verifiedResident,
        Boolean customerActive,
        Boolean customerBlocked,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        String temporaryPassword
) {
}
