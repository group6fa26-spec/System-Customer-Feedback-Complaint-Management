package com.bql.feedback.dto;

import java.time.LocalDate;

public record RentalContractResponseDto(
        Long id,
        String apartmentCode,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
}