package com.bql.feedback.dto;

import com.bql.feedback.model.FeedbackStatus;

public record UpdateFeedbackStatusRequestDto(
        FeedbackStatus status,
        String resolutionSummary,
        String note
) {
}
