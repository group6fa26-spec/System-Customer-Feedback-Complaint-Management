package com.bql.feedback.dto;

public record UpdateCustomerProfileRequestDto(
        String fullName,
        String phone,
        String email
) {
}
