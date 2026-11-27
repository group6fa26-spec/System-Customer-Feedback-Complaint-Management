package com.bql.feedback.dto;

public record ChangePasswordRequestDto(
        String currentPassword,
        String newPassword
) {
}
