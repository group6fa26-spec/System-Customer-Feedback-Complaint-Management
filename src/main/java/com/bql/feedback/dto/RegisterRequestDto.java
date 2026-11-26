package com.bql.feedback.dto;

import java.time.LocalDate;
import com.bql.feedback.model.ResidentType;

public record RegisterRequestDto(
        String username,
        String password,
        String fullName,
        String phone,
        String email,
        String apartmentCode,
        ResidentType residentType,
        LocalDate rentalContractStartDate,
        LocalDate rentalContractEndDate
) {
}
