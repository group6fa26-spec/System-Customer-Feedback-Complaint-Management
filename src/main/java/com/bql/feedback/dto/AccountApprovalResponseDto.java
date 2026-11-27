package com.bql.feedback.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AccountApprovalResponseDto(
        Long accountId,
        String username,
        String role,
        String accountStatus,
        boolean accountActive,
        Long customerId,
        String fullName,
        String phone,
        String email,
        String apartmentCode,
        String residentType,
        boolean verifiedResident,
        boolean customerActive,
        List<RentalContractResponseDto> rentalContracts,
        LocalDateTime createdAt
) {
}
