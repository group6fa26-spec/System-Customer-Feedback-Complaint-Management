package com.bql.feedback.dto;

import java.time.LocalDateTime;
import java.util.List;

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
        String serviceName,
        Long customerId,
        Long categoryId,
        Long autoCategoryId,
        String classifiedBy,
        double autoConfidence,
        Long assignedStaffId,
        boolean afterHours,
        boolean missingInfo,
        boolean requiresManagerApproval,
        Long duplicateOfId,
        int duplicateCount,
        List<Long> suspectedDuplicateIds,
        LocalDateTime responseDueAt,
        LocalDateTime resolveDueAt,
        LocalDateTime createdAt
) {
}