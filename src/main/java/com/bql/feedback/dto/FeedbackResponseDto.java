package com.bql.feedback.dto;

import java.time.LocalDateTime;

public record FeedbackResponseDto(
        Long id,
        String code,
        String title,
        String content,
        String type,
        String priority,
        String status,
        String apartmentCode,
        String location,
        Long customerId,
        Long categoryId,
        Long assignedStaffId,
        LocalDateTime createdAt
) {
}