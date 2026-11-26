package com.bql.feedback.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerResponseDto(
        Long id,
        String fullName,
        String phone,
        String email,
        String apartmentCode,
        String residentType,
        List<RentalContractResponseDto> rentalContracts,
        boolean verifiedResident,
        boolean active,
        boolean blocked,
        int spamStrikes,
        int ratingTrustScore,
        LocalDateTime createdAt
) {
}
