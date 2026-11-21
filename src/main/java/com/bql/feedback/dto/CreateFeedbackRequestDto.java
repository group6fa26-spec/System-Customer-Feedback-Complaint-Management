package com.bql.feedback.dto;

import com.bql.feedback.model.FeedbackType;

public record CreateFeedbackRequestDto(
        Long customerId,
        FeedbackType type,
        String title,
        String content,
        Long categoryId,
        String serviceName,
        String location,
        String apartmentCode,
        Long aboutStaffId
) {
}
