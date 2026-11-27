package com.bql.feedback.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LoginResponseDto(
        Long accountId,
        String username,
        String role,
        Long customerId,
        Long staffId,
        String fullName,
        String department,
        String apartmentCode,
        Boolean blocked,
        LocalDateTime lastLoginAt,
        String residentType,
        String accountStatus,
        List<RentalContractResponseDto> rentalContracts,
        String token
) {
}
