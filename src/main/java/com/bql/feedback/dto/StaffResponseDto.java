package com.bql.feedback.dto;

public record StaffResponseDto(
        Long id,
        String fullName,
        String role,
        String department,
        boolean active,
        boolean onDuty
) {
}
