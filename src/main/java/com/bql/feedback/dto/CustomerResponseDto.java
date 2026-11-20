package com.bql.feedback.dto;

import java.time.LocalDateTime;

public record CustomerResponseDto(
        Long id,
        String fullName,
        String phone,
        String email,
        String apartmentCode,
        boolean verifiedResident,
        boolean active,
        boolean blocked,
        int spamStrikes,
        int ratingTrustScore,
        LocalDateTime createdAt
) {
}
