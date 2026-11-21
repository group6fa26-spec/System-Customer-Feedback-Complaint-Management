package com.bql.feedback.dto;

public record RegisterRequestDto(
        String username,
        String password,
        String fullName,
        String phone,
        String email,
        String apartmentCode
) {
}
